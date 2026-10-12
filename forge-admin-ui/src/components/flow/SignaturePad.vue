<template>
  <div class="signature-pad" :class="{ 'is-disabled': disabled }">
    <div class="signature-canvas-shell" :style="{ height: `${height}px` }">
      <canvas
        ref="canvasRef"
        class="signature-canvas"
        @pointerdown="handlePointerDown"
        @pointermove="handlePointerMove"
        @pointerup="handlePointerUp"
        @pointercancel="handlePointerUp"
        @pointerleave="handlePointerLeave"
      />
      <div v-if="isBlank" class="signature-placeholder">
        {{ localizeLabel('手写签名') }}
      </div>
      <NTooltip v-if="!disabled" trigger="hover">
        <template #trigger>
          <NButton
            class="signature-clear"
            size="tiny"
            circle
            quaternary
            :disabled="isBlank"
            @click.stop="clear"
          >
            <template #icon>
              <i class="i-material-symbols:delete-outline" />
            </template>
          </NButton>
        </template>
        {{ localizeLabel('清空签名') }}
      </NTooltip>
    </div>
  </div>
</template>

<script setup>
import { NButton, NTooltip } from 'naive-ui'

import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { localizeLabel } from '@/locales'
import { request } from '@/utils'

const props = defineProps({
  modelValue: { type: String, default: '' },
  businessType: { type: String, default: 'flow_signature' },
  businessId: { type: String, default: '' },
  storageType: { type: String, default: '' },
  height: { type: Number, default: 180 },
  disabled: { type: Boolean, default: false },
  strokeWidth: { type: Number, default: 2.6 },
})

const emit = defineEmits(['update:modelValue', 'uploaded', 'clear'])

const canvasRef = ref(null)
const hasInk = ref(false)
const dirty = ref(false)
const drawing = ref(false)
const strokes = ref([])
const canvasSize = ref({ width: 0, height: props.height })

let resizeObserver = null
let activeStroke = null
let dpr = 1

const isBlank = computed(() => !hasInk.value && !props.modelValue)

function getContext() {
  const canvas = canvasRef.value
  if (!canvas)
    return null

  const ctx = canvas.getContext('2d')
  if (!ctx)
    return null

  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.lineWidth = props.strokeWidth
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = '#111827'
  ctx.fillStyle = '#111827'
  return ctx
}

function paintBackground(ctx) {
  ctx.clearRect(0, 0, canvasSize.value.width, canvasSize.value.height)
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvasSize.value.width, canvasSize.value.height)
  ctx.fillStyle = '#111827'
}

function drawStroke(ctx, points) {
  if (!ctx || !points?.length)
    return

  if (points.length === 1) {
    const point = points[0]
    ctx.beginPath()
    ctx.arc(point.x, point.y, props.strokeWidth / 2, 0, Math.PI * 2)
    ctx.fill()
    return
  }

  ctx.beginPath()
  ctx.moveTo(points[0].x, points[0].y)
  for (let i = 1; i < points.length; i += 1) {
    ctx.lineTo(points[i].x, points[i].y)
  }
  ctx.stroke()
}

function redraw() {
  const ctx = getContext()
  if (!ctx)
    return

  paintBackground(ctx)
  strokes.value.forEach(points => drawStroke(ctx, points))
}

function resizeCanvas() {
  const canvas = canvasRef.value
  const host = canvas?.parentElement
  if (!canvas || !host)
    return

  const rect = host.getBoundingClientRect()
  const width = Math.max(Math.floor(rect.width), 1)
  const height = props.height
  dpr = Math.max(window.devicePixelRatio || 1, 1)

  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  canvasSize.value = { width, height }

  redraw()
}

function getPoint(event) {
  const rect = canvasRef.value.getBoundingClientRect()
  return {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
}

function startStroke(event) {
  const point = getPoint(event)
  activeStroke = [point]
  strokes.value.push(activeStroke)
  hasInk.value = true
  dirty.value = true
  if (props.modelValue)
    emit('update:modelValue', '')

  const ctx = getContext()
  drawStroke(ctx, activeStroke)
}

function appendStroke(event) {
  if (!activeStroke)
    return

  const point = getPoint(event)
  const previous = activeStroke[activeStroke.length - 1]
  activeStroke.push(point)

  const ctx = getContext()
  drawStroke(ctx, [previous, point])
}

function handlePointerDown(event) {
  if (props.disabled)
    return

  event.preventDefault()
  drawing.value = true
  canvasRef.value?.setPointerCapture?.(event.pointerId)
  startStroke(event)
}

function handlePointerMove(event) {
  if (!drawing.value || props.disabled)
    return

  event.preventDefault()
  appendStroke(event)
}

function handlePointerUp(event) {
  if (!drawing.value)
    return

  event.preventDefault()
  drawing.value = false
  activeStroke = null
  canvasRef.value?.releasePointerCapture?.(event.pointerId)
}

function handlePointerLeave(event) {
  if (drawing.value)
    handlePointerUp(event)
}

function clear() {
  strokes.value = []
  activeStroke = null
  drawing.value = false
  hasInk.value = false
  dirty.value = false
  emit('update:modelValue', '')
  emit('clear')
  redraw()
}

function hasSignature() {
  return Boolean(props.modelValue?.trim()) || hasInk.value
}

function exportBlob() {
  if (!hasInk.value) {
    return Promise.reject(new Error(localizeLabel('请完成手写签名')))
  }

  return new Promise((resolve, reject) => {
    const canvas = canvasRef.value
    if (!canvas) {
      reject(new Error(localizeLabel('签名画布未初始化')))
      return
    }

    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error(localizeLabel('签名图片生成失败')))
        return
      }
      resolve(blob)
    }, 'image/png')
  })
}

async function upload() {
  if (props.modelValue && !dirty.value)
    return props.modelValue

  const blob = await exportBlob()
  const formData = new FormData()
  const safeBusinessId = String(props.businessId || Date.now()).replace(/[^\w-]/g, '')
  const fileName = `signature-${safeBusinessId || Date.now()}.png`
  formData.append('file', blob, fileName)
  formData.append('businessType', props.businessType)
  if (props.businessId)
    formData.append('businessId', props.businessId)
  if (props.storageType)
    formData.append('storageType', props.storageType)
  formData.append('isPrivate', 'true')

  const res = await request({
    method: 'post',
    url: '/api/file/upload',
    data: formData,
    encrypt: false,
    timeout: 30000,
  })

  const fileId = res?.data?.fileId
  if (!fileId)
    throw new Error(localizeLabel('签名图片保存失败'))

  dirty.value = false
  emit('update:modelValue', fileId)
  emit('uploaded', res.data)
  return fileId
}

onMounted(() => {
  nextTick(() => {
    resizeCanvas()
    if (typeof ResizeObserver === 'undefined')
      return

    resizeObserver = new ResizeObserver(resizeCanvas)
    if (canvasRef.value?.parentElement)
      resizeObserver.observe(canvasRef.value.parentElement)
  })
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect?.()
  resizeObserver = null
})

defineExpose({
  clear,
  exportBlob,
  hasSignature,
  upload,
})
</script>

<style scoped>
.signature-pad {
  width: 100%;
}

.signature-canvas-shell {
  position: relative;
  width: 100%;
  border: 1px solid #d9dce6;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.signature-canvas {
  display: block;
  width: 100%;
  height: 100%;
  cursor: crosshair;
  touch-action: none;
}

.signature-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a8acb8;
  font-size: 14px;
  pointer-events: none;
}

.signature-clear {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e5e7eb;
}

.is-disabled .signature-canvas {
  cursor: default;
}
</style>
