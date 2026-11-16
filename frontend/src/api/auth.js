import request from './request'

export function register(payload) {
  return request.post('/auth/register', payload)
}

export function login(payload) {
  return request.post('/auth/login', payload)
}
