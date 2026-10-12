export const ensureObject = <T extends Record<string, any>>(target: any, key: string, defaults: T): T => {
  target[key] = {
    ...defaults,
    ...(target[key] && typeof target[key] === 'object' ? target[key] : {})
  }
  return target[key]
}

export const ensureArray = <T>(target: any, key: string, defaults: T[] = []): T[] => {
  target[key] = Array.isArray(target[key]) ? target[key] : [...defaults]
  return target[key]
}
