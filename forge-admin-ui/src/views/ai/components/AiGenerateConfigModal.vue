<template>
  <n-modal
    v-model:show="showModal"
    preset="card"
    :title="translateUiText(&quot;AI 生成页面配置&quot;)"
    style="width: 700px"
    :mask-closable="false"
  >
    <!-- 说明提示 -->
    <n-alert type="info" style="margin-bottom: 16px;">
      <template #header>
        {{ translateUiText("快速生成 vs 对话式生成") }}
      </template>
      <div style="font-size: 13px; line-height: 1.6;">
        <p><strong>{{ translateUiText("快速生成（当前）") }}</strong>{{ translateUiText("：适合简单场景，输入描述或表名后一键生成配置，生成后可直接使用。") }}</p>
        <p>
          <strong>{{ translateUiText("对话式生成") }}</strong>{{ translateUiText("：适合复杂场景，支持多轮对话迭代优化配置，实时预览和编辑。") }}
          <n-button text type="primary" size="small" @click="goToGenerator">
            {{ translateUiText("前往对话式生成 →") }}
          </n-button>
        </p>
      </div>
    </n-alert>

    <n-spin :show="generating">
      <n-form :model="formData" label-placement="left" label-width="100">
        <n-form-item :label="translateUiText(&quot;生成方式&quot;)" path="mode">
          <n-radio-group v-model:value="formData.mode">
            <n-radio value="description">
              {{ translateUiText("自然语言描述") }}
            </n-radio>
            <n-radio value="table">
              {{ translateUiText("已有数据表") }}
            </n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item label="configKey" path="configKey">
          <n-input v-model:value="formData.configKey" :placeholder="translateUiText(&quot;小写字母开头，仅含小写字母+数字+下划线&quot;)">
            <template #suffix>
              <n-button text size="tiny" type="primary" @click="autoGenerateKey">
                {{ translateUiText("自动生成") }}
              </n-button>
            </template>
          </n-input>
        </n-form-item>

        <n-form-item v-if="formData.mode === 'description'" :label="translateUiText(&quot;功能描述&quot;)" path="description">
          <n-input
            v-model:value="formData.description"
            type="textarea"
            :rows="4"
            :placeholder="translateUiText(&quot;请描述需要生成的管理页面功能，如：员工管理，包含姓名、部门、职位、入职日期等字段&quot;)"
          />
        </n-form-item>

        <n-form-item v-if="formData.mode === 'table'" :label="translateUiText(&quot;数据表名&quot;)" path="tableName">
          <n-input v-model:value="formData.tableName" :placeholder="translateUiText(&quot;请输入数据库表名，如 sys_user&quot;)" />
        </n-form-item>
      </n-form>
    </n-spin>

    <template #action>
      <n-space>
        <n-button @click="handleClose">
          {{ translateUiText("取消") }}
        </n-button>
        <n-button type="primary" :loading="generating" @click="handleGenerate">
          {{ translateUiText("AI 生成") }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { crudConfigAiGenerate, crudConfigAiGenerateFromTable } from '@/api/ai'
import { useDiscreteMessage } from '@/composables/useDiscreteMessage'

const props = defineProps({
  show: { type: Boolean, default: false },
})
const emit = defineEmits(['update:show', 'success'])

const router = useRouter()
const showModal = ref(false)
const generating = ref(false)
const { success, error, warning } = useDiscreteMessage()

const formData = ref({
  mode: 'description',
  configKey: '',
  description: '',
  tableName: '',
})

watch(() => props.show, (val) => {
  showModal.value = val
})

watch(showModal, (val) => {
  emit('update:show', val)
})

// 跳转到对话式生成页面
function goToGenerator() {
  handleClose()
  router.push('/ai/crud-generator')
}

// 自动生成configKey
function autoGenerateKey() {
  let key = ''
  if (formData.value.tableName) {
    key = formData.value.tableName.toLowerCase().replace(/^sys_/, '').replace(/[^a-z0-9]/g, '_')
  }
  else if (formData.value.description) {
    const text = formData.value.description
    const keywordMap = {
      管理: 'manage',
      系统: 'system',
      配置: 'config',
      列表: 'list',
      用户: 'user',
      角色: 'role',
      菜单: 'menu',
      部门: 'department',
      员工: 'employee',
      产品: 'product',
      订单: 'order',
      客户: 'customer',
    }
    const words = []
    for (const [cn, en] of Object.entries(keywordMap)) {
      if (text.includes(cn))
        words.push(en)
    }
    if (words.length > 0)
      key = words.slice(0, 3).join('_')
  }
  if (!key)
    key = `page_${Math.random().toString(36).substr(2, 4)}`
  formData.value.configKey = key.replace(/_+/g, '_').replace(/^_|_$/g, '')
}

async function handleGenerate() {
  if (!formData.value.configKey) {
    warning('请输入 configKey')
    return
  }
  if (!formData.value.configKey.match(/^[a-z][a-z0-9_]{1,63}$/)) {
    warning('configKey格式不正确')
    return
  }

  generating.value = true
  try {
    let res
    if (formData.value.mode === 'description') {
      if (!formData.value.description) {
        warning('请输入功能描述')
        return
      }
      res = await crudConfigAiGenerate({
        description: formData.value.description,
        configKey: formData.value.configKey,
      })
    }
    else {
      if (!formData.value.tableName) {
        warning('请输入数据表名')
        return
      }
      res = await crudConfigAiGenerateFromTable({
        tableName: formData.value.tableName,
        configKey: formData.value.configKey,
      })
    }

    if (res.data?.success) {
      emit('success', res.data)
      handleClose()
      success('AI 生成配置成功')
    }
    else {
      error(res.data?.errorMessage || 'AI 生成失败')
    }
  }
  catch (e) {
    error(e.message || 'AI 生成异常')
  }
  finally {
    generating.value = false
  }
}

function handleClose() {
  showModal.value = false
  formData.value = { mode: 'description', configKey: '', description: '', tableName: '' }
}
</script>
