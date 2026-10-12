import { createStorage } from './storage'

const tenant = import.meta.env.VITE_TENANT || 'default'
const prefixKey = `app_${tenant}_`

export function createLocalStorage(option = {}) {
  return createStorage({
    prefixKey: option.prefixKey || '',
    storage: localStorage,
  })
}

export function createSessionStorage(option = {}) {
  return createStorage({
    prefixKey: option.prefixKey || '',
    storage: sessionStorage,
  })
}

export const lStorage = createLocalStorage({ prefixKey })
export const sStorage = createSessionStorage({ prefixKey })
