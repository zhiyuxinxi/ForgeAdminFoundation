<template>
  <div ref="hostRef" class="fg-auth-image" :class="{ loaded: imageReady }">
    <img
      v-if="currentSrc"
      :src="currentSrc"
      :alt="alt"
      :class="imgClass"
      :style="imgStyle"
      :loading="lazy ? 'lazy' : 'eager'"
      decoding="async"
      @load="handleLoad"
      @error="handleError"
    >
    <div v-else class="fg-auth-image__placeholder" :class="placeholderClass">
      <slot name="placeholder">
        <span class="fg-auth-image__placeholder-text">{{ placeholderText }}</span>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { parseFileAssetRef, resolveAssetSourceUrl } from '@/utils/assetRef'
import { removeCachedFileAccessUrl } from '@/utils/fileAccess'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  alt: {
    type: String,
    default: ''
  },
  fallback: {
    type: String,
    default: ''
  },
  imgClass: {
    type: [String, Array, Object],
    default: ''
  },
  imgStyle: {
    type: [String, Object],
    default: ''
  },
  placeholderClass: {
    type: [String, Array, Object],
    default: ''
  },
  placeholderText: {
    type: String,
    default: '加载中...'
  },
  lazy: {
    type: Boolean,
    default: true
  },
  expires: {
    type: Number,
    default: 43200
  }
})

const emit = defineEmits(['load', 'error'])

const hostRef = ref<HTMLElement | null>(null)
const currentSrc = ref('')
const imageReady = ref(false)
const retried = ref(false)
let observer: IntersectionObserver | null = null

const isDirectUrl = (value: string) => {
  return value.startsWith('http://')
    || value.startsWith('https://')
    || value.startsWith('data:')
    || value.startsWith('blob:')
}

const loadImage = async () => {
  if (!props.src) {
    currentSrc.value = props.fallback || ''
    return
  }

  try {
    currentSrc.value = isDirectUrl(props.src)
      ? props.src
      : await resolveAssetSourceUrl(props.src, props.expires)
  } catch (error) {
    currentSrc.value = props.fallback || ''
    emit('error', error)
  }
}

const observeLazyLoad = () => {
  if (!props.lazy || !hostRef.value) {
    loadImage()
    return
  }

  observer?.disconnect()
  observer = new IntersectionObserver((entries) => {
    const entry = entries[0]
    if (!entry?.isIntersecting) {
      return
    }
    observer?.disconnect()
    observer = null
    loadImage()
  }, {
    rootMargin: '220px 0px'
  })
  observer.observe(hostRef.value)
}

const handleLoad = () => {
  imageReady.value = true
  emit('load')
}

const handleError = async (error: Event) => {
  if (!retried.value && props.src && !isDirectUrl(props.src)) {
    retried.value = true
    removeCachedFileAccessUrl(parseFileAssetRef(props.src))
    try {
      currentSrc.value = await resolveAssetSourceUrl(props.src, props.expires)
      return
    } catch (retryError) {
      emit('error', retryError)
    }
  }

  if (props.fallback && currentSrc.value !== props.fallback) {
    currentSrc.value = props.fallback
    return
  }
  emit('error', error)
}

watch(() => props.src, () => {
  imageReady.value = false
  retried.value = false
  currentSrc.value = ''
  nextTick(() => observeLazyLoad())
}, { immediate: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})
</script>

<style lang="scss" scoped>
.fg-auth-image {
  width: 100%;
  height: 100%;
  text-align: center;
  img{
    max-width: 100%;
    height: auto;
    object-fit: fill;
  }
}

.fg-auth-image__placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fg-auth-image__placeholder-text {
  font-size: 12px;
  opacity: 0.72;
}
</style>
