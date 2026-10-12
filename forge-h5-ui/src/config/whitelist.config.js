// 路由白名单：不需要登录即可访问的页面
export const WHITE_LIST = [
  '/login',
  '/404',
]

export function isInWhiteList(path) {
  return WHITE_LIST.includes(path)
}
