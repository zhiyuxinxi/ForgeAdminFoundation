import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const MiniTrendCardConfig: ConfigType = {
  key: 'MiniTrendCard',
  chartKey: 'VMiniTrendCard',
  conKey: 'VCMiniTrendCard',
  title: '迷你趋势卡',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'line_gradient_single.png'
}
