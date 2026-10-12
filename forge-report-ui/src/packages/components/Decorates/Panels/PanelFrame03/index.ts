import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const PanelFrame03Config: ConfigType = {
  key: 'PanelFrame03',
  chartKey: 'VPanelFrame03',
  conKey: 'VCPanelFrame03',
  title: '扫描模块框',
  category: ChatCategoryEnum.PANEL,
  categoryName: ChatCategoryEnumName.PANEL,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'box03.png'
}
