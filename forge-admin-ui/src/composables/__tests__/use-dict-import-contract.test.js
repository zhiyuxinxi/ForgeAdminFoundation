import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * useDict 的 HTTP 依赖来源契约。
 *
 * `@/utils` 是 14 个模块的聚合桶（crypto / sse / websocket / collaboration-runtime /
 * offline-draft-runtime / encrypt-request / file / storage …）。从聚合桶取 `request`
 * 会把这些模块连带拉进每一个使用字典的页面导入图，放大首轮导入成本。
 * `@/utils/http` 导出的是同一个 axios 实例，因此必须从具体模块导入。
 *
 * 读取方式沿用仓库既有测试的 Node 路径约定（`path.resolve(process.cwd(), 'src/...')`），
 * 不在 jsdom 环境下依赖全局 `URL` 实例。
 */
const SOURCE = readFileSync(path.resolve(process.cwd(), 'src/composables/useDict.js'), 'utf8')

describe('useDict 依赖来源契约', () => {
  it('request 从具体模块 @/utils/http 导入，而不是聚合桶 @/utils', () => {
    expect(SOURCE).toMatch(/import\s*\{\s*request\s*\}\s*from\s*'@\/utils\/http'/)
    expect(SOURCE).not.toMatch(/import\s*\{[^}]*\brequest\b[^}]*\}\s*from\s*'@\/utils'/)
  })
})
