import { describe, expect, it, vi } from 'vitest'
import { loadMemberPage, loadOrganizationBranch, resolveIdentityReference } from '../identity-reference'

describe('business identity reference adapter', () => {
  const reference = { base: '/ai/crud/test/references/ownerId', operation: 'add' }

  it('uses the explicit runtime context and preserves custom external options', () => {
    const context = { identityReferenceConfigKey: 'test', modalStatus: 'edit' }
    expect(resolveIdentityReference({ field: 'orgId' }, context)).toEqual({ base: '/ai/crud/test/references/orgId', operation: 'edit' })
    expect(resolveIdentityReference({ field: 'orgId' }, {})).toBeNull()
    expect(resolveIdentityReference({ field: 'orgId' }, { identityReferenceApiBase: '/biz/records/references', modalStatus: 'edit' }))
      .toEqual({ base: '/biz/records/references/orgId', operation: 'edit' })
    expect(resolveIdentityReference({ field: 'external', props: { optionSource: { api: 'get@/contractors/options' } } }, context)).toBeNull()
    expect(resolveIdentityReference({ field: 'external', options: [{ value: 'contractor-A', label: '外部单位' }] }, context)).toBeNull()
    expect(resolveIdentityReference({ field: 'orgId', props: { optionSource: { api: 'get@/system/org/tree' } } }, context)).not.toBeNull()
    expect(resolveIdentityReference({ field: 'orgId' }, { ...context, modalStatus: 'detail' }).operation).toBeNull()
  })

  it('continues organization pages without assuming a total or loading descendants', async () => {
    const first = Array.from({ length: 200 }, (_, index) => ({ orgId: String(index + 1), parentId: '0', orgName: `Org ${index + 1}` }))
    const request = { get: vi.fn().mockResolvedValueOnce({ code: 200, data: first }).mockResolvedValueOnce({ code: 200, data: [{ orgId: '201', parentId: '0', orgName: 'Last' }] }) }
    const rows = await loadOrganizationBranch(request, reference)
    expect(rows).toHaveLength(201)
    expect(rows[200]).toMatchObject({ key: '201', value: '201', label: 'Last', isLeaf: false })
    expect(request.get.mock.calls[1][1].params).toEqual({ operation: 'add', parentId: undefined, afterOrgId: '200', limit: 200 })
    expect(request.get).toHaveBeenCalledTimes(2)
  })

  it('loads only an expanded parent and detects a stalled cursor', async () => {
    const request = { get: vi.fn().mockResolvedValue({ code: 200, data: Array.from({ length: 200 }, () => ({ orgId: '1', orgName: 'Repeated' })) }) }
    await expect(loadOrganizationBranch(request, reference, '10')).rejects.toThrow('cursor did not advance')
    expect(request.get.mock.calls[0][1].params.parentId).toBe('10')
  })

  it('rejects HTTP-success business failures and never queries for detail mode', async () => {
    const request = { get: vi.fn().mockResolvedValue({ code: 403, msg: '无权选择', data: [] }) }
    await expect(loadOrganizationBranch(request, reference)).rejects.toThrow('无权选择')
    request.get.mockClear()
    expect(await loadOrganizationBranch(request, { ...reference, operation: null })).toEqual([])
    expect(request.get).not.toHaveBeenCalled()
  })

  it('uses member cursors and a lookahead row without inventing total counts', async () => {
    const request = { get: vi.fn().mockResolvedValue({ code: 200, data: [101, 102, 103].map(userId => ({ userId: String(userId), username: `u${userId}`, displayName: '同名' })) }) }
    const page = await loadMemberPage(request, reference, { orgId: '10', keyword: '同名', afterUserId: '100', pageSize: 2 })
    expect(page.hasNext).toBe(true)
    expect(page.rows.map(row => row.id)).toEqual(['101', '102'])
    expect(request.get.mock.calls[0][1].params).toEqual({ operation: 'add', directOrgId: '10', keyword: '同名', afterUserId: '100', limit: 3 })
    expect(page).not.toHaveProperty('total')
  })

  it('requires an organization before browsing members', async () => {
    const request = { get: vi.fn() }
    expect(await loadMemberPage(request, reference, { pageSize: 20 })).toEqual({ rows: [], hasNext: false })
    expect(request.get).not.toHaveBeenCalled()
  })
})
