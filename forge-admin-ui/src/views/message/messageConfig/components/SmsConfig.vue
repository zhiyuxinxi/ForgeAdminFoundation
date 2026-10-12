<template>
  <div class="sms-config">
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
              <i class="i-material-symbols:sms-rounded section-icon" />
              {{ translateUiText("基础配置") }}
            </div>
            <div class="section-desc">
              {{ translateUiText("短信服务厂商与账号配置") }}
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

            <n-form-item :label="translateUiText(&quot;短信厂商&quot;)" path="supplier">
              <n-select
                v-model:value="formData.supplier"
                :options="supplierOptions"
                :placeholder="translateUiText(&quot;请选择短信厂商&quot;)"
                @update:value="handleSupplierChange"
              />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;配置标识&quot;)" path="configId">
              <n-input v-model:value="formData.configId" :placeholder="translateUiText(&quot;请输入配置标识（如：aliyun-sms）&quot;)" />
            </n-form-item>

            <n-form-item label="AccessKey ID" path="accessKeyId">
              <n-input v-model:value="formData.accessKeyId" :placeholder="translateUiText(&quot;请输入 AccessKey ID&quot;)" />
            </n-form-item>

            <n-form-item label="AccessKey Secret" path="accessKeySecret">
              <n-input
                v-model:value="formData.accessKeySecret"
                type="password"
                show-password-on="click"
                :placeholder="translateUiText(&quot;请输入 AccessKey Secret&quot;)"
              />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;短信签名&quot;)" path="signature">
              <n-input v-model:value="formData.signature" :placeholder="translateUiText(&quot;请输入短信签名&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;默认模板ID&quot;)" path="templateId">
              <n-input v-model:value="formData.templateId" :placeholder="translateUiText(&quot;请输入默认模板ID&quot;)" />
            </n-form-item>
          </div>
        </div>

        <!-- 阿里云自定义配置 -->
        <div v-if="formData.supplier === 'alibaba'" class="config-section">
          <div class="section-header">
            <div class="section-title">
              <i class="i-material-symbols:cloud-outline section-icon" />
              {{ translateUiText("阿里云自定义配置") }}
            </div>
          </div>
          <div class="section-content">
            <n-form-item :label="translateUiText(&quot;模板变量名称&quot;)">
              <n-input v-model:value="extraConfig.alibaba.templateName" :placeholder="translateUiText(&quot;模板变量名称（可选）&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;请求地址&quot;)">
              <n-input v-model:value="extraConfig.alibaba.requestUrl" :placeholder="translateUiText(&quot;默认: dysmsapi.aliyuncs.com&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;接口名称&quot;)">
              <n-input v-model:value="extraConfig.alibaba.action" :placeholder="translateUiText(&quot;默认: SendSms&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;接口版本号&quot;)">
              <n-input v-model:value="extraConfig.alibaba.version" :placeholder="translateUiText(&quot;默认: 2017-05-25&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;地域信息&quot;)">
              <n-input v-model:value="extraConfig.alibaba.regionId" :placeholder="translateUiText(&quot;默认: cn-hangzhou&quot;)" />
            </n-form-item>
          </div>
        </div>

        <!-- 腾讯云自定义配置 -->
        <div v-if="formData.supplier === 'tencent'" class="config-section">
          <div class="section-header">
            <div class="section-title">
              <i class="i-material-symbols:cloud-outline section-icon" />
              {{ translateUiText("腾讯云自定义配置") }}
            </div>
          </div>
          <div class="section-content">
            <n-form-item label="SDK AppId">
              <n-input v-model:value="extraConfig.tencent.sdkAppId" :placeholder="translateUiText(&quot;请输入 SDK AppId&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;地域信息&quot;)">
              <n-input v-model:value="extraConfig.tencent.territory" :placeholder="translateUiText(&quot;默认: ap-guangzhou&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;验证码短信请求地址&quot;)">
              <n-input v-model:value="extraConfig.tencent.codeUrl" :placeholder="translateUiText(&quot;默认: https://api.netease.im/sms/sendcode.action&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;请求超时时间&quot;)">
              <n-input-number v-model:value="extraConfig.tencent.connTimeout" :min="1" :placeholder="translateUiText(&quot;默认: 60&quot;)" style="width: 200px" />
              <span class="ml-8 text-gray-500">{{ translateUiText("秒") }}</span>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;请求地址&quot;)">
              <n-input v-model:value="extraConfig.tencent.requestUrl" :placeholder="translateUiText(&quot;默认: sms.tencentcloudapi.com&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;接口名称&quot;)">
              <n-input v-model:value="extraConfig.tencent.action" :placeholder="translateUiText(&quot;默认: SendSms&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;接口版本&quot;)">
              <n-input v-model:value="extraConfig.tencent.version" :placeholder="translateUiText(&quot;默认: 2021-01-11&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;服务名&quot;)">
              <n-input v-model:value="extraConfig.tencent.service" :placeholder="translateUiText(&quot;默认: sms&quot;)" />
            </n-form-item>
          </div>
        </div>

        <!-- 中国移动云MAS自定义配置 -->
        <div v-if="formData.supplier === 'mas'" class="config-section">
          <div class="section-header">
            <div class="section-title">
              <i class="i-material-symbols:cloud-outline section-icon" />
              {{ translateUiText("中国移动云MAS自定义配置") }}
            </div>
          </div>
          <div class="section-content">
            <n-alert type="info" class="mb-16">
              <template #header>
                {{ translateUiText("字段说明") }}
              </template>
              <ul class="text-12">
                <li>{{ translateUiText("SDK AppId = 接口账号用户名") }}</li>
                <li>{{ translateUiText("AccessKey Secret = 用户密码") }}</li>
                <li>{{ translateUiText("企业名称 = 企业名称") }}</li>
                <li>{{ translateUiText("签名编码 = 签名编码") }}</li>
              </ul>
            </n-alert>

            <n-form-item :label="translateUiText(&quot;企业名称&quot;)">
              <n-input v-model:value="extraConfig.mas.ecName" :placeholder="translateUiText(&quot;请输入企业名称&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;签名编码&quot;)">
              <n-input v-model:value="extraConfig.mas.signature" :placeholder="translateUiText(&quot;请输入签名编码&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;模板ID&quot;)">
              <n-input v-model:value="extraConfig.mas.templateId" :placeholder="translateUiText(&quot;有模板接口时需要配置&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;扩展码&quot;)">
              <n-input v-model:value="extraConfig.mas.addSerial" :placeholder="translateUiText(&quot;可为空，总长度不能超过20位&quot;)" />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;请求方法&quot;)">
              <n-select
                v-model:value="extraConfig.mas.action"
                :options="translateUiMetadata(masActionOptions)"
                :placeholder="translateUiText(&quot;选择请求方法&quot;)"
              />
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;请求地址&quot;)">
              <n-input v-model:value="extraConfig.mas.requestUrl" :placeholder="translateUiText(&quot;HTTP模式默认: http://112.35.1.155:1992/sms/&quot;)" />
            </n-form-item>
          </div>
        </div>

        <!-- 发送限制 -->
        <div class="config-section">
          <div class="section-header">
            <div class="section-title">
              <i class="i-material-symbols:counter-1 section-icon" />
              {{ translateUiText("发送限制") }}
            </div>
            <div class="section-desc">
              {{ translateUiText("控制短信发送频率与数量上限") }}
            </div>
          </div>
          <div class="section-content">
            <n-form-item :label="translateUiText(&quot;每日上限&quot;)" path="dailyLimit">
              <n-input-number
                v-model:value="formData.dailyLimit"
                :min="0"
                :placeholder="translateUiText(&quot;每日发送上限&quot;)"
                style="width: 200px"
              />
              <span class="ml-8 text-gray-500">{{ translateUiText("条/天（0表示不限制）") }}</span>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;每分钟上限&quot;)" path="minuteLimit">
              <n-input-number
                v-model:value="formData.minuteLimit"
                :min="0"
                :placeholder="translateUiText(&quot;每分钟发送上限&quot;)"
                style="width: 200px"
              />
              <span class="ml-8 text-gray-500">{{ translateUiText("条/分钟（0表示不限制）") }}</span>
            </n-form-item>

            <n-form-item :label="translateUiText(&quot;发送上限&quot;)" path="maximum">
              <n-input-number
                v-model:value="formData.maximum"
                :min="0"
                :placeholder="translateUiText(&quot;厂商发送上限&quot;)"
                style="width: 200px"
              />
              <span class="ml-8 text-gray-500">{{ translateUiText("条（0表示不限制）") }}</span>
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

            <n-form-item :label="translateUiText(&quot;权重&quot;)" path="weight">
              <n-input-number
                v-model:value="formData.weight"
                :min="1"
                :placeholder="translateUiText(&quot;权重&quot;)"
                style="width: 200px"
              />
              <span class="ml-8 text-gray-500">{{ translateUiText("负载均衡权重值") }}</span>
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
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

import api from '../api.js'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)

const formData = ref({
  id: null,
  configId: null,
  supplier: null,
  accessKeyId: null,
  accessKeySecret: null,
  signature: null,
  templateId: null,
  weight: 1,
  retryInterval: 5,
  maxRetries: 0,
  maximum: null,
  dailyLimit: null,
  minuteLimit: null,
  status: 0,
  remark: null,
  extraConfig: null,
})

const extraConfig = ref({
  alibaba: {
    templateName: null,
    requestUrl: 'dysmsapi.aliyuncs.com',
    action: 'SendSms',
    version: '2017-05-25',
    regionId: 'cn-hangzhou',
  },
  tencent: {
    sdkAppId: null,
    territory: 'ap-guangzhou',
    codeUrl: 'https://api.netease.im/sms/sendcode.action',
    connTimeout: 60,
    requestUrl: 'sms.tencentcloudapi.com',
    action: 'SendSms',
    version: '2021-01-11',
    service: 'sms',
  },
  mas: {
    ecName: null,
    signature: null,
    templateId: null,
    addSerial: null,
    action: 'tmpsubmit',
    requestUrl: 'http://112.35.1.155:1992/sms/',
  },
})

const rules = {
  supplier: { required: true, message: '请选择短信厂商', trigger: 'change' },
  configId: { required: true, message: '请输入配置标识', trigger: 'blur' },
  accessKeyId: { required: true, message: '请输入 AccessKey ID', trigger: 'blur' },
  accessKeySecret: { required: true, message: '请输入 AccessKey Secret', trigger: 'blur' },
}

const supplierOptions = ref([])

const masActionOptions = [
  { label: 'HTTP-无模板接口(norsubmit)', value: 'norsubmit' },
  { label: 'HTTP-有模板接口(tmpsubmit)', value: 'tmpsubmit' },
  { label: 'HTTPS-有模板接口(submit)', value: 'submit' },
]

async function loadSuppliers() {
  try {
    const res = await api.getSmsSuppliers()
    if (res.code === 200 && res.data) {
      supplierOptions.value = Object.entries(res.data).map(([value, label]) => ({
        label,
        value,
      }))
    }
  }
  catch (error) {
    console.error('获取短信厂商列表失败:', error)
  }
}

async function loadConfig() {
  loading.value = true
  try {
    const res = await api.getSmsConfig()
    if (res.code === 200 && res.data) {
      Object.assign(formData.value, res.data)

      if (res.data.extraConfig) {
        try {
          const extra = JSON.parse(res.data.extraConfig)
          if (extra.alibaba) {
            Object.assign(extraConfig.value.alibaba, extra.alibaba)
          }
          if (extra.tencent) {
            Object.assign(extraConfig.value.tencent, extra.tencent)
          }
          if (extra.mas) {
            Object.assign(extraConfig.value.mas, extra.mas)
          }
        }
        catch (e) {
          console.error('解析额外配置失败:', e)
        }
      }
    }
  }
  catch (error) {
    console.error('获取短信配置失败:', error)
  }
  finally {
    loading.value = false
  }
}

function handleSupplierChange() {
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saving.value = true

    const submitData = { ...formData.value }

    const currentSupplier = formData.value.supplier
    if (currentSupplier && extraConfig.value[currentSupplier]) {
      const extra = {}
      extra[currentSupplier] = extraConfig.value[currentSupplier]
      submitData.extraConfig = JSON.stringify(extra)
    }

    const res = await api.saveSmsConfig(submitData)
    if (res.code === 200) {
      window.$message.success(translateUiText("保存成功"))
      loadConfig()
    }
    else {
      window.$message.error(res.msg || '保存失败')
    }
  }
  catch (error) {
    console.error('保存短信配置失败:', error)
  }
  finally {
    saving.value = false
  }
}

function handleReset() {
  loadConfig()
}

onMounted(() => {
  loadSuppliers()
  loadConfig()
})
</script>

<style scoped>
.sms-config {
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
  color: #10b981;
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
  color: #34d399;
}

.dark .section-desc {
  color: #94a3b8;
}

.dark .form-footer {
  border-top-color: #334155;
}
</style>
