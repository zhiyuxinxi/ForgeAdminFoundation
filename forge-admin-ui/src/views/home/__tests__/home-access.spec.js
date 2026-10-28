import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it, vi } from 'vitest'
import { canLoadHomeWorkbench, filterAccessibleHomeItems, hasAccessibleHomeRoute, loadAccessibleHomeWorkbench, resolveHomeAdminStatsAccess, resolveHomeFlowApiAccess } from '../home-access'

const homePage = readFileSync(path.resolve(process.cwd(), 'src/views/home/index.vue'), 'utf8')

describe('home role-aware access', () => {
  const managerRoutes = [
    { path: '/flow/todo' },
    { path: '/flow/done?source=home' },
    { path: 'flow/started/' },
  ]

  it('keeps only entries backed by the current role route tree', () => {
    const items = [
      { path: '/flow/todo', title: '我的待办' },
      { path: '/system/online', title: '在线用户' },
    ]

    expect(filterAccessibleHomeItems(items, managerRoutes)).toEqual([
      { path: '/flow/todo', title: '我的待办' },
    ])
    expect(hasAccessibleHomeRoute(managerRoutes, '/flow/done')).toBe(true)
  })

  it('fails closed for administrator statistics not granted to a manager', () => {
    expect(resolveHomeAdminStatsAccess(managerRoutes)).toEqual({
      online: false,
      users: false,
      loginLogs: false,
    })
  })

  it('allows only explicitly granted administrator statistics', () => {
    expect(resolveHomeAdminStatsAccess([{ path: '/system/online' }])).toEqual({
      online: true,
      users: false,
      loginLogs: false,
    })
  })

  it('uses exact API permissions instead of visible flow routes for homepage requests', () => {
    expect(resolveHomeFlowApiAccess([], false)).toEqual({
      todo: false,
      done: false,
      started: false,
    })
    expect(resolveHomeFlowApiAccess(['flow:task:todo', 'flow:task:done'], false)).toEqual({
      todo: true,
      done: true,
      started: false,
    })
    expect(resolveHomeFlowApiAccess(['*:*:*'], false)).toEqual({
      todo: true,
      done: true,
      started: true,
    })
    expect(resolveHomeFlowApiAccess([], true)).toEqual({
      todo: true,
      done: true,
      started: true,
    })
    expect(homePage).toContain('resolveHomeFlowApiAccess(userStore.apiPermissions, userStore.isAdmin)')
    expect(homePage).toContain('access.todo')
    expect(homePage).toContain('access.done')
    expect(homePage).toContain('access.started')
  })

  it('loads the application workbench only for roles with the application center route', async () => {
    const loader = vi.fn().mockResolvedValue({ data: [{ id: 'app-1' }] })

    const managerApplications = await loadAccessibleHomeWorkbench(managerRoutes, loader)
    expect(managerApplications).toEqual([])
    expect(loader).not.toHaveBeenCalled()

    const adminApplications = await loadAccessibleHomeWorkbench([{ path: '/app-center' }], loader)
    expect(adminApplications).toEqual([{ id: 'app-1' }])
    expect(canLoadHomeWorkbench(managerRoutes)).toBe(false)
    expect(loader).toHaveBeenCalledTimes(1)
  })
})
