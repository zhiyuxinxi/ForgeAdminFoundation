import { describe, expect, it } from 'vitest'
import { createViewSchemaFromPageSchema, mergeDesignerDraftFields, sanitizeViewSchemaFieldRefs } from '../viewSchema'

const reportTypeField = {
  fieldCode: 'reportType',
  fieldName: '上报类型',
  label: '上报类型',
  componentType: 'select',
  width: 168,
  sortable: false,
  dictType: 'accident_type',
}

describe('viewSchema page compiler', () => {
  it('prefers the saved list title while preserving the existing column configuration', () => {
    const schema = createViewSchemaFromPageSchema({
      zones: [{
        zoneKey: 'table',
        fieldRefs: ['reportType'],
        props: {
          fieldSettings: {
            reportType: {
              title: '事件类型（设计验证）',
              align: 'center',
              width: 220,
              fixed: 'left',
              sortable: true,
              renderType: 'dictTag',
            },
          },
        },
      }],
    }, [reportTypeField])

    expect(schema.list.columns).toEqual([expect.objectContaining({
      fieldCode: 'reportType',
      label: '事件类型（设计验证）',
      visible: true,
      order: 0,
      align: 'center',
      width: 220,
      fixed: 'left',
      sortable: true,
      formatter: 'dictTag',
    })])
  })

  it('falls back to the field name when no list display title is configured', () => {
    const schema = createViewSchemaFromPageSchema({
      zones: [{ zoneKey: 'table', fieldRefs: ['reportType'], props: { fieldSettings: {} } }],
    }, [reportTypeField])

    expect(schema.list.columns[0]).toMatchObject({
      label: '上报类型',
      width: 168,
      sortable: false,
      formatter: 'dictTag',
    })
  })

  it('preserves readonly business fields and untouched views when a form sync returns a partial catalog', () => {
    const currentFields = [
      {
        fieldCode: 'reportNo',
        fieldName: '事故编号',
        readonly: true,
        systemField: false,
        internal: false,
        basicProps: { required: true },
      },
      {
        fieldCode: 'reportType',
        fieldName: '上报类型',
        readonly: true,
        systemField: false,
        internal: false,
        advancedProps: { locked: true },
      },
      { fieldCode: 'reporter', fieldName: '上报人' },
    ]
    const syncedFields = [
      {
        fieldCode: 'reporter',
        fieldName: '上报人（布局调整）',
        basicProps: { span: 2 },
      },
    ]
    const viewSchema = {
      search: { fields: [{ fieldCode: 'reportNo' }, { fieldCode: 'reportType' }] },
      list: { columns: [{ fieldCode: 'reportNo' }, { fieldCode: 'reportType', label: '事件类型（设计验证）' }, { fieldCode: 'reporter' }] },
    }

    const mergedFields = mergeDesignerDraftFields(currentFields, syncedFields)
    const sanitizedViewSchema = sanitizeViewSchemaFieldRefs(viewSchema, mergedFields)

    expect(mergedFields).toEqual([
      currentFields[0],
      currentFields[1],
      expect.objectContaining({
        fieldCode: 'reporter',
        fieldName: '上报人（布局调整）',
        basicProps: { span: 2 },
      }),
    ])
    expect(sanitizedViewSchema.search.fields.map(item => item.fieldCode)).toEqual(['reportNo', 'reportType'])
    expect(sanitizedViewSchema.list.columns).toEqual([
      expect.objectContaining({ fieldCode: 'reportNo' }),
      expect.objectContaining({ fieldCode: 'reportType', label: '事件类型（设计验证）' }),
      expect.objectContaining({ fieldCode: 'reporter' }),
    ])
  })

  it('keeps the configured detail display name ahead of the field fallback', () => {
    const schema = createViewSchemaFromPageSchema({
      zones: [{
        zoneKey: 'detail',
        fieldRefs: ['reportType'],
        props: {
          fieldSettings: {
            reportType: { title: '事件类型（详情验证）' },
          },
        },
      }],
    }, [reportTypeField])

    expect(schema.detail.sections[0].fields[0]).toMatchObject({
      fieldCode: 'reportType',
      label: '事件类型（详情验证）',
      visible: true,
      readonly: true,
      formatter: 'dictTag',
    })
  })
})
