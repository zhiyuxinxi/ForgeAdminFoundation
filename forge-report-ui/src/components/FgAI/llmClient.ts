import { AIGenerateResponse } from '@/api/ai/ai.d'

/**
 * 从 LLM 响应中提取 JSON
 */
export function extractJSON(text: string): string {
  let cleaned = text.trim()
  const jsonStart = cleaned.indexOf('{')
  // 取最后一个 } 或最后一个 ] 的较后者，防止 AI 输出缺根 } 只以 ] 结尾
  const jsonEnd = Math.max(cleaned.lastIndexOf('}'), cleaned.lastIndexOf(']'))
  if (jsonStart >= 0 && jsonEnd > jsonStart) {
    cleaned = cleaned.slice(jsonStart, jsonEnd + 1).trim()
  }
  const codeBlockMatch = cleaned.match(/^```(?:json)?\s*\n?([\s\S]*?)\n?\s*```$/)
  if (codeBlockMatch) {
    cleaned = codeBlockMatch[1].trim()
  }
  return cleaned
}

function normalizeJSONString(jsonStr: string): string {
  let result = jsonStr
    .replace(/^\uFEFF/, '')
    // 移除明显不成形的对象碎片（如 { "0 } 这类 AI 幻觉产物）
    .replace(/\{[^}]*?"[^}]{0,40}\}(?=\s*[,\}\]])/g, (m) => {
      if (/\{[^}]*"[^"]*"\s*:/.test(m)) return m
      return ''
    })
    // 修复引号错乱: "xxx"option":   →   "xxx", "option":
    .replace(/"\s*"option"\s*:/g, '", "option":')
    // 修复尾部多余逗号
    .replace(/,\s*([}\]])/g, '$1')
    // 修复 AI 常见错误：对象中缺少引号的 key
    .replace(/([{,]\s*)(key|x|y|w|h|title|unit|max|type|name|value|region|dimensions|source|header|dataset|option|canvasConfig|components|componentList|editCanvasConfig|styles|attr|chartConfig|request|events|status|preview|filter)(\s*:)/g, '$1"$2"$3')

  // 修复缺失的组件对象开头:  "key": "xxx", → {"key": "xxx",
  result = result.replace(/(\[|,)\s*("key"\s*:)/g, '$1{$2')

  // 平衡括号
  let openBraces = 0, closeBraces = 0, openBrackets = 0, closeBrackets = 0
  let inString = false, escaped = false
  for (let i = 0; i < result.length; i++) {
    const ch = result[i]
    if (escaped) { escaped = false; continue }
    if (ch === '\\') { escaped = true; continue }
    if (ch === '"') { inString = !inString; continue }
    if (inString) continue
    if (ch === '{') openBraces++
    if (ch === '}') closeBraces++
    if (ch === '[') openBrackets++
    if (ch === ']') closeBrackets++
  }
  if (openBraces > closeBraces) result += '}'.repeat(openBraces - closeBraces)
  if (openBrackets > closeBrackets) result += ']'.repeat(openBrackets - closeBrackets)

  return result
}

/**
 * 从流式输出的完整文本中解析 AIGenerateResponse
 */
export function parseStreamedResponse(fullText: string): AIGenerateResponse {
  const jsonStr = normalizeJSONString(extractJSON(fullText))
  let parsed: AIGenerateResponse
  try {
    parsed = JSON.parse(jsonStr)
  } catch (e) {
    try {
      const repaired = repairJSON(jsonStr)
      parsed = JSON.parse(repaired)
    } catch (e2) {
      console.error('[LLM Client] 流式 JSON 解析失败，原始内容:', fullText)
      console.error('[LLM Client] 规范化后内容:', jsonStr.substring(0, 800))
      throw new Error('AI 返回的 JSON 格式无效，请重试')
    }
  }
  if (!parsed.title || !Array.isArray(parsed.components)) {
    throw new Error('AI 返回的数据结构不完整')
  }
  return parsed
}

/**
 * JSON 二次修复——处理仍存在的格式错误
 */
function repairJSON(jsonStr: string): string {
  let result = jsonStr
  result = result.replace(/\}\s*\{/g, '},{')
  result = result.replace(/,\s*([}\]])/g, '$1')
  // 补缺根对象的 }
  if (result.startsWith('{') && !result.endsWith('}')) {
    result += '}'
  }
  return result
}
