import { ref, reactive } from 'vue'
import { request } from '@/utils'

/**
 * 分页组合式函数
 * @param {string} restfulApi - 请求的API地址
 * @param {Function} beforeRenderList - 数据渲染前的处理函数
 */
export function usePaging(restfulApi, beforeRenderList) {
  const pageNum = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const dataSource = ref([])
  const queryFormData = reactive({})
  const finished = ref(false)
  const refreshing = ref(false)

  const refresh = () => {
    pageNum.value = 1
    dataSource.value = []
    finished.value = false
    refreshing.value = true
    searchPage(queryFormData)
  }

  const load = () => {
    if (!finished.value) {
      pageNum.value++
      postMethodsList(queryFormData)
    }
  }

  const deepDataCopy = (data) => {
    const obj = {}
    for (const key in data) {
      if (
        typeof data[key] !== 'undefined' &&
        data[key] !== null &&
        data[key] !== '' &&
        !(Array.isArray(data[key]) && data[key].length === 0)
      ) {
        obj[key] = data[key]
      }
    }
    return obj
  }

  const searchPage = (formData) => {
    Object.assign(queryFormData, deepDataCopy(formData))
    pageNum.value = 1
    dataSource.value = []
    finished.value = false
    postMethodsList(queryFormData)
  }

  const postMethodsList = (data) => {
    const method = 'post'
    const formData = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...data
    }

    request({
      url: restfulApi,
      method,
      data: formData
    })
      .then((res) => {
        refreshing.value = false
        let newData

        if (Array.isArray(res)) {
          newData = res
        } else {
          newData = res.data?.list || res.data?.records || []
          total.value = res?.data?.total ? +res.data.total : 0
        }

        if (beforeRenderList) {
          beforeRenderList(newData, (records) => {
            dataSource.value = [...dataSource.value, ...records]
          })
        } else {
          dataSource.value = [...dataSource.value, ...newData]
        }

        if (dataSource.value.length >= total.value && pageNum.value > 1) {
          finished.value = true
        }

        if (newData.length === 0 && pageNum.value === 1) {
          finished.value = true
        }
      })
      .catch(() => {
        refreshing.value = false
        if (pageNum.value > 1) {
          pageNum.value--
        }
      })
  }

  return {
    pageNum,
    pageSize,
    total,
    dataSource,
    queryFormData,
    finished,
    refreshing,
    refresh,
    load,
    searchPage,
    postMethodsList
  }
}
