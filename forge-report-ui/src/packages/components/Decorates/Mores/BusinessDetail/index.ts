import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const BusinessDetailConfig: ConfigType = {
  key: 'BusinessDetail',
  chartKey: 'VBusinessDetail',
  conKey: 'VCBusinessDetail',
  title: '业务详情',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'label.png'
}
