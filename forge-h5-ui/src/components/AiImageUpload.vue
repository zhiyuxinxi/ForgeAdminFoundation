<template>
  <view class="ai-image-upload-shell" :class="{ 'is-uploading': uploading }">
    <view class="ai-image-upload" @click="openPicker">
      <slot :src="modelValue" :uploading="uploading">
        <view class="ai-image-upload__avatar">
          <AiAuthImage :src="modelValue" :fallback="fallback" mode="aspectFill" />
          <view class="ai-image-upload__mask">
            <AiIcon icon="/static/icons/ai-icon/camera.svg" color="#ffffff" size="sm" />
            <text>{{ uploading ? L("上传中") : L("更换") }}</text>
          </view>
        </view>
      </slot>
    </view>

    <AiAvatarCropper
      v-model="cropVisible"
      :source="cropSource"
      :size="size"
      :quality="quality"
      @confirm="handleCropConfirm"
      @cancel="cancelCrop"
    />
  </view>
</template>

<script setup>
import { L, getLocale } from '@/locales'
import { onUnmounted, ref } from 'vue'
import AiAuthImage from '@/components/AiAuthImage.vue'
import AiAvatarCropper from '@/components/AiAvatarCropper.vue'
import AiIcon from '@/components/AiIcon.vue'
import { useAuthStore } from '@/store'
import { toast } from '@/utils/notify'

const props = defineProps({
  modelValue: {
    type: [String, Object],
    default: '',
  },
  fallback: {
    type: String,
    default: '',
  },
  businessType: {
    type: String,
    default: 'image',
  },
  crop: {
    type: Boolean,
    default: true,
  },
  size: {
    type: Number,
    default: 512,
  },
  quality: {
    type: Number,
    default: 0.9,
  },
})

const emit = defineEmits(['update:modelValue', 'success', 'error', 'uploadStart', 'uploadEnd'])
const authStore = useAuthStore()
const uploading = ref(false)
const cropVisible = ref(false)
const cropSource = ref('')
let cropObjectUrl = ''

async function openPicker() {
  if (uploading.value) {
    return
  }
  const picked = await chooseImageFile()
  if (!picked?.url) {
    return
  }

  try {
    const file = picked.file || await urlToFile(picked.url)
    if (!file.type?.startsWith('image/')) {
      toast(L("请选择图片文件"), { type: 'warning' })
      return
    }

    if (!props.crop) {
      await uploadFile(file)
      return
    }

    openCropper(URL.createObjectURL(file))
  }
  catch (error) {
    console.error('选择图片失败:', error)
    toast(error?.message || L("选择图片失败"), { type: 'error' })
  }
}

function chooseImageFile() {
  return new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['original', 'compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = res.tempFiles?.[0]?.file || res.tempFiles?.[0]
        resolve({
          url: res.tempFilePaths?.[0] || file?.path || file?.url || '',
          file,
        })
      },
      fail: () => resolve(null),
    })
  })
}

async function urlToFile(url) {
  const response = await fetch(url)
  const blob = await response.blob()
  return new File([blob], `avatar-${Date.now()}.${resolveExtension(blob.type)}`, { type: blob.type || 'image/jpeg' })
}

function openCropper(url) {
  releaseCropObjectUrl()
  cropObjectUrl = url
  cropSource.value = url
  cropVisible.value = true
}

function cancelCrop() {
  cropVisible.value = false
  cropSource.value = ''
  releaseCropObjectUrl()
}

async function handleCropConfirm({ file }) {
  if (!file || uploading.value) {
    return
  }
  try {
    await uploadFile(file)
  }
  catch (error) {
    console.error('裁剪上传失败:', error)
    toast(error?.message || L("裁剪上传失败"), { type: 'error' })
  }
  finally {
    cancelCrop()
  }
}

async function uploadFile(file) {
  uploading.value = true
  emit('uploadStart')
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', props.businessType)
    const response = await fetch(`${import.meta.env.VITE_REQUEST_PREFIX || ''}/api/file/upload`, {
      method: 'POST',
      headers: {
        'Accept-Language': getLocale(),
        Authorization: `${authStore.tokenType || 'Bearer'} ${authStore.accessToken}`,
      },
      body: formData,
    })
    const result = await response.json()
    if (!response.ok || !(result?.code === 200 || result?.respCode === '0000')) {
      throw new Error(result?.message || result?.msg || L("图片上传失败"))
    }

    const fileData = result.data
    const value = fileData?.fileId || fileData?.filePath || fileData?.id || ''
    emit('update:modelValue', value)
    emit('success', fileData)
  }
  catch (error) {
    emit('error', error)
    throw error
  }
  finally {
    uploading.value = false
    emit('uploadEnd')
  }
}

function resolveExtension(type) {
  if (type?.includes('png')) {
    return 'png'
  }
  if (type?.includes('webp')) {
    return 'webp'
  }
  return 'jpg'
}

function releaseCropObjectUrl() {
  if (cropObjectUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(cropObjectUrl)
  }
  cropObjectUrl = ''
}

onUnmounted(() => {
  releaseCropObjectUrl()
})
</script>

<style lang="scss" scoped>
.ai-image-upload-shell,
.ai-image-upload {
  display: inline-flex;
}

.ai-image-upload__avatar {
  position: relative;
  width: 156rpx;
  height: 156rpx;
  overflow: hidden;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #dbeafe, #e0e7ff);
}

.ai-image-upload__mask {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 10rpx 8rpx;
  background: rgba(15, 23, 42, 0.64);
}

.ai-image-upload__mask text {
  display: block;
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 850;
}

</style>
