function text(value) {
  return String(value ?? '').trim()
}

function formMode(asset = {}) {
  return text(asset.formMode || asset.type || 'BUSINESS_CODE_FORM')
}

function assetIdentity(asset = {}) {
  const source = asset && typeof asset === 'object' ? asset : {}
  return {
    objectCode: text(source.objectCode),
    formKey: text(source.formKey),
    providerKey: text(source.providerKey),
    formMode: formMode(source),
  }
}

function hasSelectableIdentity(asset = {}) {
  const identity = assetIdentity(asset)
  if (!identity.formKey)
    return false
  return identity.formMode !== 'BUSINESS_CODE_FORM' || !!identity.providerKey
}

export function isSameCodeAppAsset(left = {}, right = {}) {
  const leftIdentity = assetIdentity(left)
  const rightIdentity = assetIdentity(right)
  if (!leftIdentity.formKey || leftIdentity.formKey !== rightIdentity.formKey)
    return false
  return ['objectCode', 'providerKey', 'formMode'].every((key) => {
    const leftValue = leftIdentity[key]
    const rightValue = rightIdentity[key]
    return !leftValue || !rightValue || leftValue === rightValue
  })
}

export function findCodeAppAssetMatch(assets = [], reference = {}) {
  if (!hasSelectableIdentity(reference))
    return null
  return (Array.isArray(assets) ? assets : [])
    .find(asset => isSameCodeAppAsset(asset, reference)) || null
}

export function selectCodeAppCatalogAssets(assets = [], catalogAssets = [], objectCode = '') {
  const currentAssets = Array.isArray(assets) ? assets : []
  const catalog = Array.isArray(catalogAssets) ? catalogAssets : []
  if (currentAssets.length) {
    return catalog.filter(catalogAsset => currentAssets.some((asset) => {
      return hasSelectableIdentity(asset) && isSameCodeAppAsset(asset, catalogAsset)
    }))
  }
  const currentObjectCode = text(objectCode)
  if (!currentObjectCode)
    return []
  return catalog.filter(asset => text(asset?.objectCode) === currentObjectCode)
}

export function codeAppAssetKey(asset = {}) {
  const identity = assetIdentity(asset)
  if (!identity.formKey && !identity.providerKey)
    return ''
  return [
    identity.objectCode || '*',
    identity.formMode || '*',
    identity.providerKey || '*',
    identity.formKey || '*',
  ].join('::')
}

export function appendCodeAppAsset(result, seenKeys, asset = {}, configured = null) {
  const keys = [codeAppAssetKey(asset), codeAppAssetKey(configured)].filter(Boolean)
  if (keys.some(key => seenKeys.has(key)))
    return
  keys.forEach(key => seenKeys.add(key))
  result.push(asset)
}

export function mergeCodeAppFieldCapabilities(providerField = {}, configuredField = null) {
  const configured = configuredField && typeof configuredField === 'object' ? configuredField : {}
  const merged = { ...providerField, ...configured }
  const providerDeclaresSearch = typeof providerField.searchable === 'boolean'
  const configuredDeclaresSearch = typeof configured.searchable === 'boolean'
  if (providerDeclaresSearch)
    merged.searchable = providerField.searchable
  else if (configuredDeclaresSearch)
    merged.searchable = configured.searchable
  else
    delete merged.searchable
  return merged
}

export function preserveCodeAppFieldCapabilities(field = {}) {
  const normalized = { ...field }
  if (typeof field.searchable !== 'boolean')
    delete normalized.searchable
  return normalized
}
