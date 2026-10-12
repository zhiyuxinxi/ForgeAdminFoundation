import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const MetricCompareCardConfig: ConfigType = {
  key: 'MetricCompareCard',
  chartKey: 'VMetricCompareCard',
  conKey: 'VCMetricCompareCard',
  title: '对比指标卡',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'dbzb.png'
}
