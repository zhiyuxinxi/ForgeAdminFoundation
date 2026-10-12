import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const CrudTableConfig: ConfigType = {
  key: 'CrudTable',
  chartKey: 'VCrudTable',
  conKey: 'VCCrudTable',
  title: 'CRUD 查询表格',
  category: ChatCategoryEnum.TABLE,
  categoryName: ChatCategoryEnumName.TABLE,
  package: PackagesCategoryEnum.TABLES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'tables_basic.png'
}
