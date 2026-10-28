export function upsertFinishedImage(fileList = [], file = {}, fileData = {}, displayUrl = '') {
  const id = file.id || fileData.fileId
  const finishedFile = {
    ...file,
    id,
    name: fileData.originalName || file.name,
    fileId: fileData.fileId,
    filePath: fileData.filePath,
    originalUrl: fileData.fileId || fileData.filePath,
    url: displayUrl,
    thumbnailUrl: displayUrl,
    status: 'finished',
    percentage: 100,
    metadata: fileData,
  }
  const nextList = [...fileList]
  const index = nextList.findIndex(item => item.id === id)
  if (index >= 0)
    nextList.splice(index, 1, finishedFile)
  else
    nextList.push(finishedFile)
  return { finishedFile, fileList: nextList }
}

export function serializeFinishedImageValue(fileList = [], valueType = 'string') {
  const values = fileList
    .filter(file => file.status === 'finished')
    .map(file => file.fileId || file.filePath || file.originalUrl)
    .filter(value => value && !(typeof value === 'string' && value.startsWith('blob:')))
  return valueType === 'array' ? values : values.join(',')
}
