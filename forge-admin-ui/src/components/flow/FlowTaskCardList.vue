<template>
  <section class="flow-task-card-list">
    <div class="task-list-toolbar">
      <div class="task-list-titlebar">
        <n-checkbox
          v-if="selectable"
          :checked="allCurrentPageSelected"
          :indeterminate="partiallySelected"
          @update:checked="toggleCurrentPage"
        />
        <div class="task-list-title">
          {{ localizeLabel(title) }}
        </div>
        <span v-if="selectedKeys.length > 0" class="task-list-selected">
          {{ t('common.selected', { count: selectedKeys.length }) }}
        </span>
        <button v-if="selectedKeys.length > 0" class="task-list-clear" type="button" @click="clearSelection">
          {{ localizeLabel('清空') }}
        </button>
        <slot name="batch-actions" :selected-keys="selectedKeys" />
      </div>

      <div class="task-list-tools">
        <slot name="filters" />
        <n-input
          v-if="showSearch"
          :value="searchValue"
          :placeholder="localizeLabel(searchPlaceholder)"
          clearable
          class="task-list-search"
          @update:value="emit('update:searchValue', $event)"
          @keydown.enter="emit('search')"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-button quaternary class="task-list-icon-btn" :title="localizeLabel('刷新')" :aria-label="localizeLabel('刷新列表')" @click="emit('refresh')">
          <n-icon :size="18">
            <RefreshOutline />
          </n-icon>
        </n-button>
      </div>
    </div>

    <n-spin :show="loading">
      <div v-if="items.length > 0" class="task-card-stack">
        <article
          v-for="item in items"
          :key="getRowKey(item)"
          class="task-card-row"
          :class="{ selected: isSelected(item), unread: isUnread(item) }"
          @click="emit('rowClick', item)"
        >
          <div v-if="selectable" class="task-card-check" @click.stop>
            <n-checkbox :checked="isSelected(item)" @update:checked="checked => toggleRow(item, checked)" />
          </div>

          <div class="task-card-main">
            <div class="task-card-heading">
              <slot name="status" :row="item" />
              <button type="button" class="task-card-title" @click.stop="emit('rowClick', item)">
                <slot name="title" :row="item">
                  {{ item.title || item.taskName || '-' }}
                </slot>
              </button>
            </div>
            <div class="task-card-meta">
              <slot name="meta" :row="item" />
            </div>
            <div v-if="$slots.summary" class="task-card-summary">
              <slot name="summary" :row="item" />
            </div>
          </div>

          <div v-if="$slots.actions" class="task-card-actions" @click.stop>
            <slot name="actions" :row="item" />
          </div>
        </article>
      </div>
      <n-empty v-else class="task-list-empty" :description="localizeLabel(emptyText)" size="small" />
    </n-spin>

    <div v-if="pagination && pagination.itemCount > 0" class="task-list-pagination">
      <n-pagination
        :page="pagination.page"
        :page-size="pagination.pageSize"
        :item-count="pagination.itemCount"
        :page-sizes="pagination.pageSizes || [10, 20, 50]"
        :show-size-picker="pagination.showSizePicker !== false"
        @update:page="emit('update:page', $event)"
        @update:page-size="emit('update:pageSize', $event)"
      />
    </div>
  </section>
</template>

<script setup>
import { RefreshOutline } from '@vicons/ionicons5'

import { computed } from 'vue'
import { localizeLabel, t } from '@/locales'

const props = defineProps({
  title: { type: String, default: '列表' },
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  pagination: { type: Object, default: null },
  selectedKeys: { type: Array, default: () => [] },
  rowKey: { type: [String, Function], default: 'id' },
  searchValue: { type: String, default: '' },
  searchPlaceholder: { type: String, default: '通过名称搜索' },
  emptyText: { type: String, default: '暂无数据' },
  selectable: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  unreadKey: { type: String, default: '' },
})

const emit = defineEmits([
  'update:selectedKeys',
  'update:searchValue',
  'update:page',
  'update:pageSize',
  'search',
  'refresh',
  'rowClick',
])

const selectedSet = computed(() => new Set(props.selectedKeys))
const currentPageKeys = computed(() => props.items.map(item => getRowKey(item)).filter(key => key !== undefined && key !== null))
const allCurrentPageSelected = computed(() => currentPageKeys.value.length > 0 && currentPageKeys.value.every(key => selectedSet.value.has(key)))
const partiallySelected = computed(() => currentPageKeys.value.some(key => selectedSet.value.has(key)) && !allCurrentPageSelected.value)

function getRowKey(row) {
  if (typeof props.rowKey === 'function')
    return props.rowKey(row)
  return row?.[props.rowKey]
}

function isSelected(row) {
  return selectedSet.value.has(getRowKey(row))
}

function isUnread(row) {
  return props.unreadKey ? row?.[props.unreadKey] === 0 : false
}

function toggleRow(row, checked) {
  const key = getRowKey(row)
  if (key === undefined || key === null)
    return
  const next = new Set(props.selectedKeys)
  if (checked)
    next.add(key)
  else
    next.delete(key)
  emit('update:selectedKeys', [...next])
}

function toggleCurrentPage(checked) {
  const next = new Set(props.selectedKeys)
  currentPageKeys.value.forEach((key) => {
    if (checked)
      next.add(key)
    else
      next.delete(key)
  })
  emit('update:selectedKeys', [...next])
}

function clearSelection() {
  emit('update:selectedKeys', [])
}
</script>

<style scoped>
.flow-task-card-list {
  box-sizing: border-box;
  display: flex;
  flex: 1;
  width: 100%;
  max-width: none;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}

:deep(.n-spin-container),
:deep(.n-spin-content) {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  width: 100%;
}

.task-list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex: 0 0 auto;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.task-list-titlebar {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.task-list-title {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.task-list-selected {
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.task-list-clear {
  color: var(--primary-color);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
}

.task-list-tools {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.task-list-search {
  width: 300px;
}

.task-list-icon-btn {
  width: 34px;
  min-width: 34px;
  height: 34px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 18px;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

.task-list-icon-btn:hover {
  border-color: color-mix(in srgb, var(--primary-color) 34%, var(--border-light));
  background: color-mix(in srgb, var(--primary-color) 7%, var(--bg-primary));
  color: var(--primary-color);
}

:deep(.task-list-icon-btn .n-button__content) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
}

:deep(.task-list-icon-btn .n-icon) {
  color: currentColor;
}

:deep(.task-list-icon-btn i) {
  font-size: 18px;
  line-height: 1;
}

.task-card-stack {
  display: flex;
  width: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px;
}

.task-card-row {
  box-sizing: border-box;
  display: grid;
  flex: 0 0 auto;
  width: 100%;
  min-height: 82px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  column-gap: 12px;
  border: 1px solid var(--border-light);
  border-radius: 7px;
  background: var(--bg-primary);
  padding: 12px 14px;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
  overflow: hidden;
}

.task-card-row:hover,
.task-card-row.selected {
  border-color: color-mix(in srgb, var(--primary-color) 30%, var(--border-light));
  background: color-mix(in srgb, var(--primary-color) 4%, var(--bg-primary));
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.05);
  transform: translateY(-1px);
}

.task-card-row.unread {
  background: color-mix(in srgb, var(--primary-color) 5%, var(--bg-primary));
}

.task-card-check {
  display: flex;
  align-items: flex-start;
  align-self: stretch;
  padding-top: 4px;
}

.task-card-main {
  min-width: 0;
  overflow: hidden;
}

.task-card-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.task-card-title {
  min-width: 0;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card-title:hover {
  color: var(--primary-color);
}

.task-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px 18px;
  margin-top: 9px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 20px;
  overflow: hidden;
}

.task-card-summary {
  display: -webkit-box;
  max-width: 100%;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.task-card-summary > * {
  min-width: 0;
}

.task-card-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: flex-start;
  justify-content: flex-end;
  justify-self: end;
  gap: 8px;
  min-width: 168px;
  max-width: 260px;
  flex-wrap: wrap;
}

:global(.task-row-link-action) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 30px;
  padding: 0 9px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

:global(.task-row-link-action:hover) {
  border-color: color-mix(in srgb, var(--primary-color) 24%, var(--border-light));
  background: color-mix(in srgb, var(--primary-color) 7%, transparent);
  color: var(--primary-color);
}

:global(.task-row-link-action.primary) {
  color: var(--primary-color);
}

:global(.task-row-link-action.info) {
  color: #2563eb;
}

:global(.task-row-link-action.info:hover) {
  background: #eff6ff;
  color: #1d4ed8;
}

:global(.task-row-link-action.success) {
  color: #15803d;
}

:global(.task-row-link-action.success:hover) {
  background: #ecfdf3;
  color: #166534;
}

:global(.task-row-link-action.danger) {
  color: #b42318;
}

:global(.task-row-link-action.danger:hover) {
  background: #fff1f2;
  color: #9f1239;
}

:global(.task-row-link-action i) {
  font-size: 18px;
  line-height: 1;
}

.task-list-empty {
  flex: 1;
  padding: 72px 0;
}

.task-list-pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  padding: 10px 14px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

:global(.task-status-pill) {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 4px;
  background: #fff8e8;
  color: #c07b16;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

:global(.task-list-hint) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--primary-color) 8%, transparent);
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

:global(.task-list-hint.urgent) {
  background: #fff7ed;
  color: #c2410c;
}

:global(.task-list-hint.pending) {
  background: #eff6ff;
  color: #2563eb;
}

:global(.task-status-pill.success),
:global(.task-status-pill.read) {
  background: #ecfdf3;
  color: #15803d;
}

:global(.task-status-pill.error),
:global(.task-status-pill.unread) {
  background: #fff1f2;
  color: #be123c;
}

:global(.task-status-pill.info) {
  background: #eff6ff;
  color: #2563eb;
}

:global(.task-status-pill.default) {
  background: #f1f5f9;
  color: #64748b;
}

:global(.task-meta-label) {
  color: var(--text-secondary);
  font-weight: 400;
}

:global(.task-meta-value) {
  color: var(--text-primary);
  font-weight: 500;
}

@media (max-width: 900px) {
  .task-list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .task-list-tools {
    flex-wrap: wrap;
  }

  .task-list-search {
    width: min(100%, 328px);
  }

  .task-card-row {
    grid-template-columns: auto minmax(0, 1fr);
    row-gap: 14px;
    min-height: 0;
    padding: 12px;
  }

  .task-card-actions {
    grid-column: 1 / -1;
    min-width: 0;
    justify-content: flex-end;
  }
}
</style>
