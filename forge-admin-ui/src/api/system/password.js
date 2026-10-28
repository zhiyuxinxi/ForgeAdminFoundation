import { request } from '@/utils'

export function changePassword(oldPassword, newPassword) {
  return request.post('/auth/changePassword', { oldPassword, newPassword })
}

export function resetUserPassword(id, password) {
  return request.post('/system/user/resetPwd', { id, password })
}
