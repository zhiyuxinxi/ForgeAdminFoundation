import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const FilterBarConfig: ConfigType = {
  key: 'FilterBar',
  chartKey: 'VFilterBar',
  conKey: 'VCFilterBar',
  title: '联动筛选器',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'inputs_select.png'
}
