import request from './request'

export function createOrder(payload) {
  return request.post('/family-orders', payload)
}

export function listOrders() {
  return request.get('/family-orders')
}

export function getOrder(orderId) {
  return request.get(`/family-orders/${orderId}`)
}

export function cancelOrder(orderId) {
  return request.post(`/family-orders/${orderId}/cancel`)
}

export function startPreparing(orderId) {
  return request.post(`/family-orders/${orderId}/start-preparing`)
}

export function markMealServed(orderId) {
  return request.post(`/family-orders/${orderId}/meal-served`)
}

export function updateCookStatus(orderItemId, payload) {
  return request.patch(`/family-orders/items/${orderItemId}/cook-status`, payload)
}

export function assignResponsible(orderItemId, payload) {
  return request.patch(`/family-orders/items/${orderItemId}/responsible`, payload)
}

export function deleteOrder(orderId) {
  return request.delete(`/family-orders/${orderId}`)
}
