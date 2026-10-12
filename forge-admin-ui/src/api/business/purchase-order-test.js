import { request } from '@/utils'

const BASE = '/business/sample-purchase-order'

export function purchaseOrderPage(params) {
  return request.get(`${BASE}/page`, { params })
}

export function purchaseOrderDetail(query) {
  const params = query && typeof query === 'object' ? { ...query } : { id: query }
  if (params.id !== null && params.id !== undefined)
    params.id = String(params.id)
  return request.post(`${BASE}/getById`, null, { params })
}

export function createPurchaseOrder(data) {
  return request.post(`${BASE}/add`, data)
}

export function updatePurchaseOrder(data) {
  return request.post(`${BASE}/edit`, data)
}

export function removePurchaseOrder(id) {
  return request.post(`${BASE}/remove/${encodeURIComponent(String(id))}`)
}

export function submitPurchaseOrder(id, data) {
  return request.post(`${BASE}/submit/${encodeURIComponent(String(id))}`, data)
}

export function savePurchaseOrderTaskFields(data) {
  return request.post(`${BASE}/task/save`, data)
}

export function initPurchaseOrderFlow() {
  return request.post(`${BASE}/init-flow`)
}
