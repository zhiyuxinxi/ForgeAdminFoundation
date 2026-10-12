<template>
  <span v-if="!value" class="signature-empty">-</span>
  <AuthImage
    v-else-if="shouldRenderImage"
    :src="value"
    :alt="localizeLabel('审批签名')"
    :lazy="lazy"
    :img-style="imageStyle"
    @error="imageError = true"
  />
  <span v-else class="signature-text">{{ value }}</span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

import AuthImage from '@/components/common/AuthImage.vue'
import { localizeLabel } from '@/locales'

const props = defineProps({
  value: { type: String, default: '' },
  compact: { type: Boolean, default: false },
  lazy: { type: Boolean, default: true },
})

const imageError = ref(false)

const shouldRenderImage = computed(() => !imageError.value && isSignatureImageValue(props.value))

const imageStyle = computed(() => ({
  width: props.compact ? '88px' : '220px',
  height: props.compact ? '36px' : '72px',
  display: 'block',
  objectFit: 'contain',
  border: '1px solid #e2e8f0',
  borderRadius: '6px',
  background: '#fff',
}))

watch(() => props.value, () => {
  imageError.value = false
})

function isSignatureImageValue(value) {
  if (!value)
    return false

  return value.startsWith('data:image/')
    || value.startsWith('blob:')
    || value.startsWith('/api/file/')
    || value.includes('/api/file/')
    || value.startsWith('http://')
    || value.startsWith('https://')
    || /^[a-f0-9]{32}$/i.test(value)
}
</script>

<style scoped>
.signature-empty {
  color: #94a3b8;
}

.signature-text {
  color: #475569;
  word-break: break-all;
}
</style>
