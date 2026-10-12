import { L } from '@/locales'
import { resolveStaticUrl } from '@/utils/assets'

const DEFAULT_TOAST = {
  message: '',
  type: 'info',
  duration: 2500,
}

const DEFAULT_NOTIFY = {
  title: '',
  description: '',
  type: 'info',
  duration: 4000,
}

const ICON_MAP = {
  success: '/static/icons/ai-icon/check-circle.svg',
  warning: '/static/icons/ai-icon/alert-triangle.svg',
  error: '/static/icons/ai-icon/x-circle.svg',
  info: '/static/icons/ai-icon/info.svg',
}

function ensureH5Document() {
  return typeof document !== 'undefined' && document.body
}

function getToastContainer() {
  let container = document.querySelector('.forge-toast-container')
  if (!container) {
    container = document.createElement('div')
    container.className = 'forge-toast-container'
    document.body.appendChild(container)
  }
  return container
}

function getNotifyContainer() {
  let container = document.querySelector('.forge-notify-container')
  if (!container) {
    container = document.createElement('div')
    container.className = 'forge-notify-container'
    document.body.appendChild(container)
  }
  return container
}

function createIcon(type, className) {
  const iconUrl = resolveStaticUrl(ICON_MAP[type] || ICON_MAP.info)
  const icon = document.createElement('span')
  icon.className = className
  icon.style.webkitMask = `url(${iconUrl}) center / contain no-repeat`
  icon.style.mask = `url(${iconUrl}) center / contain no-repeat`
  return icon
}

function removeNode(node) {
  node.classList.remove('is-open')
  node.classList.add('is-leaving')
  window.setTimeout(() => node.remove(), 220)
}

function nativeToast(options) {
  const config = { ...DEFAULT_TOAST, ...options }
  uni.showToast({
    title: config.message,
    icon: config.type === 'success' ? 'success' : 'none',
    duration: config.duration,
  })
}

export function toast(message, options = {}) {
  const config = {
    ...DEFAULT_TOAST,
    ...options,
    message,
  }

  if (!ensureH5Document()) {
    nativeToast(config)
    return
  }

  const container = getToastContainer()
  const node = document.createElement('div')
  node.className = `forge-toast forge-toast--${config.type}`

  if (config.type !== 'info') {
    node.appendChild(createIcon(config.type, 'forge-toast__icon'))
  }

  const text = document.createElement('span')
  text.className = 'forge-toast__text'
  text.textContent = config.message
  node.appendChild(text)
  container.appendChild(node)

  requestAnimationFrame(() => node.classList.add('is-open'))
  window.setTimeout(() => removeNode(node), config.duration)
}

export function notify(options = {}) {
  const config = {
    ...DEFAULT_NOTIFY,
    ...options,
  }

  if (!ensureH5Document()) {
    nativeToast({
      message: config.description || config.title,
      type: config.type,
      duration: config.duration,
    })
    return
  }

  const container = getNotifyContainer()
  const node = document.createElement('div')
  node.className = `forge-notify forge-notify--${config.type}`

  const glow = document.createElement('div')
  glow.className = 'forge-notify__glow'

  const iconWrap = document.createElement('div')
  iconWrap.className = 'forge-notify__icon'
  iconWrap.appendChild(createIcon(config.type, 'forge-notify__icon-mask'))

  const body = document.createElement('div')
  body.className = 'forge-notify__body'

  const title = document.createElement('div')
  title.className = 'forge-notify__title'
  title.textContent = config.title

  const desc = document.createElement('div')
  desc.className = 'forge-notify__desc'
  desc.textContent = config.description

  const close = document.createElement('button')
  close.type = 'button'
  close.className = 'forge-notify__close'
  close.setAttribute('aria-label', L("关闭"))
  close.addEventListener('click', () => removeNode(node))

  body.appendChild(title)
  if (config.description) {
    body.appendChild(desc)
  }

  node.appendChild(glow)
  node.appendChild(iconWrap)
  node.appendChild(body)
  node.appendChild(close)
  container.appendChild(node)

  requestAnimationFrame(() => node.classList.add('is-open'))
  window.setTimeout(() => removeNode(node), config.duration)
}

export function showToastMessage(message, options = {}) {
  toast(message, options)
}

export function showNotifyMessage(options = {}) {
  notify(options)
}
