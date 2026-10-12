import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ProgressRingConfig: ConfigType = {
  key: 'ProgressRing',
  chartKey: 'VProgressRing',
  conKey: 'VCProgressRing',
  title: '环形进度',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'process.png'
}
