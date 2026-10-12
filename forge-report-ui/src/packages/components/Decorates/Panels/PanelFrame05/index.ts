import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const PanelFrame05Config: ConfigType = {
  key: 'PanelFrame05',
  chartKey: 'VPanelFrame05',
  conKey: 'VCPanelFrame05',
  title: '辉光模块框',
  category: ChatCategoryEnum.PANEL,
  categoryName: ChatCategoryEnumName.PANEL,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'box05.png'
}
