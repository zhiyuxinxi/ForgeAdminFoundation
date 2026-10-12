import { afterEach, expect, it } from 'vitest'
import { setLocale } from '@/locales'
import { getBusinessFormDisplayTitle, getRowDisplayTitle, getTaskDisplayName } from '../processDisplay'

afterEach(() => setLocale('zh-CN'))

it('localizes workflow display metadata after stripping technical node codes', () => {
  setLocale('en-US')
  expect(getTaskDisplayName({ taskName: '部门负责人审批 (approve_step)', taskDefKey: 'approve_step' })).toBe('Department head approval')
  expect(getRowDisplayTitle({ modelName: '采购单审批测试流程' })).toBe('Purchase order approval test workflow')
  expect(getRowDisplayTitle({ businessObjectName: '采购申请' })).toBe('Purchase request')
  expect(getBusinessFormDisplayTitle({ businessObjectName: '采购申请', businessSummary: '原始采购主题' })).toBe('Purchase request · 原始采购主题')
  setLocale('zh-CN')
  expect(getTaskDisplayName({ taskName: '部门负责人审批 (approve_step)', taskDefKey: 'approve_step' })).toBe('部门负责人审批')
})

it('keeps customer summaries, historical titles and unknown metadata intact', () => {
  setLocale('en-US')
  const source = { businessSummary: '客户填写的采购说明', modelName: '请假申请' }
  expect(getRowDisplayTitle(source)).toBe('客户填写的采购说明')
  expect(source.modelName).toBe('请假申请')
  expect(getRowDisplayTitle({ processTitle: '张三 的请假申请' })).toBe('张三 的请假申请')
  expect(getTaskDisplayName({ taskName: '客户自定义环节' })).toBe('客户自定义环节')
  expect(getBusinessFormDisplayTitle({ businessSummary: '客户填写的采购说明' }, 'Business form')).toBe('客户填写的采购说明')
})
