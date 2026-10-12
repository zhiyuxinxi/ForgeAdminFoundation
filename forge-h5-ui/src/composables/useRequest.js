import { L } from '@/locales'
import { computed, ref } from 'vue'
import { request } from '@/utils'
import { toast } from '@/utils/notify'

export function useRequest(service, options = {}) {
  const {
    immediate = false,
    defaultParams,
    manual = !immediate,
    showSuccess = false,
    successMessage = L("操作成功"),
    showError = false,
    errorMessage,
    throwOnError = true,
    transform,
  } = options

  const loading = ref(false)
  const data = ref()
  const error = ref(null)
  const params = ref(defaultParams)

  const finished = computed(() => !loading.value && (data.value !== undefined || error.value))

  async function execute(nextParams = params.value, override = {}) {
    params.value = nextParams
    loading.value = true
    error.value = null

    try {
      const raw = typeof service === 'function'
        ? await service(nextParams, override)
        : await request({ ...service, ...override, data: nextParams ?? service?.data, params: override.params ?? service?.params })
      const result = transform ? transform(raw) : raw
      data.value = result
      if (override.showSuccess ?? showSuccess) {
        toast(override.successMessage || successMessage, { type: 'success' })
      }
      return result
    }
    catch (err) {
      error.value = err
      if (override.showError ?? showError) {
        toast(override.errorMessage || errorMessage || err?.message || L("请求失败"), { type: 'error' })
      }
      if (override.throwOnError ?? throwOnError) {
        throw err
      }
      return undefined
    }
    finally {
      loading.value = false
    }
  }

  function reset() {
    loading.value = false
    data.value = undefined
    error.value = null
  }

  if (!manual) {
    execute(defaultParams).catch(() => {})
  }

  return {
    loading,
    data,
    error,
    params,
    finished,
    execute,
    run: execute,
    reset,
  }
}
