import { describe, expect, it } from 'vitest'
import { shouldUseBusinessCodeComponentForm } from '../taskFormPresentation'

describe('business code task form presentation', () => {
  it('uses schema fields when the provider opts out of a historical page fallback', () => {
    expect(shouldUseBusinessCodeComponentForm({
      configured: true,
      formType: 'business-code',
      formUrl: '/security/civil-defense',
      providerKey: 'securityCivilDefense',
      formRef: { taskFormRenderMode: 'schema' },
      editMode: 'READONLY',
    })).toBe(false)
  })

  it('preserves legitimate custom components for other business code forms', () => {
    expect(shouldUseBusinessCodeComponentForm({
      configured: true,
      formType: 'business-code',
      formUrl: '/purchase-order/PurchaseOrderApproveForm',
      formRef: {},
    })).toBe(true)
  })

  it('does not select a component without a component URL', () => {
    expect(shouldUseBusinessCodeComponentForm({
      configured: true,
      formType: 'business-code',
      formRef: {},
    })).toBe(false)
  })
})
