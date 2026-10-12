import { describe, expect, it } from 'vitest'
import { isPageFieldVisible } from '@/components/lowcode-builder/page/page-schema'
import {
  appendCodeAppAsset,
  codeAppAssetKey,
  findCodeAppAssetMatch,
  mergeCodeAppFieldCapabilities,
  preserveCodeAppFieldCapabilities,
  selectCodeAppCatalogAssets,
} from '../code-app-designer-assets'

const accidentPrimary = {
  objectCode: 'accident_report',
  formKey: 'report_form',
  formMode: 'BUSINESS_CODE_FORM',
  providerKey: 'accidentProvider',
  fields: [{ field: 'accidentType', label: '事故类型' }],
}

const mixedCatalog = [
  accidentPrimary,
  {
    ...accidentPrimary,
    providerKey: 'purchaseProvider',
    fields: [{ field: 'purchaseNo', label: '采购单号' }],
  },
  {
    ...accidentPrimary,
    formMode: 'BUSINESS_OBJECT_FORM',
    type: 'BUSINESS_OBJECT_FORM',
    providerKey: '',
    fields: [{ field: 'idCard', label: '人员身份证' }],
  },
  {
    objectCode: 'purchase_order',
    formKey: 'purchase_form',
    formMode: 'BUSINESS_CODE_FORM',
    providerKey: 'purchaseProvider',
    fields: [{ field: 'supplierName', label: '供应商' }],
  },
]

describe('code app designer asset resolution', () => {
  it('仅用明确选择的provider、mode和formKey补齐当前资产', () => {
    const selected = selectCodeAppCatalogAssets([{
      objectCode: 'accident_report',
      formKey: 'report_form',
      formMode: 'BUSINESS_CODE_FORM',
      providerKey: 'accidentProvider',
    }], mixedCatalog, 'accident_report')

    expect(selected).toEqual([accidentPrimary])
    expect(selected.flatMap(asset => asset.fields).map(field => field.field)).toEqual(['accidentType'])
  })

  it('当前对象没有已选资产时只采用目录中属于当前对象的多表单', () => {
    const secondaryForm = {
      ...accidentPrimary,
      formKey: 'report_review_form',
      fields: [{ field: 'reviewResult', label: '复核结果' }],
    }
    const selected = selectCodeAppCatalogAssets(
      [],
      [...mixedCatalog, secondaryForm],
      'accident_report',
    )

    expect(selected).toEqual([
      accidentPrimary,
      mixedCatalog[1],
      mixedCatalog[2],
      secondaryForm,
    ])
    expect(selected).not.toContain(mixedCatalog[3])
  })

  it('metadata没有匹配项时允许空configured并保留资产字段', () => {
    const result = []
    const seenKeys = new Set()

    expect(codeAppAssetKey(null)).toBe('')
    expect(() => appendCodeAppAsset(result, seenKeys, accidentPrimary, null)).not.toThrow()
    expect(result).toEqual([accidentPrimary])
    expect(result[0].fields).toEqual([{ field: 'accidentType', label: '事故类型' }])
  })

  it('同formKey不同provider或mode保持独立身份', () => {
    expect(findCodeAppAssetMatch(mixedCatalog, accidentPrimary)).toEqual(accidentPrimary)
    expect(codeAppAssetKey(accidentPrimary)).not.toBe(codeAppAssetKey(mixedCatalog[1]))
    expect(codeAppAssetKey(accidentPrimary)).not.toBe(codeAppAssetKey(mixedCatalog[2]))
  })

  it('provider查询能力覆盖旧false元数据且保存重开不丢失查询候选', () => {
    const providerFields = [
      { field: 'status', readonly: true, searchable: true },
      { field: 'reportType', readonly: true, searchable: true },
      { field: 'level', readonly: true, searchable: true },
      { field: 'title', readonly: true, searchable: true },
      { field: 'reportNo', readonly: true, searchable: false },
      { field: 'location', readonly: false, searchable: false },
      { field: 'createTime', readonly: true, systemField: true, searchable: true },
    ]
    const legacyMetadata = providerFields
      .filter(field => typeof field.searchable === 'boolean')
      .map(field => ({ field: field.field, label: field.field, searchable: false }))
    const reopened = providerFields.map((providerField) => {
      const configured = legacyMetadata.find(field => field.field === providerField.field)
      return preserveCodeAppFieldCapabilities(mergeCodeAppFieldCapabilities(providerField, configured))
    })

    expect(reopened.filter(field => isPageFieldVisible(field, 'search')).map(field => field.field))
      .toEqual(['status', 'reportType', 'level', 'title'])
    expect(reopened.find(field => field.field === 'location').searchable).toBe(false)
    expect(reopened.find(field => field.field === 'reportNo').searchable).toBe(false)
    expect(reopened.find(field => field.field === 'status').readonly).toBe(true)
    expect(preserveCodeAppFieldCapabilities({ field: 'undeclared' })).not.toHaveProperty('searchable')
  })
})
