import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const KpiCardConfig: ConfigType = {
  key: 'KpiCard',
  chartKey: 'VKpiCard',
  conKey: 'VCKpiCard',
  title: '指标卡',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'zhibk.png'
}
