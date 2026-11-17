import request from './request'

export function listDishes(params = {}) {
  return request.get('/dishes', { params })
}

export function getDish(dishId) {
  return request.get(`/dishes/${dishId}`)
}

export function createDish(payload) {
  return request.post('/dishes', payload)
}

export function updateDish(dishId, payload) {
  return request.put(`/dishes/${dishId}`, payload)
}

export function changeDishAvailability(dishId, available) {
  return request.patch(`/dishes/${dishId}/availability`, null, { params: { available } })
}

export function listDishCategories() {
  return request.get('/dishes/categories')
}

export function createDishCategory(payload) {
  return request.post('/dishes/categories', payload)
}

export function updateDishCategory(categoryId, payload) {
  return request.put(`/dishes/categories/${categoryId}`, payload)
}

export function deleteDishCategory(categoryId) {
  return request.delete(`/dishes/categories/${categoryId}`)
}

export function deleteDish(dishId) {
  return request.delete(`/dishes/${dishId}`)
}

export function publishDishToSquare(dishId, categoryId) {
  return request.post(`/dishes/${dishId}/publish-to-square`, null, { params: { categoryId } })
}
