import request from './request'

export function listShoppingItems() {
  return request.get('/shopping-list')
}

export function addShoppingItem(payload) {
  return request.post('/shopping-list/items', payload)
}

export function markPurchased(id, purchased) {
  return request.patch(`/shopping-list/items/${id}/purchased`, { purchased })
}

export function updateShoppingItem(id, payload) {
  return request.put(`/shopping-list/items/${id}`, payload)
}

export function deleteShoppingItem(id) {
  return request.delete(`/shopping-list/items/${id}`)
}
