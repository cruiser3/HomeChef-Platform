import { defineStore } from 'pinia'
import { getCart, addCartItem, updateCartItem, removeCartItem, clearCart } from '../api/cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    cart: null,
    loading: false,
  }),

  getters: {
    items: (state) => state.cart?.items || [],
    itemCount: (state) => state.cart?.itemCount || 0,
    totalAmount: (state) => state.cart?.totalAmount || 0,
    isEmpty: (state) => !state.cart?.items?.length,
  },

  actions: {
    async loadCart() {
      this.loading = true
      try {
        this.cart = await getCart()
      } finally {
        this.loading = false
      }
    },

    async addItem(dishId, quantity = 1) {
      this.cart = await addCartItem({ dishId, quantity })
    },

    async updateItem(itemId, quantity) {
      this.cart = await updateCartItem(itemId, { quantity })
    },

    async removeItem(itemId) {
      this.cart = await removeCartItem(itemId)
    },

    async clear() {
      this.cart = await clearCart()
    },

    reset() {
      this.cart = null
    },
  },
})
