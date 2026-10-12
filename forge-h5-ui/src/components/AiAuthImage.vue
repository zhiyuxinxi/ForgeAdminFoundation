<template>
  <image
    class="ai-auth-image"
    :class="imgClass"
    :src="currentSrc"
    :mode="mode"
    @load="$emit('load', $event)"
    @error="handleError"
  />
</template>

<script setup>
import { ref, watch } from 'vue'
import { resolveRenderableFileUrl } from '@/utils/file'

const props = defineProps({
  src: {
    type: [String, Object],
    default: '',
  },
  fallback: {
    type: String,
    default: '',
  },
  mode: {
    type: String,
    default: 'aspectFill',
  },
  imgClass: {
    type: [String, Array, Object],
    default: '',
  },
})

const emit = defineEmits(['load', 'error'])
const currentSrc = ref('')
let loadSeq = 0

watch(() => props.src, () => {
  loadImage()
}, { immediate: true })

async function loadImage(forceRefresh = false) {
  const seq = ++loadSeq
  if (!props.src) {
    currentSrc.value = props.fallback
    return
  }

  try {
    const url = await resolveRenderableFileUrl(props.src, { forceRefresh })
    if (seq !== loadSeq) {
      return
    }
    currentSrc.value = url || props.fallback
  }
  catch (error) {
    if (seq !== loadSeq) {
      return
    }
    currentSrc.value = props.fallback
    emit('error', error)
  }
}

function handleError(error) {
  if (props.src && !currentSrc.value?.startsWith('blob:')) {
    loadImage(true)
    return
  }
  if (props.fallback && currentSrc.value !== props.fallback) {
    currentSrc.value = props.fallback
    return
  }
  emit('error', error)
}
</script>

<style lang="scss" scoped>
.ai-auth-image {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
