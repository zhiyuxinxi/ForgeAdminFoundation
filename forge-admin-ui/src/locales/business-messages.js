// Feature messages live with their pages, inside the frontend phase's writable scope.
// They are always mounted below business.*, so a feature cannot replace core messages.
export function collectBusinessMessages(modules) {
  const messages = { 'zh-CN': {}, 'en-US': {} }
  for (const [path, source] of Object.entries(modules)) {
    const language = path.match(/\/(zh-CN|en-US)\.js$/)?.[1]
    if (language)
      mergeMessages(messages[language], source, path)
  }
  return messages
}

function mergeMessages(target, source, owner) {
  for (const [key, value] of Object.entries(source)) {
    if (typeof value === 'string') {
      if (Object.hasOwn(target, key))
        throw new Error(`Duplicate business message: ${owner}: ${key}`)
      target[key] = value
    }
    else if (value && typeof value === 'object' && !Array.isArray(value)) {
      if (Object.hasOwn(target, key) && typeof target[key] !== 'object')
        throw new Error(`Conflicting business message namespace: ${owner}: ${key}`)
      target[key] ??= {}
      mergeMessages(target[key], value, owner)
    }
  }
}
