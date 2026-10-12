
import { withLocaleHeaders } from '@/locales'
import { finishGlobalLoading, startGlobalLoading } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store/modules/auth'
import { generateUUID } from '@/utils/common'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''

export function streamFlowGenerate(data, onChunk, onComplete, onError, options = {}) {
  const { maxRetries = 1, retryDelay = 1000 } = options
  const controller = new AbortController()
  const authStore = useAuthStore()
  const loadingToken = startGlobalLoading({
    globalLoadingType: 'submit',
    globalLoadingText: '流程生成中，请稍候...',
  })
  let currentRetry = 0
  let isAborted = false
  let loadingFinished = false

  function finishStreamLoading() {
    if (loadingFinished)
      return

    loadingFinished = true
    finishGlobalLoading(loadingToken)
  }

  controller.signal.addEventListener('abort', () => {
    isAborted = true
    finishStreamLoading()
  })

  function doFetch() {
    fetch(`${BASE_URL}/api/flow/ai-generator/stream-generate`, {
      method: 'POST',
      headers: withLocaleHeaders({
        'Content-Type': 'application/json',
        'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': generateUUID(),
      }),
      body: JSON.stringify(data),
      signal: controller.signal,
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(response.statusText || `HTTP ${response.status}`)
        }

        currentRetry = 0
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let hasReceivedData = false

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              if (!hasReceivedData) {
                handleRetry('服务器未返回数据')
              }
              else {
                finishStreamLoading()
              }
              return
            }
            hasReceivedData = true
            buffer += decoder.decode(value, { stream: true })

            const events = buffer.split('\n\n')
            buffer = events.pop() || ''

            for (const eventStr of events) {
              if (!eventStr.trim())
                continue

              const lines = eventStr.split('\n')
              let eventType = 'message'
              let eventData = ''

              for (const line of lines) {
                if (line.startsWith('event:')) {
                  eventType = line.substring(6).trim()
                }
                else if (line.startsWith('data:')) {
                  eventData = line.substring(5).trim()
                }
              }

              if (eventData) {
                try {
                  const parsed = JSON.parse(eventData)
                  if (eventType === 'progress' || eventType === 'chunk') {
                    onChunk({ event: eventType, data: parsed })
                  }
                  else if (eventType === 'complete') {
                    finishStreamLoading()
                    onComplete(parsed)
                  }
                  else if (eventType === 'error') {
                    finishStreamLoading()
                    onError(parsed.message)
                  }
                }
                catch (error) {
                  console.warn('SSE JSON parse error:', error)
                }
              }
            }
            read()
          }).catch((error) => {
            if (error.name !== 'AbortError') {
              handleRetry(error.message)
            }
          })
        }
        read()
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          handleRetry(error.message)
        }
      })
  }

  function handleRetry(errorMessage) {
    if (isAborted)
      return

    if (currentRetry < maxRetries) {
      currentRetry++
      onChunk({
        event: 'progress',
        data: {
          stage: 'retrying',
          message: `连接中断，正在重试 (${currentRetry}/${maxRetries})...`,
        },
      })
      setTimeout(doFetch, retryDelay)
    }
    else {
      finishStreamLoading()
      onError(`连接失败: ${errorMessage}`)
    }
  }

  doFetch()
  return controller
}
