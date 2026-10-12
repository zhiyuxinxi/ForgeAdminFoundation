<template>
  <section class="settings-section-card">
    <header>
      <h2>{{ translateUiText("应用权限") }}</h2>
      <p>{{ translateUiText("应用可见范围控制谁能打开应用；页面入口和对象数据范围按角色配置。") }}</p>
    </header>
    <n-alert type="info" :bordered="false" class="settings-info-alert">
      {{ translateUiText("应用管理员自动拥有所有页面权限。保存后需重新发布，正式门户和工作台才使用新的可见范围。") }}
    </n-alert>
    <n-form label-placement="top">
      <n-form-item :label="translateUiText(&quot;可见范围&quot;)">
        <n-radio-group :value="permission.visibility" @update:value="patch({ visibility: $event })">
          <n-radio-button value="all">
            {{ translateUiText("组织全员") }}
          </n-radio-button>
          <n-radio-button value="roles">
            {{ translateUiText("指定角色") }}
          </n-radio-button>
          <n-radio-button value="departments">
            {{ translateUiText("指定部门") }}
          </n-radio-button>
          <n-radio-button value="users">
            {{ translateUiText("指定用户") }}
          </n-radio-button>
        </n-radio-group>
      </n-form-item>
      <n-form-item :label="translateUiText(&quot;应用管理员&quot;)">
        <UserSelectPicker
          :model-value="permission.administrators"
          :label-value="permission.administratorLabels"
          multiple
          :placeholder="translateUiText(&quot;选择应用管理员&quot;)"
          @update:model-value="patch({ administrators: normalizeIds($event) })"
          @update:label-value="patch({ administratorLabels: $event })"
        />
      </n-form-item>
      <n-form-item v-if="permission.visibility === 'roles'" :label="translateUiText(&quot;可见角色&quot;)">
        <n-select
          :value="permission.roleIds"
          :options="roleOptions"
          :loading="roleLoading"
          multiple
          filterable
          :placeholder="translateUiText(&quot;选择可见角色&quot;)"
          @update:value="patch({ roleIds: normalizeIds($event) })"
        />
      </n-form-item>
      <n-form-item v-if="permission.visibility === 'departments'" :label="translateUiText(&quot;可见部门&quot;)">
        <n-tree-select
          :value="permission.departmentIds"
          :options="orgOptions"
          :loading="orgLoading"
          multiple
          filterable
          check-strategy="all"
          key-field="key"
          label-field="label"
          :placeholder="translateUiText(&quot;选择可见部门&quot;)"
          @update:value="patch({ departmentIds: normalizeIds($event) })"
        />
      </n-form-item>
      <n-form-item v-if="permission.visibility === 'users'" :label="translateUiText(&quot;可见用户&quot;)">
        <UserSelectPicker
          :model-value="permission.userIds"
          :label-value="permission.userLabels"
          multiple
          :placeholder="translateUiText(&quot;选择可见用户&quot;)"
          @update:model-value="patch({ userIds: normalizeIds($event) })"
          @update:label-value="patch({ userLabels: $event })"
        />
      </n-form-item>
    </n-form>

    <div class="settings-page-permission">
      <h3>{{ translateUiText("页面与数据权限") }}</h3>
      <ApplicationPermissionsPanel
        v-if="application?.applicationCode"
        :application="application"
      />
    </div>
  </section>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { computed, onMounted, ref } from 'vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { request } from '@/utils/http'
import ApplicationPermissionsPanel from '../../application-workspace/ApplicationPermissionsPanel.vue'

const props = defineProps({
  modelValue: { type: Object, required: true },
  application: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])
const permission = computed(() => props.modelValue.permission || {})
const roleOptions = ref([])
const orgOptions = ref([])
const roleLoading = ref(false)
const orgLoading = ref(false)

onMounted(() => {
  loadRoles()
  loadOrgs()
})

function patch(value) {
  emit('update:modelValue', {
    ...props.modelValue,
    permission: { ...permission.value, ...value },
  })
}

function normalizeIds(values) {
  return [...new Set((Array.isArray(values) ? values : []).map(value => String(value || '').trim()).filter(Boolean))]
}

async function loadRoles() {
  roleLoading.value = true
  try {
    const response = await request.get('/system/role/page', { params: { pageNum: 1, pageSize: 200 } })
    const records = response.data?.records || response.data?.rows || response.data || []
    roleOptions.value = (Array.isArray(records) ? records : []).map(item => ({
      label: `${item.roleName || item.roleKey || item.id}${item.roleKey ? ` · ${item.roleKey}` : ''}`,
      value: String(item.id || item.roleId),
    })).filter(item => item.value)
  }
  catch {
    roleOptions.value = []
  }
  finally {
    roleLoading.value = false
  }
}

async function loadOrgs() {
  orgLoading.value = true
  try {
    const response = await request.get('/system/org/tree')
    orgOptions.value = normalizeOrgTree(response.data || [])
  }
  catch {
    orgOptions.value = []
  }
  finally {
    orgLoading.value = false
  }
}

function normalizeOrgTree(nodes) {
  return (Array.isArray(nodes) ? nodes : []).map(node => ({
    key: String(node.id || node.orgId || ''),
    label: node.orgName || node.label || node.name || String(node.id || ''),
    children: normalizeOrgTree(node.children || []),
  })).filter(node => node.key)
}
</script>

<style scoped>
.settings-page-permission {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--n-border-color, #e5e6eb);
}

.settings-page-permission h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.settings-info-alert {
  margin-bottom: 16px;
}
</style>
