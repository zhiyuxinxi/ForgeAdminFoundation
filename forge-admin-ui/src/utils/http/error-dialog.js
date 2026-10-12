import { NButton, NCollapseTransition, NScrollbar, NSpace, NTag } from 'naive-ui'
import { computed, defineComponent, h, ref } from 'vue'
import { localizeLabel, t } from '@/locales'

let activeErrorDialog = null

const RequestErrorDialogContent = defineComponent({
  name: 'RequestErrorDialogContent',
  props: {
    summary: {
      type: String,
      default: '',
    },
    code: {
      type: [String, Number],
      default: '',
    },
    method: {
      type: String,
      default: '',
    },
    url: {
      type: String,
      default: '',
    },
    traceId: {
      type: String,
      default: '',
    },
    detailText: {
      type: String,
      default: '',
    },
  },
  setup(props) {
    const detailVisible = ref(false)
    const hasDetail = computed(() => Boolean(props.detailText))
    const codeTagType = computed(() => {
      const numericCode = Number(props.code)
      if (!Number.isNaN(numericCode) && numericCode >= 500)
        return 'error'
      if (props.code === 'NETWORK_ERROR')
        return 'error'
      return 'warning'
    })
    const metaTags = computed(() => {
      const tags = []
      if (props.code !== '' && props.code !== null && props.code !== undefined) {
        tags.push({
          key: 'code',
          text: t('common.errorCode', { code: props.code }),
          type: codeTagType.value,
        })
      }
      if (props.traceId) {
        tags.push({
          key: 'traceId',
          text: `TraceId ${props.traceId}`,
          type: 'default',
        })
      }
      return tags
    })

    return () => h('div', { style: 'display:flex;flex-direction:column;gap:12px;' }, [
      h('div', {
        style: 'padding:12px 14px;border-radius:10px;background:rgba(244,63,94,0.04);border:1px solid rgba(244,63,94,0.14);',
      }, [
        h('div', {
          style: 'font-size:12px;line-height:1.5;color:var(--n-text-color-3);',
        }, localizeLabel('错误内容')),
        h('div', {
          style: 'margin-top:4px;font-size:18px;font-weight:600;line-height:1.6;color:var(--n-text-color);word-break:break-word;',
        }, props.summary || localizeLabel('请求失败')),
      ]),
      metaTags.value.length
        ? h(NSpace, {
            size: [8, 8],
            wrap: true,
          }, {
            default: () => metaTags.value.map(item => h(NTag, {
              key: item.key,
              type: item.type,
              size: 'small',
              round: false,
            }, {
              default: () => item.text,
            })),
          })
        : null,
      hasDetail.value
        ? h('div', {
            style: 'display:flex;justify-content:flex-start;',
          }, [
            h(NButton, {
              text: true,
              size: 'small',
              type: 'primary',
              onClick: () => {
                detailVisible.value = !detailVisible.value
              },
            }, {
              default: () => detailVisible.value ? localizeLabel('收起详情') : localizeLabel('查看详情'),
            }),
          ])
        : null,
      h(NCollapseTransition, { show: detailVisible.value && hasDetail.value }, {
        default: () => h('div', {
          style: 'padding:10px 12px;border-radius:10px;background:rgba(15,23,42,0.03);border:1px solid rgba(148,163,184,0.18);',
        }, [
          h('div', {
            style: 'margin-bottom:8px;font-size:12px;font-weight:500;line-height:1.5;color:var(--n-text-color-2);',
          }, localizeLabel('诊断详情')),
          props.method || props.url
            ? h('div', {
                style: 'margin-bottom:8px;font-size:12px;line-height:1.6;color:var(--n-text-color-3);word-break:break-all;',
              }, `${props.method || ''}${props.method && props.url ? ' ' : ''}${props.url || ''}`)
            : null,
          h(NScrollbar, { style: 'max-height:320px;' }, {
            default: () => h('pre', {
              style: 'margin:0;white-space:pre-wrap;word-break:break-all;font-size:12px;line-height:1.7;color:var(--n-text-color);font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,Liberation Mono,Courier New,monospace;',
            }, props.detailText),
          }),
        ]),
      }),
    ])
  },
})

function normalizeDetailText(detail) {
  if (!detail) {
    return ''
  }
  if (typeof detail === 'string') {
    return detail.trim()
  }

  const lines = []
  if (detail.rawMessage && detail.rawMessage !== detail.message) {
    lines.push(`原始信息: ${detail.rawMessage}`)
  }
  if (detail.method || detail.url) {
    lines.push(`请求: ${(detail.method || '').toUpperCase()} ${detail.url || ''}`.trim())
  }
  if (detail.traceId) {
    lines.push(`TraceId: ${detail.traceId}`)
  }
  if (detail.status !== undefined && detail.status !== null && detail.status !== '') {
    lines.push(`HTTP状态: ${detail.status}`)
  }
  if (detail.responseData !== undefined) {
    lines.push(localizeLabel('服务端响应:'))
    lines.push(stringifyDetail(detail.responseData))
  }
  if (detail.error !== undefined) {
    lines.push(localizeLabel('异常对象:'))
    lines.push(stringifyDetail(detail.error))
  }
  return lines.filter(Boolean).join('\n')
}

function stringifyDetail(value) {
  if (value === null || value === undefined) {
    return String(value)
  }
  if (typeof value === 'string') {
    return value.trim()
  }
  if (value instanceof Error) {
    return value.stack || value.message || String(value)
  }
  try {
    const cache = new WeakSet()
    return JSON.stringify(value, (key, currentValue) => {
      if (currentValue instanceof Error) {
        return {
          name: currentValue.name,
          message: currentValue.message,
          stack: currentValue.stack,
        }
      }
      if (typeof currentValue === 'object' && currentValue !== null) {
        if (cache.has(currentValue)) {
          return '[Circular]'
        }
        cache.add(currentValue)
      }
      return currentValue
    }, 2)
  }
  catch {
    return String(value)
  }
}

export function showRequestErrorDialog(options = {}) {
  const {
    title = localizeLabel('请求失败'),
    message = localizeLabel('系统异常，请联系管理员'),
    code = '',
    method = '',
    url = '',
    traceId = '',
    detail,
  } = options

  const detailText = normalizeDetailText(detail)

  activeErrorDialog?.destroy?.()
  const dialog = window.$dialog?.error?.({
    title: localizeLabel(title),
    positiveText: localizeLabel('知道了'),
    closable: true,
    maskClosable: true,
    style: 'width:min(560px,calc(100vw - 24px));',
    content: () => h(RequestErrorDialogContent, {
      summary: message,
      code,
      method,
      url,
      traceId,
      detailText,
    }),
    onAfterLeave: () => {
      if (activeErrorDialog === dialog) {
        activeErrorDialog = null
      }
    },
  })
  activeErrorDialog = dialog
}
