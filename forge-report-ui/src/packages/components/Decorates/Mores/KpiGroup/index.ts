import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const KpiGroupConfig: ConfigType = {
  key: 'KpiGroup',
  chartKey: 'VKpiGroup',
  conKey: 'VCKpiGroup',
  title: '指标组',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'zhibk.png'
}
