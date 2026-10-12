import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const PanelFrame02Config: ConfigType = {
  key: 'PanelFrame02',
  chartKey: 'VPanelFrame02',
  conKey: 'VCPanelFrame02',
  title: '棱角模块框',
  category: ChatCategoryEnum.PANEL,
  categoryName: ChatCategoryEnumName.PANEL,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'box02.png'
}
