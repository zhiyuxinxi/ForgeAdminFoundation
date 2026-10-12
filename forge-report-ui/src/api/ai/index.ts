import { uiText } from '@/i18n/display'
import { del, get, post, put } from '@/api/http'
import { requestLanguage } from '@/i18n/locale'
import { StorageEnum } from '@/enums/storageEnum'
import { getLocalStorage } from '@/utils/storage'
import type { AIGenerateRequest, AIGenerateResponse } from './ai.d'

interface ApiResponse<T = any> {
  code: number
  data: T
  msg: string
}

// ==================== 类型定义 ====================

export interface AiProviderTemplate {
  templateKey: string
  name: string
  baseUrl: string
  defaultModel: string
}

export interface AiProvider {
  id?: number | string
  providerName?: string
  providerType?: string
  apiKey?: string
  baseUrl?: string
  models?: string
  defaultModel?: string
  isDefault?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface AiChatSession {
  id: string
  userId?: number
  agentCode?: string
  sessionName?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export interface AiChatRecord {
  id?: number
  sessionId?: string
  userId?: number
  agentCode?: string
  role: 'user' | 'assistant' | 'system'
  content: string
  tokenUsage?: number
  createTime?: string
}

export interface AiChatStreamRequest {
  content: string
  agentCode?: string
  sessionId?: string
  projectName?: string
  canvasContext?: string
  providerId?: number | string
  modelName?: string
  temperature?: number
  maxTokens?: number
}

export interface AiGenerateStreamRequest {
  prompt: string
  sessionId?: string
  style?: string
  canvasWidth?: number
  canvasHeight?: number
  componentCatalog?: string
  projectName?: string
  canvasContext?: string
  businessDefinitionId?: number | string
  businessContext?: string
  providerId?: number | string
  modelName?: string
  temperature?: number
  maxTokens?: number
  promptTemplateId?: number | string
  promptTemplateName?: string
}

export interface AiDashboardGenerateRecord {
  id?: number | string
  sessionId?: string
  projectId?: number | string
  projectName?: string
  businessDefinitionId?: number | string
  businessName?: string
  providerId?: number | string
  providerName?: string
  modelName?: string
  style?: string
  canvasWidth?: number
  canvasHeight?: number
  prompt?: string
  requestJson?: string
  generatedTitle?: string
  responseJson?: string
  validationSummaryJson?: string
  status?: 'success' | 'failed' | 'parse_failed' | 'stopped' | string
  componentCount?: number
  boundCount?: number
  staticCount?: number
  staticFallbackCount?: number
  repairedCount?: number
  elapsedMs?: number
  errorMessage?: string
  createTime?: string
  updateTime?: string
}

export interface AiDashboardGenerateRecordSaveRequest {
  sessionId?: string
  projectId?: number | string
  projectName?: string
  businessDefinitionId?: number | string
  businessName?: string
  providerId?: number | string
  providerName?: string
  modelName?: string
  style?: string
  canvasWidth?: number
  canvasHeight?: number
  prompt?: string
  requestJson?: string
  generatedTitle?: string
  responseJson?: string
  validationSummaryJson?: string
  status?: string
  componentCount?: number
  boundCount?: number
  staticCount?: number
  staticFallbackCount?: number
  repairedCount?: number
  elapsedMs?: number
  errorMessage?: string
  lineageItems?: AiDashboardComponentLineageItem[]
}

export interface AiDashboardComponentLineageItem {
  componentIndex?: number
  componentKey?: string
  componentTitle?: string
  datasetId?: number | string
  datasetName?: string
  fieldNames?: string[]
  bindingStatus?: string
}

export interface AiDashboardDatasetImpactItem {
  lineageId?: number | string
  recordId?: number | string
  projectId?: number | string
  projectName?: string
  businessDefinitionId?: number | string
  businessName?: string
  componentIndex?: number
  componentKey?: string
  componentTitle?: string
  datasetId?: number | string
  datasetName?: string
  fieldNames?: string
  bindingStatus?: string
  generatedTitle?: string
  recordStatus?: string
  createTime?: string
}

export interface AiPromptTemplate {
  id?: number | string
  templateName?: string
  templateCode?: string
  usageScene?: string
  businessCategory?: string
  domainCategory?: string
  templateTags?: string
  description?: string
  contentSummary?: string
  promptContent?: string
  exampleInput?: string
  status?: string
  isRecommended?: string
  sortOrder?: number
  useCount?: number
  testCount?: number
  downloadCount?: number
  creatorName?: string
  createTime?: string
  updateTime?: string
}

// ==================== AI 生成 ====================

/** AI 生成大屏 */
export const aiGenerate = async (data: AIGenerateRequest) => {
  const res = await (post('/forge-report-api/ai/generate', data) as unknown as Promise<ApiResponse<AIGenerateResponse | string>>)
  if (typeof res.data === 'string') {
    return JSON.parse(res.data) as AIGenerateResponse
  }
  return res.data
}

/** AI 流式生成大屏 */
export const aiGenerateStream = async (
  data: AiGenerateStreamRequest,
  onChunk: (chunk: string) => void,
  onDone: (fullText: string) => void,
  onError: (error: Error) => void,
  signal?: AbortSignal
) => {
  return consumeAiSse('/forge-report-api/ai/generate/stream', data, onChunk, onDone, onError, signal, 'AI 生成请求失败')
}

/** 保存 AI 大屏生成记录 */
export const saveDashboardGenerateRecordApi = (data: AiDashboardGenerateRecordSaveRequest) =>
  post('/forge-report-api/ai/dashboard-generate-record', data) as unknown as Promise<ApiResponse<AiDashboardGenerateRecord>>

/** 获取当前用户最近 AI 大屏生成记录 */
export const getDashboardGenerateRecentApi = (params?: {
  businessDefinitionId?: number | string
  projectId?: number | string
  limit?: number
}) =>
  get('/forge-report-api/ai/dashboard-generate-record/recent', params) as unknown as Promise<ApiResponse<AiDashboardGenerateRecord[]>>

/** 删除 AI 大屏生成记录 */
export const deleteDashboardGenerateRecordApi = (id: number | string) =>
  del(`/forge-report-api/ai/dashboard-generate-record/${id}`) as unknown as Promise<ApiResponse>

/** 查询数据集影响的大屏组件 */
export const getDashboardDatasetImpactApi = (datasetId: number | string, params?: { limit?: number }) =>
  get(`/forge-report-api/ai/dashboard-generate-record/impact/dataset/${datasetId}`, params) as unknown as Promise<ApiResponse<AiDashboardDatasetImpactItem[]>>

/** 查询启用的提示词模板 */
export const getPromptTemplateListApi = (params?: {
  keyword?: string
  usageScene?: string
  businessCategory?: string
  domainCategory?: string
  limit?: number
}) =>
  get('/forge-report-api/ai/prompt-template/list', params) as unknown as Promise<ApiResponse<AiPromptTemplate[]>>

/** 预览提示词模板 */
export const previewPromptTemplateApi = (id: number | string) =>
  get(`/forge-report-api/ai/prompt-template/${id}/preview`) as unknown as Promise<ApiResponse<AiPromptTemplate>>

/** 使用提示词模板并计数 */
export const usePromptTemplateApi = (id: number | string) =>
  post(`/forge-report-api/ai/prompt-template/${id}/use`) as unknown as Promise<ApiResponse<AiPromptTemplate>>

/** 下载提示词模板并计数 */
export const downloadPromptTemplateApi = (id: number | string) =>
  post(`/forge-report-api/ai/prompt-template/${id}/download`) as unknown as Promise<ApiResponse<AiPromptTemplate>>

// ==================== AI 供应商管理 ====================

/** 获取内置供应商预设模板列表 */
export const getProviderTemplatesApi = () =>
  get('/forge-report-api/ai/provider/templates') as unknown as Promise<ApiResponse<AiProviderTemplate[]>>

/** 分页查询已配置的供应商 */
export const getProviderPageApi = (params?: { pageNum?: number; pageSize?: number }) =>
  get('/forge-report-api/ai/provider/page', params) as unknown as Promise<ApiResponse<{ records: AiProvider[]; total: number }>>

/** 查询供应商详情 */
export const getProviderDetailApi = (id: number | string) =>
  get(`/forge-report-api/ai/provider/${id}`) as unknown as Promise<ApiResponse<AiProvider>>

/** 创建供应商 */
export const createProviderApi = (data: AiProvider) =>
  post('/forge-report-api/ai/provider', data) as unknown as Promise<ApiResponse>

/** 更新供应商 */
export const updateProviderApi = (data: AiProvider) =>
  put('/forge-report-api/ai/provider', data) as unknown as Promise<ApiResponse>

/** 删除供应商 */
export const deleteProviderApi = (id: number | string) =>
  del(`/forge-report-api/ai/provider/${id}`) as unknown as Promise<ApiResponse>

/** 测试供应商连接（传入 baseUrl + apiKey + defaultModel） */
export const testProviderApi = (data: Pick<AiProvider, 'providerName' | 'baseUrl' | 'apiKey' | 'defaultModel'>) =>
  post('/forge-report-api/ai/provider/test', data) as unknown as Promise<ApiResponse<string>>

/** 设为默认供应商 */
export const setDefaultProviderApi = (id: number | string) =>
  put(`/forge-report-api/ai/provider/${id}/default`) as unknown as Promise<ApiResponse>

// ==================== AI 会话管理 ====================

/** 获取当前用户历史会话列表 */
export const getSessionListApi = () =>
  get('/forge-report-api/ai/session/list') as unknown as Promise<ApiResponse<AiChatSession[]>>

/** 获取会话消息记录 */
export const getSessionMessagesApi = (sessionId: string) =>
  get(`/forge-report-api/ai/session/${sessionId}/messages`) as unknown as Promise<ApiResponse<AiChatRecord[]>>

/** 删除会话 */
export const deleteSessionApi = (sessionId: string) =>
  del(`/forge-report-api/ai/session/${sessionId}`) as unknown as Promise<ApiResponse>

const createNonce = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `nonce-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const consumeAiSse = async (
  url: string,
  data: Record<string, any>,
  onChunk: (chunk: string) => void,
  onDone: (fullText: string) => void,
  onError: (error: Error) => void,
  signal?: AbortSignal,
  requestLabel: string = 'AI 请求失败'
) => {
  const token = getLocalStorage(StorageEnum.GO_ACCESS_TOKEN_STORE)
  let response: Response

  try {
    response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept-Language': requestLanguage(),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': createNonce()
      },
      body: JSON.stringify(data),
      signal
    })
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      onDone('')
      return
    }
    onError(err instanceof Error ? err : new Error(String(err)))
    return
  }

  if (!response.ok) {
    const errorText = await response.text()
    onError(new Error(`${uiText(requestLabel)} (${response.status}): ${errorText}`))
    return
  }

  const reader = response.body?.getReader()
  if (!reader) {
    onError(new Error(uiText("无法获取 AI 对话响应流")))
    return
  }

  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let fullText = ''
  let finished = false

  const flushBlock = (block: string) => {
    const lines = block.split('\n')
    let dataLines: string[] = []
    let blockEventName = 'message'
    for (const rawLine of lines) {
      const line = rawLine.trim()
      if (!line) continue
      if (line.startsWith('event:')) {
        blockEventName = line.slice(6).trim() || 'message'
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }

    const payload = dataLines.join('\n')
    if (!payload) return
    if (blockEventName === 'done' || payload === '[DONE]') {
      finished = true
      onDone(fullText)
      return
    }
    if (blockEventName === 'error') {
      finished = true
      onError(new Error(payload))
      return
    }
    fullText += payload
    onChunk(payload)
  }

  try {
    let reading = true
    while (reading) {
      const { done, value } = await reader.read()
      if (done || finished) {
        reading = false
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() || ''
      blocks.forEach(flushBlock)
    }

    if (!finished && buffer.trim()) {
      flushBlock(buffer)
    }
    if (!finished) {
      onDone(fullText)
    }
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      onDone('')
      return
    }
    onError(err instanceof Error ? err : new Error(String(err)))
  } finally {
    reader.releaseLock()
  }
}

/** AI 对话流式输出 */
export const aiChatStream = async (
  data: AiChatStreamRequest,
  onChunk: (chunk: string) => void,
  onDone: (fullText: string) => void,
  onError: (error: Error) => void,
  signal?: AbortSignal
) => {
  return consumeAiSse('/forge-report-api/ai/chat/stream', data, onChunk, onDone, onError, signal, 'AI 对话请求失败')
}
