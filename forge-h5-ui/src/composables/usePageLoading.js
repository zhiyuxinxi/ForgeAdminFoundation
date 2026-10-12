import { computed, ref } from 'vue'

export function usePageLoading(initialStatus = 'idle') {
  const status = ref(initialStatus)
  const error = ref(null)

  const loading = computed(() => status.value === 'loading')
  const refreshing = computed(() => status.value === 'refreshing')
  const success = computed(() => status.value === 'success')
  const empty = computed(() => status.value === 'empty')
  const failed = computed(() => status.value === 'error')

  function setLoading() {
    error.value = null
    status.value = 'loading'
  }

  function setRefreshing() {
    error.value = null
    status.value = 'refreshing'
  }

  function setSuccess() {
    error.value = null
    status.value = 'success'
  }

  function setEmpty() {
    error.value = null
    status.value = 'empty'
  }

  function setError(value) {
    error.value = value
    status.value = 'error'
  }

  function reset() {
    error.value = null
    status.value = 'idle'
  }

  async function run(task, options = {}) {
    const { refreshing: isRefreshing = false, emptyWhen } = options
    if (isRefreshing) {
      setRefreshing()
    }
    else {
      setLoading()
    }

    try {
      const result = await task()
      const shouldEmpty = typeof emptyWhen === 'function'
        ? emptyWhen(result)
        : Array.isArray(result) && result.length === 0
      if (shouldEmpty) {
        setEmpty()
      }
      else {
        setSuccess()
      }
      return result
    }
    catch (err) {
      setError(err)
      throw err
    }
  }

  return {
    status,
    error,
    loading,
    refreshing,
    success,
    empty,
    failed,
    setLoading,
    setRefreshing,
    setSuccess,
    setEmpty,
    setError,
    reset,
    run,
  }
}
