<template>
  <div
    class="page-list-item"
    :class="{ active, home, modal: isModal }"
    @click="$emit('select')"
  >
    <div class="page-index">
      {{ displayIndex }}
    </div>

    <div class="page-main">
      <n-input
        v-if="editing"
        ref="inputRef"
        v-model:value="draftName"
        size="tiny"
        class="page-name-input"
        @click.stop
        @blur="saveRename"
        @keyup.enter="saveRename"
        @keyup.esc="cancelRename"
      />
      <template v-else>
        <n-ellipsis class="page-name">
          {{ page.name }}
        </n-ellipsis>
        <div class="page-meta">
          <span v-if="isModal">{{ $ui("弹窗") }}</span>
          <span v-if="home">{{ $ui("首页") }}</span>
          <span>{{ page.componentList?.length || 0 }} {{ $ui("组件") }}</span>
        </div>
      </template>
    </div>

    <n-dropdown
      trigger="click"
      size="small"
      :options="options"
      @select="handleAction"
    >
      <n-button class="page-more" quaternary circle size="tiny" @click.stop>
        <template #icon>
          <n-icon size="15">
            <ellipsis-horizontal-circle-sharp-icon />
          </n-icon>
        </template>
      </n-button>
    </n-dropdown>
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, nextTick, PropType, ref } from 'vue'
import { NIcon } from 'naive-ui'
import { renderIcon } from '@/utils'
import { icon } from '@/plugins'
import type { ReportCanvasPage } from '@/store/modules/chartEditStore/chartEditStore.d'

const {
  CopyIcon,
  EllipsisHorizontalCircleSharpIcon,
  HomeIcon,
  PencilIcon,
  TrashIcon,
  ChevronUpIcon,
  ChevronDownIcon
} = icon.ionicons5

const props = defineProps({
  page: {
    type: Object as PropType<ReportCanvasPage>,
    required: true
  },
  index: {
    type: Number,
    required: true
  },
  active: {
    type: Boolean,
    default: false
  },
  home: {
    type: Boolean,
    default: false
  },
  onlyOne: {
    type: Boolean,
    default: false
  },
  first: {
    type: Boolean,
    default: false
  },
  last: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'select',
  'rename',
  'duplicate',
  'delete',
  'setHome',
  'moveUp',
  'moveDown'
])

const inputRef = ref()
const editing = ref(false)
const draftName = ref(props.page.name)

const displayIndex = computed(() => String(props.index + 1).padStart(2, '0'))
const isModal = computed(() => props.page.pageType === 'modal')

const options = computed(() => [
  {
    get label() { return uiText("重命名") },
    key: 'rename',
    icon: renderIcon(PencilIcon)
  },
  {
    get label() { return uiText("设为首页") },
    key: 'home',
    disabled: props.home || isModal.value,
    icon: renderIcon(HomeIcon)
  },
  {
    get label() { return uiText("复制页面") },
    key: 'duplicate',
    icon: renderIcon(CopyIcon)
  },
  {
    get label() { return uiText("上移") },
    key: 'moveUp',
    disabled: props.first,
    icon: renderIcon(ChevronUpIcon)
  },
  {
    get label() { return uiText("下移") },
    key: 'moveDown',
    disabled: props.last,
    icon: renderIcon(ChevronDownIcon)
  },
  {
    type: 'divider',
    key: 'divider'
  },
  {
    get label() { return uiText("删除") },
    key: 'delete',
    disabled: props.onlyOne,
    icon: renderIcon(TrashIcon)
  }
])

const startRename = async () => {
  draftName.value = props.page.name
  editing.value = true
  await nextTick()
  inputRef.value?.focus()
}

const saveRename = () => {
  const nextName = draftName.value.trim()
  editing.value = false
  if (nextName && nextName !== props.page.name) {
    emit('rename', nextName)
  }
}

const cancelRename = () => {
  draftName.value = props.page.name
  editing.value = false
}

const handleAction = (key: string) => {
  if (key === 'rename') {
    startRename()
    return
  }
  if (key === 'home') emit('setHome')
  if (key === 'duplicate') emit('duplicate')
  if (key === 'delete') emit('delete')
  if (key === 'moveUp') emit('moveUp')
  if (key === 'moveDown') emit('moveDown')
}
</script>

<style lang="scss" scoped>
.page-list-item {
  position: relative;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 28px;
  align-items: center;
  gap: 8px;
  min-height: 54px;
  margin: 6px;
  padding: 7px 7px 7px 8px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid rgba(148, 163, 184, 0.11);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.045), transparent 52%),
    rgba(15, 23, 42, 0.46);
  @extend .go-transition-quick;

  &:hover {
    border-color: rgba(var(--app-theme-rgb), 0.22);
    background:
      linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.13), transparent 58%),
      rgba(15, 23, 42, 0.58);
  }

  &.active {
    border-color: rgba(var(--app-theme-rgb), 0.82);
    box-shadow:
      0 0 16px rgba(var(--app-theme-rgb), 0.16),
      inset 3px 0 0 var(--app-theme);
  }

  &.home .page-index {
    color: var(--app-theme);
    border-color: rgba(var(--app-theme-rgb), 0.28);
  }

  &.modal .page-index {
    color: #38bdf8;
    border-color: rgba(56, 189, 248, 0.28);
    background: rgba(14, 116, 144, 0.14);
  }
}

.page-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 7px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(2, 6, 23, 0.42);
  color: rgba(226, 232, 240, 0.72);
  font-size: 11px;
  font-weight: 700;
}

.page-main {
  min-width: 0;
}

.page-name {
  display: block;
  max-width: 100%;
  color: rgba(226, 232, 240, 0.94);
  font-size: 12px;
  font-weight: 650;
}

.page-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: rgba(148, 163, 184, 0.72);
  font-size: 10px;

  span + span::before {
    content: '';
    display: inline-block;
    width: 3px;
    height: 3px;
    margin-right: 6px;
    border-radius: 50%;
    background: rgba(var(--app-theme-rgb), 0.65);
    vertical-align: middle;
  }
}

.page-more {
  color: rgba(203, 213, 225, 0.78);
}

.page-name-input {
  width: 100%;
}
</style>
