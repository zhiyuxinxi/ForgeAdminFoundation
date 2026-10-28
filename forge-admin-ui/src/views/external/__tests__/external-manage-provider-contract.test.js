import { readFileSync } from 'node:fs'
import { URL } from 'node:url'
import { describe, expect, it } from 'vitest'

/**
 * 通用外部接口和客户端 provider 合同。
 *
 * 覆盖本轮被 typecheck 暴露的两个页面：
 * - src/views/external/manage.vue
 * - src/views/system/client.vue
 *
 * 必须保持：
 * 1. 不再引用未声明的 window.$message / window.$dialog 全局；
 * 2. 消息与对话框能力改由 Naive UI provider 组合式 API（useMessage/useDialog）提供；
 * 3. 外部接口日志汇总类型覆盖后端 ExternalApiLogSummary 的真实字段，
 *    日志查询参数类型覆盖 /external/api/log/summary 的真实入参。
 */
const readSource = relative => readFileSync(new URL(relative, import.meta.url), 'utf8')

const MANAGE_SOURCE = readSource('../manage.vue')
const CLIENT_SOURCE = readSource('../../system/client.vue')

/** 后端 ExternalApiLogSummary 的真实字段（forge-plugin-external/vo/ExternalApiLogSummary.java） */
const BACKEND_LOG_SUMMARY_FIELDS = [
  'totalCount',
  'successCount',
  'failureCount',
  'debugCount',
  'successRate',
  'avgDurationMs',
  'maxDurationMs',
]

/** 后端 getExternalApiLogSummary / clearExternalApiLogs 的真实查询参数 */
const BACKEND_LOG_QUERY_FIELDS = ['systemId', 'apiId', 'callStatus', 'debugFlag']

function interfaceBody(source, name) {
  return source.match(new RegExp(`interface\\s+${name}\\s*\\{([^}]*)\\}`))?.[1] ?? ''
}

describe('外部接口/系统客户端页面发布阻断修复契约', () => {
  it('不再引用未声明的 window.$message / window.$dialog 全局', () => {
    expect(MANAGE_SOURCE).not.toMatch(/window\.\$message/)
    expect(MANAGE_SOURCE).not.toMatch(/window\.\$dialog/)
    expect(CLIENT_SOURCE).not.toMatch(/window\.\$message/)
    expect(CLIENT_SOURCE).not.toMatch(/window\.\$dialog/)
  })

  it('消息与对话框能力由 Naive UI provider 组合式 API 提供', () => {
    expect(MANAGE_SOURCE).toMatch(/import\s*\{[^}]*useMessage[^}]*\}\s*from\s*'naive-ui'/)
    expect(MANAGE_SOURCE).toMatch(/import\s*\{[^}]*useDialog[^}]*\}\s*from\s*'naive-ui'/)
    expect(MANAGE_SOURCE).toMatch(/const message = useMessage\(\)/)
    expect(MANAGE_SOURCE).toMatch(/const dialog = useDialog\(\)/)
    expect(MANAGE_SOURCE).toMatch(/dialog\.warning\(\{/)

    expect(CLIENT_SOURCE).toMatch(/import\s*\{[^}]*useMessage[^}]*\}\s*from\s*'naive-ui'/)
    expect(CLIENT_SOURCE).toMatch(/const message = useMessage\(\)/)
  })

  it('日志汇总类型覆盖后端 ExternalApiLogSummary 的全部真实字段', () => {
    const summary = interfaceBody(MANAGE_SOURCE, 'ExternalApiLogSummary')
    expect(summary, '缺少 ExternalApiLogSummary 声明').not.toBe('')

    for (const field of BACKEND_LOG_SUMMARY_FIELDS)
      expect(summary, `ExternalApiLogSummary 缺少后端字段 ${field}`).toMatch(new RegExp(`\\b${field}\\??\\s*:`))

    expect(MANAGE_SOURCE).toMatch(/const logSummary = ref<ExternalApiLogSummary>\(\{\}\)/)
  })

  it('日志查询参数类型覆盖后端真实入参', () => {
    const query = interfaceBody(MANAGE_SOURCE, 'ExternalApiLogQuery')
    expect(query, '缺少 ExternalApiLogQuery 声明').not.toBe('')

    for (const field of BACKEND_LOG_QUERY_FIELDS)
      expect(query, `ExternalApiLogQuery 缺少后端字段 ${field}`).toMatch(new RegExp(`\\b${field}\\??\\s*:`))

    expect(MANAGE_SOURCE).toMatch(/function buildLogParams\(params: ExternalApiLogQuery = \{\}\)/)
  })
})
