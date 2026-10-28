import { describe, expect, it } from 'vitest'
import { serializeFinishedImageValue, upsertFinishedImage } from '../image-upload-value'

describe('image upload form value', () => {
  it('synchronously persists the uploaded file id into the controlled list', () => {
    const controlledFile = { id: 'upload-1', name: '现场照片.png', status: 'uploading' }
    const callbackFile = { id: 'upload-1', name: '现场照片.png', status: 'uploading' }
    const { fileList } = upsertFinishedImage(
      [controlledFile],
      callbackFile,
      { fileId: '2103233946292453999', filePath: '/security/photo.png', originalName: '现场照片.png' },
      'blob:preview',
    )

    expect(fileList[0]).toMatchObject({
      status: 'finished',
      fileId: '2103233946292453999',
      originalUrl: '2103233946292453999',
    })
    expect(serializeFinishedImageValue(fileList, 'string')).toBe('2103233946292453999')
    expect(serializeFinishedImageValue(fileList, 'array')).toEqual(['2103233946292453999'])
  })

  it('does not submit uploading files or transient blob urls', () => {
    expect(serializeFinishedImageValue([
      { status: 'uploading', fileId: 'pending-id' },
      { status: 'finished', originalUrl: 'blob:preview' },
    ])).toBe('')
  })
})
