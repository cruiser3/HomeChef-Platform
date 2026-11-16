import request from './request'

export function getMyProfile() {
  return request.get('/users/me')
}

export function updateMyProfile(payload) {
  return request.put('/users/me', payload)
}
