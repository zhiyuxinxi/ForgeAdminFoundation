import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const SectionHeaderConfig: ConfigType = {
  key: 'SectionHeader',
  chartKey: 'VSectionHeader',
  conKey: 'VCSectionHeader',
  title: '模块标题条',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'title01.png'
}
