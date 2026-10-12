import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const DataPairListConfig: ConfigType = {
  key: 'DataPairList',
  chartKey: 'VDataPairList',
  conKey: 'VCDataPairList',
  title: '键值信息列表',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'jianzhi.png'
}
