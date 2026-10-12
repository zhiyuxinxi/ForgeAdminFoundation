import { translateBpmn } from '@/components/bpmn/bpmn-locale'
import { afterEach, expect, it, vi } from 'vitest'
import { createFieldFromGenColumn } from '@/components/lowcode-builder/model/model-schema'
import { createComponentFromField, normalizeFormDesignerSchema, updateDesignerComponent } from '@/views/app-center/components/designer/form-first/formDesignerSchema'
import { forgeSchemaToFormCreate } from '@/views/app-center/components/designer/form-first/forgeToFormCreate'
import { formCreateToForgeSchema } from '@/views/app-center/components/designer/form-first/formCreateToForge'
import { localizeMetadata } from '../metadata'
import { formatUiText, localizeLabel, setLocale } from '../index'
import { installForgeBusinessComponents } from '@/views/app-center/components/designer/form-first/forgeBusinessComponents'

vi.mock('@/utils', () => ({ request: { get: vi.fn().mockResolvedValue({ data: [] }) } }))

afterEach(() => setLocale('zh-CN'))

it('preserves bilingual field metadata across model, designer and form-create round trips', () => {
  const field = { ...createFieldFromGenColumn({ javaField: 'reference', columnName: 'reference', columnComment: '参考编号', columnCommentEn: 'Reference number', dictLabelsI18n: { WAITING: { 'en-US': 'Pending' } } }), placeholderI18n: { 'en-US': 'Enter reference number' }, defaultValue: '用户原文' }
  expect(field.dictLabelsI18n).toEqual({ WAITING: { 'en-US': 'Pending' } })
  const component = createComponentFromField(field)
  component.props.defaultValue = '用户原文'
  const schema = normalizeFormDesignerSchema({ components: [component] })
  const converted = forgeSchemaToFormCreate({ schema, fields: [field] })
  expect(converted.rules[0].titleI18n).toEqual({ 'en-US': 'Reference number' })
  expect(converted.rules[0].value).toBe('用户原文')
  setLocale('en-US')
  expect(localizeMetadata(converted.rules)[0].title).toBe('Reference number')
  expect(converted.rules[0].title).toBe('参考编号')
  const restored = formCreateToForgeSchema({ rules: converted.rules, options: converted.options, fields: [field] })
  expect(restored.components[0].labelI18n).toEqual(field.labelI18n)
  expect(restored.components[0].props.placeholderI18n).toEqual(field.placeholderI18n)
  expect(restored.components[0].fieldBinding.fieldCode).toBe('reference')
  const edited = updateDesignerComponent(restored, restored.components[0].id, { labelI18n: { 'en-US': 'Reference ID' } })
  expect(edited.components[0].labelI18n['en-US']).toBe('Reference ID')
  expect(edited.components[0].label).toBe('参考编号')
})

it('localizes public designer chrome while retaining unknown and user-provided values', () => {
  setLocale('en-US')
  expect(localizeLabel('目标页面')).toBe('Target page')
  expect(localizeLabel('字段英文名称')).toBe('English field name')
  const record = { value: '目标页面', defaultValue: '目标页面', data: { title: '目标页面' }, label: '目标页面' }
  expect(localizeMetadata(record)).toEqual({ ...record, label: 'Target page' })
  expect(record.label).toBe('目标页面')
})

it('refreshes designer palette chrome without changing saved rules or duplicating menu entries', async () => {
  const canvas = [{ field: 'ref', value: '未保存草稿', title: '业务输入' }]
  const designer = {
    menu: null,
    addMenu(menu) { this.menu = menu },
    addComponent(items) { this.menu.list.push(...items) },
    setBaseRuleConfig: vi.fn(),
    setComponentRuleConfig: vi.fn(),
    getRule: () => canvas,
    setRule: vi.fn(),
  }
  installForgeBusinessComponents(designer)
  const names = designer.menu.list.map(item => item.name)
  setLocale('en-US')
  installForgeBusinessComponents(designer)
  await Promise.resolve()
  expect(designer.menu.list.map(item => item.name)).toEqual(names)
  expect(designer.menu.list.find(item => item.name === 'forgeFileUpload').label).toBe('File upload')
  expect(designer.getRule()).toBe(canvas)
  expect(designer.getRule()[0].value).toBe('未保存草稿')
  expect(designer.setRule).not.toHaveBeenCalled()
  expect(formatUiText('确定删除“{0}”吗？', ['用户{1}原文'])).toBe('Delete "用户{1}原文"?')
})

it('uses BPMN library language and preserves replacement values verbatim', () => {
  setLocale('zh-CN')
  expect(translateBpmn('Create task')).toBe('创建任务')
  expect(translateBpmn('Open {element}', { element: '业务{element}' })).toBe('打开 业务{element}')
  setLocale('en-US')
  expect(translateBpmn('Create task')).toBe('Create task')
  expect(translateBpmn('Open {element}', { element: '业务{element}' })).toBe('Open 业务{element}')
})
