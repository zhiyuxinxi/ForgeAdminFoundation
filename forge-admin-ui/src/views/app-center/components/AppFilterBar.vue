<template>
  <div class="app-filter-bar">
    <n-input
      :value="keyword"
      clearable
      class="filter-search"
      :placeholder="translateUiText(&quot;搜索业务单元、访问入口或发布配置&quot;)"
      @update:value="value => emit('update:keyword', value)"
      @keyup.enter="emit('search')"
    >
      <template #prefix>
        <n-icon><SearchOutline /></n-icon>
      </template>
    </n-input>
    <n-select
      v-if="showSuite"
      :value="suiteCode"
      clearable
      filterable
      class="filter-select"
      :placeholder="translateUiText(&quot;业务域&quot;)"
      :options="suiteOptions"
      @update:value="value => emit('update:suiteCode', value)"
    />
    <DictSelect
      :value="objectType"
      class="filter-select"
      dict-type="ai_business_object_type"
      :placeholder="translateUiText(&quot;对象类型&quot;)"
      @update:value="value => emit('update:objectType', value)"
    />
    <DictSelect
      :value="appType"
      class="filter-select"
      dict-type="ai_business_app_type"
      :placeholder="translateUiText(&quot;入口类型&quot;)"
      @update:value="value => emit('update:appType', value)"
    />
    <n-button secondary @click="emit('refresh')">
      <template #icon>
        <n-icon><RefreshOutline /></n-icon>
      </template>
      {{ translateUiText("刷新") }}
    </n-button>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import { computed } from 'vue'
import DictSelect from '@/components/DictSelect.vue'

const props = defineProps({
  keyword: {
    type: String,
    default: '',
  },
  suiteCode: {
    type: String,
    default: null,
  },
  appType: {
    type: String,
    default: null,
  },
  objectType: {
    type: String,
    default: null,
  },
  suites: {
    type: Array,
    default: () => [],
  },
  showSuite: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits([
  'update:keyword',
  'update:suiteCode',
  'update:appType',
  'update:objectType',
  'search',
  'refresh',
])

const suiteOptions = computed(() => props.suites.map(item => ({
  label: item.suiteName || item.suiteCode,
  value: item.suiteCode,
})))
</script>

<style scoped>
.app-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.filter-search {
  flex: 1 1 260px;
  width: auto;
  max-width: 420px;
}

.filter-select {
  flex: 0 0 180px;
  width: 180px;
}

@media (max-width: 680px) {
  .app-filter-bar {
    align-items: stretch;
  }

  .filter-search,
  .filter-select,
  .app-filter-bar :deep(.n-button) {
    width: 100%;
  }
}
</style>
