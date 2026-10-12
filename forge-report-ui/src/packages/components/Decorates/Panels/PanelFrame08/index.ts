import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const PanelFrame08Config: ConfigType = {
  key: 'PanelFrame08',
  chartKey: 'VPanelFrame08',
  conKey: 'VCPanelFrame08',
  title: '卡片模块框',
  category: ChatCategoryEnum.PANEL,
  categoryName: ChatCategoryEnumName.PANEL,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'box08.png'
}
