import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

const root = process.cwd()
const page = readFileSync(path.resolve(root, 'src/views/flow/cc.vue'), 'utf8')
const api = readFileSync(path.resolve(root, 'src/api/flow.js'), 'utf8')
const businessApi = readFileSync(path.resolve(root, 'src/api/business-app.js'), 'utf8')

describe('public CC recipient form contract', () => {
  it('本人列表不再向后端提交可伪造的 userId', () => {
    expect(page).toContain("flowApi.getMyCc(params)")
    expect(page).not.toContain('userId: userStore.userId')
    expect(api).toContain("request.get('/api/flow/cc/my', { params })")
    expect(api).toContain("getUnreadCcCount: () =>")
    expect(api).toContain("request.get('/api/flow/cc/unread/count')")
    expect(api).not.toContain("getUnreadCcCount: userId =>")
  })

  it('接收页面保持业务表单只读且不向收件角色展示发送页签', () => {
    expect(page).toContain(':read-only="true"')
    expect(page).toContain('v-if="canViewSent" name="sent"')
    expect(page).toContain("permissions.includes('flow:cc:sent')")
  })

  it('业务代码表单通过本人抄送上下文只读渲染且不打开完整业务列表', () => {
    expect(businessApi).toContain('/ai/business/flow/task-form-context/cc/')
    expect(page).toContain('businessCcTaskFormReadonlyContext(ccId)')
    expect(page).toContain('v-else-if="useCcBusinessCodeForm"')
    expect(page).toContain(':show-actions="false"')
    expect(page).not.toContain('openBusinessCodeForm')
    expect(page).not.toContain('router.push')
  })

})
