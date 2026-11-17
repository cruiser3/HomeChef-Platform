import request from './request'

export function listCuisines() {
  return request.get('/cuisines')
}

export function createCuisine(payload) {
  return request.post('/cuisines', payload)
}
