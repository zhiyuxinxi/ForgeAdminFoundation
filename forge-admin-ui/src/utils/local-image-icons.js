const LOCAL_IMAGE_ICON_PREFIX = 'local-image:'

const imageIconModules = import.meta.glob('../assets/icons/image-icons/**/*.{png,jpg,jpeg,webp,gif,avif}', {
  eager: true,
  import: 'default',
  query: '?url',
})

function normalizeIconName(path) {
  return path.replace('../assets/icons/image-icons/', '')
}

function formatImageIconName(name) {
  const filename = name.split('/').pop() || name
  return filename.replace(/\.[^.]+$/, '')
}

export const localImageIconPrefix = LOCAL_IMAGE_ICON_PREFIX

export const localImageIcons = Object.entries(imageIconModules)
  .map(([path, url]) => {
    const name = normalizeIconName(path)
    return {
      name,
      value: `${LOCAL_IMAGE_ICON_PREFIX}${name}`,
      url,
      displayName: formatImageIconName(name),
    }
  })
  .sort((a, b) => a.name.localeCompare(b.name))

const localImageIconMap = new Map(localImageIcons.map(icon => [icon.value, icon]))

export function isLocalImageIcon(value) {
  return typeof value === 'string' && value.trim().startsWith(LOCAL_IMAGE_ICON_PREFIX)
}

export function resolveLocalImageIconUrl(value) {
  if (!isLocalImageIcon(value))
    return ''
  return localImageIconMap.get(value.trim())?.url || ''
}

export function getLocalImageIconName(value) {
  if (!isLocalImageIcon(value))
    return ''
  return value.trim().replace(LOCAL_IMAGE_ICON_PREFIX, '')
}
