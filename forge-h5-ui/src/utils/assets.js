export function getPublicPath() {
  const rawPath = import.meta.env.VITE_PUBLIC_PATH || import.meta.env.BASE_URL || '/'
  const value = String(rawPath || '/').trim()
  if (!value || value === '/') {
    return '/'
  }
  if (value === './' || value === '.') {
    return './'
  }
  return `/${value.replace(/^\/+|\/+$/g, '')}/`
}

export function resolveStaticUrl(path) {
  const value = String(path || '').trim()
  if (!value) {
    return ''
  }
  if (isExternalAssetUrl(value)) {
    return value
  }

  const publicPath = getPublicPath()
  const normalizedPath = value.replace(/^\/+/, '')
  const normalizedPublicPath = publicPath.replace(/^\/+|\/+$/g, '')

  if (normalizedPublicPath && normalizedPath.startsWith(`${normalizedPublicPath}/`)) {
    return value.startsWith('/') ? value : `/${value}`
  }
  if (publicPath === './') {
    return `./${normalizedPath}`
  }
  return `${publicPath}${normalizedPath}`.replace(/\/{2,}/g, '/')
}

export function resolveIconUrl(icon) {
  const value = String(icon || '').trim()
  if (!value) {
    return ''
  }
  if (value.includes('/') || /\.(?:svg|png|jpe?g|webp|gif|avif)(?:\?.*)?$/i.test(value)) {
    return resolveStaticUrl(value)
  }
  return resolveStaticUrl(`/static/icons/ai-icon/${value}.svg`)
}

function isExternalAssetUrl(value) {
  return value.startsWith('http://')
    || value.startsWith('https://')
    || value.startsWith('data:')
    || value.startsWith('blob:')
}
