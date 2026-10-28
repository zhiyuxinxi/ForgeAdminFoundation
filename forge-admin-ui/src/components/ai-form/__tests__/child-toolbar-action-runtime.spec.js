import { describe, expect, it } from 'vitest'
import { resolveChildToolbarAction } from '../child-toolbar-action-runtime'

describe('child toolbar actions', () => {
  it('selects the declared relation and preserves string identifiers and arbitrary input fields', () => {
    const id = '9223372036854775806'
    const result = resolveChildToolbarAction({
      action: {
        actionCode: 'inspect',
        actionConfig: JSON.stringify({
          recordSelector: { relationKey: 'equipment', label: '设备', labelField: 'equipmentName' },
          inputSchema: [{ name: 'reason', label: '原因', type: 'text', required: true, defaultValue: '定期检查' }],
        }),
      },
      child: { relationKey: 'inspection_logs' },
      childrenData: { equipment: [{ id, equipmentName: 'Pump' }, { equipmentName: 'Unsaved' }], inspection_logs: [{ id: 'wrong' }] },
      selectedKey: id,
    })
    expect(result.relationKey).toBe('equipment')
    expect(result.options).toEqual([{ value: id, label: 'Pump' }])
    expect(result.selectedRecord.id).toBe(id)
    expect(result.formSchema).toMatchObject([{ field: 'reason', required: true, type: 'input' }])
    expect(result.initialData).toEqual({ reason: '定期检查' })
  })

  it.each([
    ['record_pickup', '提货数量', 7],
    ['record_return', '退货数量', 3],
  ])('preserves the old %s configuration without a data migration', (actionCode, label, max) => {
    const result = resolveChildToolbarAction({
      action: { actionCode, relationKey: 'presale_items' },
      child: { relationKey: 'operation_logs' },
      childrenData: { presale_items: [{ id: '8', productName: 'Item', pendingQuantity: 7, pickedQuantity: 3 }] },
      selectedKey: '8',
    })
    expect(result.relationKey).toBe('presale_items')
    expect(result.formSchema).toMatchObject([{ field: 'quantity', label, type: 'input-number', min: 1, max, required: true }])
    expect(result.initialData).toEqual({ quantity: 1 })
    expect(result.options[0].label).toContain('Item')
  })

  it('does not apply legacy business semantics to another explicitly declared relation', () => {
    const result = resolveChildToolbarAction({
      action: { actionCode: 'record_return', actionConfig: { relationKey: 'equipment' } },
      childrenData: { equipment: [{ id: '20' }], presale_items: [{ id: '10' }] },
      selectedKey: '20',
    })
    expect(result.relationKey).toBe('equipment')
    expect(result.formSchema).toEqual([])
    expect(result.selectedRecord.id).toBe('20')
  })

  it('preserves an explicit zero limit and does not invent a selected record', () => {
    const input = {
      action: { actionCode: 'record_return', relationKey: 'presale_items' },
      childrenData: { presale_items: [{ id: '8', pickedQuantity: 0 }] },
    }
    expect(resolveChildToolbarAction({ ...input, selectedKey: '8' }).formSchema[0].max).toBe(0)
    expect(resolveChildToolbarAction({ ...input, selectedKey: 'missing' }).selectedRecord).toBeNull()
  })

  it('honors an explicitly configured form instead of adding legacy quantity fields', () => {
    const formSchema = [{ field: 'reason', type: 'input', label: '原因', required: true }]
    const result = resolveChildToolbarAction({
      action: { actionCode: 'record_return', relationKey: 'presale_items', actionConfig: { formSchema, defaultValues: { reason: 'Damaged' } } },
    })
    expect(result.formSchema).toEqual(formSchema)
    expect(result.initialData).toEqual({ reason: 'Damaged' })
  })

  it('uses the current child relation when a new action has no relation override', () => {
    const result = resolveChildToolbarAction({
      action: { actionCode: 'record_return' },
      child: { relationKey: 'equipment' },
      childrenData: { equipment: [{ id: '8' }] },
    })
    expect(result.relationKey).toBe('equipment')
    expect(result.formSchema).toEqual([])
  })
})
