import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const PanelFrame06Config: ConfigType = {
  key: 'PanelFrame06',
  chartKey: 'VPanelFrame06',
  conKey: 'VCPanelFrame06',
  title: '重角模块框',
  category: ChatCategoryEnum.PANEL,
  categoryName: ChatCategoryEnumName.PANEL,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'box06.png'
}
