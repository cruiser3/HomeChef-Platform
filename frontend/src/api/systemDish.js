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

export function setSystemDishCuisine(dishId, cuisineId) {
  const params = cuisineId != null ? { cuisineId } : {}
  return request.patch(`/system-dishes/${dishId}/cuisine`, null, { params })
}

export function setSystemDishAvailability(dishId, available) {
  return request.patch(`/system-dishes/${dishId}/availability`, null, { params: { available } })
}

export function deleteSystemDish(dishId) {
  return request.delete(`/system-dishes/${dishId}`)
}

export function listSystemDishReviews(dishId) {
  return request.get(`/system-dishes/${dishId}/reviews`)
}

export function createSystemDishReview(dishId, payload) {
  return request.post(`/system-dishes/${dishId}/reviews`, payload)
}

export function createSystemDishReply(dishId, reviewId, payload) {
  return request.post(`/system-dishes/${dishId}/reviews/${reviewId}/replies`, payload)
}

export function updateSystemDishReview(dishId, reviewId, payload) {
  return request.patch(`/system-dishes/${dishId}/reviews/${reviewId}`, payload)
}

export function deleteSystemDishReview(dishId, reviewId) {
  return request.delete(`/system-dishes/${dishId}/reviews/${reviewId}`)
}
