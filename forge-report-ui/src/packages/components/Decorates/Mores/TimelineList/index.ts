import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const TimelineListConfig: ConfigType = {
  key: 'TimelineList',
  chartKey: 'VTimelineList',
  conKey: 'VCTimelineList',
  title: '时间线列表',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'shijianxian.png'
}
