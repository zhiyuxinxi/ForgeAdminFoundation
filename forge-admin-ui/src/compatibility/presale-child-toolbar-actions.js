import { localizeLabel, t } from '@/locales'

// Compatibility for the public V1.0.105/V1.0.113 seed configuration. New actions
// declare their selector and inputSchema instead of extending this legacy map.
export function resolveLegacyChildToolbarAction(action, selectedRecord) {
  const code = action?.actionCode || action?.key
  if (!['record_pickup', 'record_return'].includes(code))
    return null
  if (action.relationKey && action.relationKey !== 'presale_items')
    return null

  const isReturn = code === 'record_return'
  return {
    recordSelector: {
      relationKey: action.relationKey || 'presale_items',
      label: localizeLabel('商品'),
      placeholder: localizeLabel('请选择商品'),
      emptyMessage: localizeLabel('请先添加并保存商品明细'),
      formatLabel: item => t('customer.pickupSummary', {
        value0: item.productName || localizeLabel('商品'),
        value1: item.pendingQuantity ?? 0,
        value2: item.pickedQuantity ?? 0,
      }),
    },
    inputSchema: [{
      name: 'quantity',
      label: localizeLabel(isReturn ? '退货数量' : '提货数量'),
      type: 'integer',
      required: true,
      defaultValue: 1,
      min: 1,
      max: selectedRecord ? (selectedRecord[isReturn ? 'pickedQuantity' : 'pendingQuantity'] ?? 0) : 999999,
    }],
  }
}
