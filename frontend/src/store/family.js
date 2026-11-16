import { defineStore } from 'pinia'
import { listFamilies, createFamily as createFamilyApi, joinFamily as joinFamilyApi } from '../api/family'
import { FAMILY_ID_KEY } from '../api/request'

function readStoredFamilyId() {
  const raw = localStorage.getItem(FAMILY_ID_KEY)
  const id = raw ? Number(raw) : null
  return Number.isFinite(id) && id > 0 ? id : null
}

export const useFamilyStore = defineStore('family', {
  state: () => ({
    families: [],
    currentFamilyId: readStoredFamilyId(),
    loaded: false,
  }),

  getters: {
    currentFamily: (state) => state.families.find((f) => f.id === state.currentFamilyId) || null,
    hasFamilies: (state) => state.families.length > 0,
  },

  actions: {
    async loadFamilies() {
      this.families = await listFamilies()
      this.loaded = true
      if (this.currentFamilyId && !this.families.some((f) => f.id === this.currentFamilyId)) {
        this.currentFamilyId = null
        localStorage.removeItem(FAMILY_ID_KEY)
      }
      return this.families
    },

    selectFamily(familyId) {
      this.currentFamilyId = familyId
      localStorage.setItem(FAMILY_ID_KEY, String(familyId))
    },

    async createFamily(name) {
      const family = await createFamilyApi({ name })
      await this.loadFamilies()
      this.selectFamily(family.id)
      return family
    },

    async joinFamily(inviteCode) {
      const family = await joinFamilyApi(inviteCode)
      await this.loadFamilies()
      this.selectFamily(family.id)
      return family
    },

    clearFamily() {
      this.families = []
      this.currentFamilyId = null
      this.loaded = false
      localStorage.removeItem(FAMILY_ID_KEY)
    },
  },
})
