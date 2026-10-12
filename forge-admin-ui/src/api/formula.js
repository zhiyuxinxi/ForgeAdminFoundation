import { request } from '@/utils'

const BASE = '/api/ai/business/formula'

/** POST /api/ai/business/formula/validate */
export function validateFormula(data) {
  return request.post(`${BASE}/validate`, data)
}

/** POST /api/ai/business/formula/preview */
export function previewFormula(data) {
  return request.post(`${BASE}/preview`, data)
}

/** POST /api/ai/business/formula/dependency */
export function analyzeDependency(data) {
  return request.post(`${BASE}/dependency`, data)
}

/** POST /api/ai/business/formula/debug */
export function debugFormula(data) {
  return request.post(`${BASE}/debug`, data)
}

/** GET /api/ai/business/formula/log/page */
export function getFormulaLogPage(params) {
  return request.get(`${BASE}/log/page`, { params })
}

/** GET /api/ai/business/formula/log/{id} */
export function getFormulaLogDetail(id) {
  return request.get(`${BASE}/log/${id}`)
}

/** POST /api/ai/business/formula/dependency/graph */
export function getDependencyGraph(data) {
  return request.post(`${BASE}/dependency/graph`, data)
}

/** POST /api/ai/business/formula/rule/compile */
export function compileConditionRule(data) {
  return request.post(`${BASE}/rule/compile`, data)
}

/** POST /api/ai/business/formula/rule/validate */
export function validateConditionRule(data) {
  return request.post(`${BASE}/rule/validate`, data)
}

/** GET /api/ai/business/formula/functions */
export function getFormulaFunctions() {
  return request.get(`${BASE}/functions`)
}

/** GET /api/ai/business/formula/function-market/page */
export function getFormulaFunctionMarketPage(params) {
  return request.get(`${BASE}/function-market/page`, { params })
}

/** GET /api/ai/business/formula/function-market/{functionCode} */
export function getFormulaFunctionMarketDetail(functionCode) {
  return request.get(`${BASE}/function-market/${functionCode}`)
}

/** POST /api/ai/business/formula/function-market/install */
export function installFormulaFunction(data) {
  return request.post(`${BASE}/function-market/install`, data)
}

/** POST /api/ai/business/formula/function-market/custom */
export function registerFormulaFunction(data) {
  return request.post(`${BASE}/function-market/custom`, data)
}

/** PUT /api/ai/business/formula/function-market/{functionCode}/enable */
export function enableFormulaFunction(functionCode) {
  return request.put(`${BASE}/function-market/${functionCode}/enable`)
}

/** PUT /api/ai/business/formula/function-market/{functionCode}/disable */
export function disableFormulaFunction(functionCode) {
  return request.put(`${BASE}/function-market/${functionCode}/disable`)
}
