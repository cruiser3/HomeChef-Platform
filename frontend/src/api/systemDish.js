import request from './request'

export function listSystemDishCategories() {
  return request.get('/system-dish-categories')
}

export function listSystemDishes(params = {}) {
  return request.get('/system-dishes', { params })
}

export function getSystemDish(dishId) {
  return request.get(`/system-dishes/${dishId}`)
}

export function copySystemDishToFamily(dishId, payload) {
  return request.post(`/system-dishes/${dishId}/copy-to-family`, payload)
}
