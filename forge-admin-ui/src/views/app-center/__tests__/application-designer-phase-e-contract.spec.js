import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

describe('application designer phase E layout contract', () => {
  it('removes the duplicate embedded object navigation without affecting standalone mode', () => {
    const source = readSource('src/views/app-center/components/designer/BusinessObjectDesignerShell.vue')

    expect(source).toContain('\'nav-hidden\': !showDesignerNavigation')
    expect(source).toContain('v-if="showDesignerNavigation" class="designer-nav"')
    expect(source).not.toContain('closureSteps')
    expect(source).not.toContain('closure-steps')
    expect(source).toContain('.designer-shell.embedded.nav-hidden .designer-workbench')
  })

  it('uses the runtime navigation as the only editing sidebar and delegates page creation to the page-type selector', () => {
    const runtimeSource = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    const treeSource = readSource('src/views/app-center/components/ApplicationDesignerResourceTree.vue')

    // 页面创建统一走「新建页面」类型选择器（名称 + 页面类型 + 所属目录），不再直接落默认页
    expect(runtimeSource).toContain('@create-page="openPageTypeSelector()"')
    expect(runtimeSource).toContain('<aside v-if="!editing" class="runtime-navigation')
    expect(runtimeSource).toContain('createPageShapeBuilder(builder.value')
    expect(treeSource).toContain('emit(\'createPage\')')
  })

  it('renders the real application extension panel in the enhance view', () => {
    const source = readSource('src/views/app-center/application-runtime.[applicationCode].vue')

    // 增强面板由「增强」视图（runtimeViewMode === enhance 顶部页签）承载
    expect(source).toContain('runtimeViewMode === \'enhance\'')
    expect(source).toContain('switchRuntimeView(\'enhance\')')
    expect(source).toContain('<ApplicationExtensionsPanel')
    expect(source).toContain(':initial-extensions="workspaceExtensions"')
  })

  it('routes the legacy enhancement guide to the real designer resource', () => {
    const source = readSource('src/views/app-center/application.[applicationCode].vue')

    expect(source).toContain('designerSection: \'automation-enhancements\'')
    expect(source).toContain('const directResource = section === \'automation-enhancements\'')
    expect(source).toContain('directResource ? { designResource: section }')
  })
})
