import { beforeEach, describe, expect, it, vi } from 'vitest'

import h5Api from '../../../../../forge-h5-ui/src/api/index.js'
import { changePassword, resetUserPassword } from '../password'

const requestMocks = vi.hoisted(() => {
  const request = vi.fn()
  request.post = vi.fn()
  return request
})

vi.mock('@/utils', () => ({ request: requestMocks }))

describe('password API transport', () => {
  beforeEach(() => {
    requestMocks.mockReset()
    requestMocks.post.mockReset()
  })

  it('sends current-user password values only in the JSON body', () => {
    changePassword('old-value', 'new-value')

    expect(requestMocks.post).toHaveBeenCalledWith('/auth/changePassword', {
      oldPassword: 'old-value',
      newPassword: 'new-value',
    })
    expect(requestMocks.post.mock.calls[0]).toHaveLength(2)
  })

  it('sends administrator reset values only in the JSON body', () => {
    resetUserPassword('76', 'replacement-value')

    expect(requestMocks.post).toHaveBeenCalledWith('/system/user/resetPwd', {
      id: '76',
      password: 'replacement-value',
    })
    expect(requestMocks.post.mock.calls[0]).toHaveLength(2)
  })

  it('sends H5 password values only in the JSON body', () => {
    h5Api.changePassword({ oldPassword: 'old-value', newPassword: 'new-value' })

    expect(requestMocks).toHaveBeenCalledWith({
      url: '/auth/changePassword',
      method: 'post',
      data: {
        oldPassword: 'old-value',
        newPassword: 'new-value',
      },
    })
  })
})
