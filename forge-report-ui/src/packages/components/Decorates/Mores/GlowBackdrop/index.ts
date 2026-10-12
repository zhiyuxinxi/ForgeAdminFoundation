import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const GlowBackdropConfig: ConfigType = {
  key: 'GlowBackdrop',
  chartKey: 'VGlowBackdrop',
  conKey: 'VCGlowBackdrop',
  title: '发光背景',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'fag.png'
}
