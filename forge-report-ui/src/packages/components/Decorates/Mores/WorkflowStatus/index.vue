<template>
  <section class="workflow-status" :style="rootStyle">
    <h3>{{ option.title }}</h3>
    <div class="steps">
      <div v-for="(step, index) in steps" :key="step.label" class="step" :class="stepClass(step, index)">
        <div class="dot">{{ index + 1 }}</div>
        <div class="line" v-if="index < steps.length - 1"></div>
        <strong>{{ step.label }}</strong>
        <span>{{ step.desc }}</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, PropType, ref, unref } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { PREVIEW_PAGE_CONTEXT_KEY } from '@/utils/requestDynamicParams'
import { fetchBusinessData, normalizeArrayData, normalizeObjectData } from '@/packages/components/common/businessDataSource'
import { option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const pageContext = inject(PREVIEW_PAGE_CONTEXT_KEY, ref({}))
const remoteSteps = ref<any[]>([])
const remoteActiveIndex = ref<number | undefined>()
const option = computed(() => ({
  ...defaultOption,
  ...(props.chartConfig.option || {}),
  style: { ...defaultOption.style, ...(props.chartConfig.option?.style || {}) },
  dataSource: { ...defaultOption.dataSource, ...(props.chartConfig.option?.dataSource || {}) }
}))
const steps = computed(() => remoteSteps.value.length ? remoteSteps.value : option.value.steps)
const activeIndex = computed(() => remoteActiveIndex.value ?? option.value.activeIndex)
const rootStyle = computed(() => ({
  '--wf-accent': option.value.style.accentColor,
  '--wf-done': option.value.style.doneColor,
  '--wf-todo': option.value.style.todoColor,
  '--wf-text': option.value.style.textColor,
  '--wf-panel': option.value.style.panelColor,
  '--wf-border': option.value.style.borderColor
}))
const stepClass = (step: any, index: number) => step.status || (index < activeIndex.value ? 'done' : index === activeIndex.value ? 'active' : 'todo')

const fetchSteps = async () => {
  const data = await fetchBusinessData(option.value.dataSource, unref(pageContext) || {})
  if (Array.isArray(data)) {
    remoteSteps.value = data
    return
  }
  const detail = normalizeObjectData(data)
  remoteSteps.value = normalizeArrayData(detail.steps)
  remoteActiveIndex.value = Number.isFinite(Number(detail.activeIndex)) ? Number(detail.activeIndex) : undefined
}

onMounted(() => {
  fetchSteps()
  window.addEventListener('forge-report-refresh', fetchSteps)
})

onUnmounted(() => {
  window.removeEventListener('forge-report-refresh', fetchSteps)
})
</script>

<style scoped lang="scss">
.workflow-status {
  width: 100%;
  height: 100%;
  padding: 16px;
  color: var(--wf-text);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  background: var(--wf-panel);
}

h3 {
  margin: 0 0 18px;
  font-size: 16px;
}

.steps {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
}

.step {
  position: relative;
  min-width: 0;

  .dot {
    position: relative;
    z-index: 1;
    display: grid;
    width: 30px;
    height: 30px;
    place-items: center;
    border: 1px solid var(--wf-todo);
    border-radius: 50%;
    color: var(--wf-todo);
    background: rgba(2, 10, 24, 0.9);
  }

  .line {
    position: absolute;
    top: 15px;
    left: 30px;
    right: 8px;
    height: 1px;
    background: rgba(255, 255, 255, 0.16);
  }

  strong,
  span {
    display: block;
    margin-top: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: var(--wf-todo);
    font-size: 12px;
  }
}

.step.done .dot {
  color: #061321;
  border-color: var(--wf-done);
  background: var(--wf-done);
}

.step.active .dot {
  color: #061321;
  border-color: var(--wf-accent);
  background: var(--wf-accent);
  box-shadow: 0 0 18px var(--wf-accent);
}
</style>
