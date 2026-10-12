<template>
  <div class="email-config">
    <n-spin :show="loading">
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="120px"
        class="config-form"
      >
        <!-- 基础配置 -->
        <div class="config-section">
          <div class="section-header">
            <div class="section-title">
              <i class="i-material-symbols:mail-rounded section-icon" />
              {{ translateUiText("基础配置") }}
            </div>
            <div class="section-desc">
              {{ translateUiText("SMTP服务器与邮箱账号配置") }}
            </div>
          </div>
          <div class="section-content">
            <n-form-item :label="translateUiText(&quot;状态&quot;)" path="status">
              <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
                <template #checked>
                  {{ translateUiText("启用") }}
                </template>
                <template #unchecked>
                  {{ translateUiText("禁用") }}
                </template>
              </n-switch>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;配置标识&quot;)" path="configId">
              <n-input v-model:value="formData.configId" :placeholder="translateUiText(&quot;请输入配置标识（如：company-email）&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;SMTP服务器&quot;)" path="smtpServer">
              <n-input v-model:value="formData.smtpServer" :placeholder="translateUiText(&quot;如：smtp.qq.com&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;端口&quot;)" path="port">
              <n-input-number
                v-model:value="formData.port"
                :min="1"
                :max="65535"
                :placeholder="translateUiText(&quot;端口&quot;)"
                style="width: 200px"
              />
              <span class="ml-8 text-gray-500">{{ translateUiText("常用：25(明文)、465(SSL)、587(TLS)") }}</span>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;用户名&quot;)" path="username">
              <n-input v-model:value="formData.username" :placeholder="translateUiText(&quot;邮箱账号&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;密码/授权码&quot;)" path="password">
              <n-input
                v-model:value="formData.password"
                type="password"
                show-password-on="click"
                :placeholder="translateUiText(&quot;QQ/网易等邮箱请使用授权码&quot;)"
              />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;发件人地址&quot;)" path="fromAddress">
              <n-input v-model:value="formData.fromAddress" :placeholder="translateUiText(&quot;发件人邮箱地址&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;发件人名称&quot;)" path="fromName">
              <n-input v-model:value="formData.fromName" :placeholder="translateUiText(&quot;发件人显示名称（可选）&quot;)" />
            </n-form-item>
          </div>
        </div>

        <!-- 安全设置 -->
        <div class="config-section">
          <div class="section-header">
            <div class="section-title">
              <i class="i-material-symbols:shield-outline section-icon" />
              {{ translateUiText("安全设置") }}
            </div>
            <div class="section-desc">
              {{ translateUiText("SSL加密与身份验证配置") }}
            </div>
          </div>
          <div class="section-content">
            <n-form-item :label="translateUiText(&quot;开启SSL&quot;)" path="isSsl">
              <n-switch v-model:value="formData.isSsl">
                <template #checked>
                  {{ translateUiText("是") }}
                </template>
                <template #unchecked>
                  {{ translateUiText("否") }}
                </template>
              </n-switch>
              <span class="ml-8 text-gray-500">{{ translateUiText("QQ、网易等邮箱通常需要开启SSL") }}</span>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;开启验证&quot;)" path="isAuth">
              <n-switch v-model:value="formData.isAuth">
                <template #checked>
                  {{ translateUiText("是") }}
                </template>
                <template #unchecked>
                  {{ translateUiText("否") }}
                </template>
              </n-switch>
            </n-form-item>
          </div>
        </div>

        <!-- 重试配置 -->
        <div class="config-section">
          <div class="section-header">
            <div class="section-title">
              <i class="i-material-symbols:refresh section-icon" />
              {{ translateUiText("重试配置") }}
            </div>
            <div class="section-desc">
              {{ translateUiText("发送失败后的重试策略") }}
            </div>
          </div>
          <div class="section-content">
            <n-form-item :label="translateUiText(&quot;重试间隔&quot;)" path="retryInterval">
              <n-input-number
                v-model:value="formData.retryInterval"
                :min="1"
                :placeholder="translateUiText(&quot;重试间隔&quot;)"
                style="width: 200px"
              />
              <span class="ml-8 text-gray-500">{{ translateUiText("秒") }}</span>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;最大重试次数&quot;)" path="maxRetries">
              <n-input-number
                v-model:value="formData.maxRetries"
                :min="0"
                :placeholder="translateUiText(&quot;最大重试次数&quot;)"
                style="width: 200px"
              />
              <span class="ml-8 text-gray-500">{{ translateUiText("次（0表示不重试）") }}</span>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;备注&quot;)" path="remark">
              <n-input
                v-model:value="formData.remark"
                type="textarea"
                :placeholder="translateUiText(&quot;请输入备注&quot;)"
                :rows="3"
              />
            </n-form-item>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="form-footer">
          <n-button type="primary" :loading="saving" @click="handleSave">
            <i class="i-material-symbols:save mr-2" />
            {{ translateUiText("保存配置") }}
          </n-button>
          <n-button @click="handleReset">
            <i class="i-material-symbols:refresh mr-2" />
            {{ translateUiText("重置") }}
          </n-button>
        </div>
      </n-form>
    </n-spin>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import api from '../api.js'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)

const formData = ref({
  id: null,
  configId: null,
  smtpServer: null,
  port: 465,
  username: null,
  password: null,
  fromAddress: null,
  fromName: null,
  isSsl: true,
  isAuth: true,
  retryInterval: 5,
  maxRetries: 1,
  status: 0,
  remark: null,
})

const rules = {
  smtpServer: { required: true, message: '请输入 SMTP 服务器地址', trigger: 'blur' },
  port: { required: true, type: 'number', message: '请输入端口', trigger: 'blur' },
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码/授权码', trigger: 'blur' },
  fromAddress: { required: true, message: '请输入发件人地址', trigger: 'blur' },
}

async function loadConfig() {
  loading.value = true
  try {
    const res = await api.getEmailConfig()
    if (res.code === 200 && res.data) {
      Object.assign(formData.value, res.data)
    }
  }
  catch (error) {
    console.error('获取邮件配置失败:', error)
  }
  finally {
    loading.value = false
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saving.value = true
    const res = await api.saveEmailConfig(formData.value)
    if (res.code === 200) {
      window.$message.success(translateUiText("保存成功"))
      loadConfig()
    }
    else {
      window.$message.error(res.msg || '保存失败')
    }
  }
  catch (error) {
    console.error('保存邮件配置失败:', error)
  }
  finally {
    saving.value = false
  }
}

function handleReset() {
  loadConfig()
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.email-config {
  max-width: 800px;
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 配置分组卡片 */
.config-section {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.section-icon {
  font-size: 18px;
  color: #3b82f6;
}

.section-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.section-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-content :deep(.n-form-item) {
  margin-bottom: 0;
}

.section-content :deep(.n-form-item-label) {
  font-weight: 500;
}

/* 表单底部 */
.form-footer {
  display: flex;
  justify-content: end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

/* 深色模式 */
.dark .config-section {
  background: #1e293b;
  border-color: #334155;
}

.dark .section-title {
  color: #f1f5f9;
}

.dark .section-icon {
  color: #60a5fa;
}

.dark .section-desc {
  color: #94a3b8;
}

.dark .form-footer {
  border-top-color: #334155;
}
</style>
