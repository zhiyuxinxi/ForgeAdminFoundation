import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const DynamicFormConfig: ConfigType = {
  key: 'DynamicForm',
  chartKey: 'VDynamicForm',
  conKey: 'VCDynamicForm',
  title: '动态表单',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'input.png'
}
