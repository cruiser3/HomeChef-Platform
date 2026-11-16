import request from './request'

export function getCart() {
  return request.get('/cart')
}

export function addCartItem(payload) {
  return request.post('/cart/items', payload)
}

export function updateCartItem(itemId, payload) {
  return request.patch(`/cart/items/${itemId}`, payload)
}

export function removeCartItem(itemId) {
  return request.delete(`/cart/items/${itemId}`)
}

export function clearCart() {
  return request.delete('/cart')
}
