import request from './request'

export function listDishReviews(dishId) {
  return request.get(`/dishes/${dishId}/reviews`)
}

export function createDishReview(dishId, payload) {
  return request.post(`/dishes/${dishId}/reviews`, payload)
}

export function createDishReply(dishId, reviewId, payload) {
  return request.post(`/dishes/${dishId}/reviews/${reviewId}/replies`, payload)
}

export function updateDishReview(dishId, reviewId, payload) {
  return request.patch(`/dishes/${dishId}/reviews/${reviewId}`, payload)
}

export function deleteDishReview(dishId, reviewId) {
  return request.delete(`/dishes/${dishId}/reviews/${reviewId}`)
}
