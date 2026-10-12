import { describe, expect, it } from 'vitest'
import { useCodeAppMetadata } from '../useCodeAppMetadata'

describe('useCodeAppMetadata display labels', () => {
  it('prefers the saved form component label and supports field aliases', () => {
    const codeAppMetadata = useCodeAppMetadata('accident_report')
    codeAppMetadata.metadata.value = {
      fields: [{ field: 'reportType', label: '上报类型' }],
      formDesignerSchema: {
        components: [{
          id: 'report_type',
          componentKey: 'select',
          label: '上报类型（表单验证）',
          fieldBinding: { mode: 'field', fieldCode: 'reportType' },
        }],
        forms: [{
          formKey: 'accident_report_form',
          schema: {
            components: [{
              id: 'report_type_copy',
              componentKey: 'select',
              label: '复制表单名称不应覆盖主表单',
              fieldBinding: { mode: 'field', fieldCode: 'reportType' },
            }],
          },
        }],
      },
    }

    expect(codeAppMetadata.formFieldLabel('reportType', '上报类型')).toBe('上报类型（表单验证）')
    expect(codeAppMetadata.formFieldLabel('report_type', '上报类型')).toBe('上报类型（表单验证）')
  })

  it('falls back to field metadata when the form has no configured component label', () => {
    const codeAppMetadata = useCodeAppMetadata('accident_report')
    codeAppMetadata.metadata.value = {
      fields: [{ field: 'reportType', label: '上报类型' }],
      formDesignerSchema: { components: [] },
    }

    expect(codeAppMetadata.formFieldLabel('reportType', '默认名称')).toBe('上报类型')
  })
})
