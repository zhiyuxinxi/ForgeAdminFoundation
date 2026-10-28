// Business-scoped identity selections use trusted server fields; UI context never grants access.
export function resolveIdentityReference(field = {}, context = {}) {
  const configKey = context.identityReferenceConfigKey
  const apiBase = context.identityReferenceApiBase
  if ((!configKey && !apiBase) || hasExternalSource(field))
    return null
  const operation = ['add', 'edit'].includes(context.modalStatus) ? context.modalStatus : null
  return {
    base: `${apiBase || `/ai/crud/${encodeURIComponent(configKey)}/references`}/${encodeURIComponent(field.field || '')}`,
    operation,
  }
}

function hasExternalSource(node) {
  if (!node || typeof node !== 'object')
    return false
  if (node.referenceSource && String(node.referenceSource).toUpperCase() !== 'SYSTEM')
    return true
  if (Array.isArray(node.options) && node.options.length)
    return true
  const source = node.optionSource
  if (source) {
    const api = typeof source === 'string' ? source : source.api || source.url || ''
    if (api && !api.endsWith('/system/org/tree') && !api.endsWith('/system/user/page'))
      return true
    if (Array.isArray(source.options) || Array.isArray(source.data))
      return true
  }
  return hasExternalSource(node.props) || hasExternalSource(node.basicProps) || hasExternalSource(node.advancedProps)
}

export function referenceRows(response) {
  if (response?.code !== 200 || !Array.isArray(response.data))
    throw new Error(response?.msg || response?.message || 'Reference lookup failed')
  return response.data
}

export async function loadOrganizationBranch(request, reference, parentId) {
  if (!reference?.operation)
    return []
  const rows = []
  let cursor
  do {
    const page = referenceRows(await request.get(`${reference.base}/organizations`, {
      params: { operation: reference.operation, parentId, afterOrgId: cursor, limit: 200 },
    }))
    rows.push(...page)
    if (page.length < 200)
      break
    const next = String(page.at(-1)?.orgId || '')
    if (!next || next === cursor)
      throw new Error('Organization lookup cursor did not advance')
    cursor = next
  } while (true)
  return rows.map(org => ({
    key: String(org.orgId),
    value: String(org.orgId),
    id: String(org.orgId),
    label: org.orgName,
    orgName: org.orgName,
    isLeaf: false,
  }))
}

export async function loadMemberPage(request, reference, { orgId, keyword, afterUserId, pageSize }) {
  if (!reference?.operation || !orgId)
    return { rows: [], hasNext: false }
  const page = referenceRows(await request.get(`${reference.base}/members`, {
    params: { operation: reference.operation, directOrgId: orgId, keyword: keyword || undefined, afterUserId, limit: pageSize + 1 },
  }))
  return {
    rows: page.slice(0, pageSize).map(member => ({
      id: String(member.userId),
      username: member.username,
      name: member.displayName,
      realName: member.displayName,
      userStatus: 1,
    })),
    hasNext: page.length > pageSize,
  }
}

export async function loadPostScope(request, reference) {
  const response = await request.get(`${reference.base}/post-scope`, { params: { operation: reference.operation } })
  if (response?.code !== 200 || typeof response.data?.tenantWideAllowed !== 'boolean')
    throw new Error(response?.msg || response?.message || 'Post scope lookup failed')
  return response.data
}

export async function loadPostPage(request, reference, { orgId, tenantWide = false, keyword, afterPostId, pageSize }) {
  if (!reference?.operation || (!tenantWide && !orgId))
    return { rows: [], hasNext: false }
  const page = referenceRows(await request.get(`${reference.base}/posts`, {
    params: { operation: reference.operation, directOrgId: orgId, tenantWide, keyword: keyword || undefined, afterPostId, limit: pageSize + 1 },
  }))
  return {
    rows: page.slice(0, pageSize).map(post => ({ value: String(post.postId), label: post.postName, postCode: post.postCode, orgId: post.orgId })),
    hasNext: page.length > pageSize,
  }
}
