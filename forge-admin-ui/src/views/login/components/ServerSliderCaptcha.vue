<template>
  <div class="server-slider-captcha" :aria-busy="fetching || busy">
    <p v-if="fetching" role="status">
      {{ t('login.slider.loading') }}
    </p>
    <p v-if="errorKey" role="alert">
      {{ t(errorKey) }}
    </p>
    <template v-if="challenge">
      <div class="puzzle" :style="{ aspectRatio: `${challenge.backgroundWidth} / ${challenge.backgroundHeight}` }">
        <img class="background" :src="challenge.backgroundImage" :alt="t('login.slider.background')" draggable="false">
        <img
          class="piece" :src="challenge.sliderImage" alt="" draggable="false"
          :style="{
            width: `${challenge.sliderWidth / challenge.backgroundWidth * 100}%`,
            height: `${challenge.sliderHeight / challenge.backgroundHeight * 100}%`,
            left: `${moveX / challenge.backgroundWidth * 100}%`,
            top: `${challenge.notchY / challenge.backgroundHeight * 100}%`,
          }"
        >
      </div>
      <label>
        {{ t('login.slider.move') }}
        <input
          v-model.number="moveX" type="range" min="0" :max="challenge.backgroundWidth - challenge.sliderWidth"
          step="1" :disabled="busy || submitted" :aria-label="t('login.slider.move')"
        >
      </label>
    </template>
    <div class="actions">
      <button type="button" :disabled="busy || fetching" @click="refresh">
        {{ t('login.slider.refresh') }}
      </button>
      <button type="button" :disabled="busy || fetching || submitted || !challenge" @click="submit">
        {{ t('login.slider.submit') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { t } from '@/locales'
import api from '../api'

const props = defineProps({ busy: Boolean })
const emit = defineEmits(['submit'])
const challenge = ref(null)
const moveX = ref(0)
const fetching = ref(false)
const submitted = ref(false)
const errorKey = ref('')
let requestVersion = 0
let expiresAt = 0

function isValidChallenge(value) {
  if (!value || typeof value.codeKey !== 'string' || !value.codeKey.trim())
    return false
  const dimensions = ['backgroundWidth', 'backgroundHeight', 'sliderWidth', 'sliderHeight']
  return dimensions.every(key => Number.isInteger(value[key]) && value[key] > 0 && value[key] <= 4096)
    && value.sliderWidth < value.backgroundWidth && value.sliderHeight < value.backgroundHeight
    && Number.isInteger(value.notchY) && value.notchY >= 0
    && value.notchY + value.sliderHeight <= value.backgroundHeight
    && Number.isFinite(value.expiresIn) && value.expiresIn > 0
    && [value.backgroundImage, value.sliderImage].every(image => typeof image === 'string' && /^data:image\/(?:png|jpeg);base64,[\w+/=]+$/.test(image))
}

async function refresh() {
  const version = ++requestVersion
  challenge.value = null
  moveX.value = 0
  submitted.value = false
  fetching.value = true
  errorKey.value = ''
  try {
    const response = await api.getSliderCaptcha()
    if (version !== requestVersion)
      return
    if (response.code !== 200 || !isValidChallenge(response.data))
      throw new Error('Invalid slider challenge')
    challenge.value = response.data
    expiresAt = Date.now() + response.data.expiresIn * 1000
  }
  catch {
    if (version === requestVersion)
      errorKey.value = 'login.slider.loadFailed'
  }
  finally {
    if (version === requestVersion)
      fetching.value = false
  }
}

function submit() {
  if (!challenge.value || props.busy || fetching.value || submitted.value)
    return
  if (Date.now() >= expiresAt) {
    challenge.value = null
    errorKey.value = 'login.slider.expired'
    return
  }
  const position = Number(moveX.value)
  if (!Number.isInteger(position) || position < 0 || position > challenge.value.backgroundWidth - challenge.value.sliderWidth)
    return
  submitted.value = true
  // The login endpoint alone validates the answer and consumes this challenge.
  emit('submit', { codeKey: challenge.value.codeKey, code: String(position) })
}

onMounted(refresh)
onUnmounted(() => requestVersion++)
defineExpose({ refresh })
</script>

<style scoped>
.server-slider-captcha {
  width: 340px;
  max-width: 100%;
}
.puzzle {
  position: relative;
  width: 100%;
  overflow: hidden;
}
.background {
  display: block;
  width: 100%;
  height: 100%;
}
.piece {
  position: absolute;
}
label {
  display: block;
  margin-top: 12px;
}
input {
  display: block;
  width: 100%;
  margin: 8px 0;
}
.actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}
button {
  padding: 6px 12px;
  border: 1px solid currentColor;
  border-radius: 6px;
  cursor: pointer;
}
button:disabled {
  opacity: 0.5;
  cursor: default;
}
</style>
