import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

describe('外部接口管理与系统客户端页：Naive UI provider 契约', () => {
  const manage = readSource('src/views/external/manage.vue')
  const client = readSource('src/views/system/client.vue')

  it('manage.vue 经 provider 获取 message/dialog，不使用 window 全局', () => {
    expect(manage).toContain('useMessage')
    expect(manage).toContain('useDialog')
    expect(manage).toContain('const message = useMessage()')
    expect(manage).toContain('const dialog = useDialog()')
    expect(manage).not.toContain('window.$message')
    expect(manage).not.toContain('window.$dialog')
  })

  it('manage.vue 使用与后端字段对齐的日志汇总/查询类型', () => {
    expect(manage).toContain('interface ExternalApiLogSummary')
    expect(manage).toContain('interface ExternalApiLogQuery')
    expect(manage).toContain('ref<ExternalApiLogSummary>({})')
    expect(manage).toContain('function buildLogParams(params: ExternalApiLogQuery = {})')
    expect(manage).toContain('totalCount?: number')
    expect(manage).toContain('maxDurationMs?: number')
  })

  it('client.vue 经 provider 获取 message，不使用 window 全局', () => {
    expect(client).toContain('useMessage')
    expect(client).toContain('const message = useMessage()')
    expect(client).not.toContain('window.$message')
  })
})
