import { uiText } from '@/i18n/display'
import html2canvas from 'html2canvas'
import { uploadFileApi, FileUploadResult } from '@/api/file'

const UNSUPPORTED_COLOR_FUNCTION_REG = /\b(color|color-mix|oklch|oklab|lab|lch|hwb)\(/i
const SAFE_TEXT_COLOR = '#e5e7eb'
const SAFE_BORDER_COLOR = 'rgba(148, 163, 184, 0.22)'

export interface CaptureOptions {
  scale?: number
  backgroundColor?: string | null
  logging?: boolean
  useCORS?: boolean
  allowTaint?: boolean
  businessType?: string
  businessId?: string | number
  isPrivate?: boolean
}

const hasUnsupportedColorFunction = (value?: string | null) => {
  return !!value && UNSUPPORTED_COLOR_FUNCTION_REG.test(value)
}

const setSafeStyle = (element: Element, property: string, value: string) => {
  const target = element as HTMLElement | SVGElement
  target.style?.setProperty(property, value, 'important')
}

const sanitizeElementForHtml2Canvas = (element: Element) => {
  const win = element.ownerDocument.defaultView
  if (!win) return

  const computedStyle = win.getComputedStyle(element)
  const colorProperties = [
    'color',
    'background-color',
    'border-top-color',
    'border-right-color',
    'border-bottom-color',
    'border-left-color',
    'outline-color',
    'text-decoration-color',
    'caret-color',
    'column-rule-color',
    'fill',
    'stroke',
    'stop-color',
    'flood-color',
    'lighting-color'
  ]
  const complexProperties = [
    'background',
    'background-image',
    'box-shadow',
    'text-shadow',
    'filter',
    'backdrop-filter',
    'border-image-source'
  ]

  colorProperties.forEach((property) => {
    const value = computedStyle.getPropertyValue(property)
    if (!hasUnsupportedColorFunction(value)) return

    if (property === 'background-color') {
      setSafeStyle(element, property, 'transparent')
      return
    }
    if (property.includes('border') || property.includes('outline') || property.includes('rule')) {
      setSafeStyle(element, property, SAFE_BORDER_COLOR)
      return
    }
    if (property === 'fill' || property === 'stroke') {
      setSafeStyle(element, property, 'currentColor')
      return
    }
    setSafeStyle(element, property, SAFE_TEXT_COLOR)
  })

  complexProperties.forEach((property) => {
    const value = computedStyle.getPropertyValue(property)
    if (!hasUnsupportedColorFunction(value)) return

    if (property === 'background' || property === 'background-image') {
      setSafeStyle(element, 'background-image', 'none')
      return
    }
    setSafeStyle(element, property, 'none')
  })
}

const sanitizeCloneForHtml2Canvas = (documentClone: Document, clonedElement: Element) => {
  const style = documentClone.createElement('style')
  style.textContent = `
    .go-edit-range-model,
    .go-edit-select,
    .go-edit-align-line {
      display: none !important;
    }
    *::before,
    *::after {
      color: inherit !important;
      background-image: none !important;
      box-shadow: none !important;
      text-shadow: none !important;
      border-color: rgba(148, 163, 184, 0.2) !important;
    }
  `
  documentClone.head.appendChild(style)

  const elements = [clonedElement, ...Array.from(clonedElement.querySelectorAll('*'))]
  elements.forEach(sanitizeElementForHtml2Canvas)
}

export const captureCanvasScreenshot = async (
  element: HTMLElement,
  options: CaptureOptions = {}
): Promise<FileUploadResult> => {
  try {
    console.log('[Capture] 开始截图, 元素尺寸:', element.offsetWidth, 'x', element.offsetHeight)

    const {
      scale = 1,
      backgroundColor = '#ffffff',
      logging = false,
      useCORS = true,
      allowTaint = false,
      businessType = 'project_screenshot',
      businessId,
      isPrivate = true
    } = options

    const canvas = await html2canvas(element, {
      scale,
      backgroundColor,
      logging,
      useCORS,
      allowTaint,
      imageTimeout: 15000,
      removeContainer: true,
      onclone: sanitizeCloneForHtml2Canvas
    })

    console.log('[Capture] Canvas生成成功, 尺寸:', canvas.width, 'x', canvas.height)

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob)
          } else {
            reject(new Error('Canvas toBlob failed'))
          }
        },
        'image/png',
        0.9
      )
    })

    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
    const fileName = `screenshot-${timestamp}.png`
    const file = new File([blob], fileName, { type: 'image/png' })

    console.log('[Capture] 文件生成成功, 名称:', fileName, '大小:', file.size, 'bytes')

    const res = await uploadFileApi(
      file,
      businessType,
      businessId === undefined || businessId === null ? undefined : String(businessId),
      isPrivate
    )
    console.log('[Capture] 上传结果:', res)

    if (res.code === 200 && res.data) {
      return res.data
    }
    console.warn('[Capture] 上传失败, 响应:', res)
    throw new Error(res?.msg || uiText("项目截图上传失败"))
  } catch (error) {
    console.error('[Capture] 截图生成失败:', error)
    throw error
  }
}

export const captureProjectScreenshot = async (
  projectId: string | number,
  canvasElement: HTMLElement
): Promise<string> => {
  console.log('[CaptureProject] 开始项目截图, projectId:', projectId)
  const result = await captureCanvasScreenshot(canvasElement, {
    scale: 0.5,
    backgroundColor: undefined,
    businessType: 'project_screenshot',
    businessId: projectId,
    isPrivate: true
  })
  if (result.fileId) {
    console.log('[CaptureProject] 截图文件ID:', result.fileId)
    return result.fileId
  }
  throw new Error(uiText("项目截图上传成功但未返回文件ID"))
}
