<template>
  <ExpandTablePanel
    v-if="isTableLikePanel"
    :panel="panel"
    :data="data"
    :row="row"
    :loading="loading"
    :context="context"
  />
  <ExpandDescriptionsPanel
    v-else-if="panel.type === 'descriptions'"
    :panel="panel"
    :data="data"
    :row="row"
    :context="context"
  />
  <ExpandFormPanel
    v-else-if="panel.type === 'form'"
    :panel="panel"
    :data="data"
    :row="row"
    :context="context"
  />
  <ExpandTabsPanel
    v-else-if="panel.type === 'tabs'"
    :panel="panel"
    :data="data"
    :row="row"
    :context="context"
  >
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps" />
    </template>
  </ExpandTabsPanel>
  <ExpandCustomPanel
    v-else-if="panel.type === 'custom'"
    :panel="panel"
    :data="data"
    :row="row"
    :context="context"
  >
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps" />
    </template>
  </ExpandCustomPanel>
  <ExpandDescriptionsPanel
    v-else
    :panel="fallbackPanel"
    :data="data"
    :row="row"
    :context="context"
  />
</template>

<script setup>
import { computed } from 'vue'
import ExpandCustomPanel from './expand-renderers/ExpandCustomPanel.vue'
import ExpandDescriptionsPanel from './expand-renderers/ExpandDescriptionsPanel.vue'
import ExpandFormPanel from './expand-renderers/ExpandFormPanel.vue'
import ExpandTablePanel from './expand-renderers/ExpandTablePanel.vue'
import ExpandTabsPanel from './expand-renderers/ExpandTabsPanel.vue'

const props = defineProps({
  panel: { type: Object, required: true },
  row: { type: Object, default: () => ({}) },
  data: { type: null, default: null },
  loading: { type: Boolean, default: false },
  context: { type: Object, default: () => ({}) },
})

const fallbackPanel = computed(() => ({
  ...props.panel,
  descriptions: props.panel.descriptions || { fields: [] },
}))

const isTableLikePanel = computed(() => ['table', 'quantity-balance', 'quantity-ledger', 'quantity-lock'].includes(props.panel.type))
</script>
