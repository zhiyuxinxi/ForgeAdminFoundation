<template>
  <div class="version-timeline">
    <div class="panel-title">
      {{ translateUiText("发布版本") }}
    </div>
    <n-spin :show="loading">
      <n-empty v-if="!visibleVersions.length" :description="translateUiText(&quot;暂无发布版本&quot;)" />
      <n-timeline v-else>
        <n-timeline-item
          v-for="item in visibleVersions"
          :key="item.id"
          :type="item.versionType === 'rollback' ? 'warning' : 'success'"
          :title="`版本 ${item.versionNo}`"
          :content="resolveContent(item)"
          :time="formatTime(item.createTime)"
        >
          <template #footer>
            <n-popconfirm @positive-click="$emit('rollback', item.id)">
              <template #trigger>
                <n-button text size="tiny" class="text-warning">
                  {{ translateUiText("回滚到此版本") }}
                </n-button>
              </template>
              {{ translateUiText("确认回滚到版本") }} {{ item.versionNo }}？
            </n-popconfirm>
          </template>
        </n-timeline-item>
      </n-timeline>
      <div v-if="versions.length > maxVisible" class="version-more">
        {{ translateUiText("仅显示最近") }} {{ maxVisible }} {{ translateUiText("个版本，共") }} {{ versions.length }} {{ translateUiText("个") }}
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { computed } from 'vue'

const props = defineProps({
  versions: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['rollback'])

const maxVisible = 5
const visibleVersions = computed(() => (props.versions || []).slice(0, maxVisible))

function resolveContent(item) {
  const action = item.remark || (item.versionType === 'rollback' ? '回滚发布' : '发布上线')
  const time = formatTime(item.createTime)
  return time ? `${action} · 发布时间 ${time}` : action
}

function formatTime(value) {
  if (!value)
    return ''
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.version-timeline {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.version-more {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  text-align: center;
}
</style>
