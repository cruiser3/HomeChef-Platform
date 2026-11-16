import request from './request'

export function listFamilies() {
  return request.get('/families')
}

export function createFamily(payload) {
  return request.post('/families', payload)
}

export function getFamily(familyId) {
  return request.get(`/families/${familyId}`)
}

export function listFamilyMembers(familyId) {
  return request.get(`/families/${familyId}/members`)
}

export function getInviteCode(familyId) {
  return request.get(`/families/${familyId}/invite-code`)
}

export function resetInviteCode(familyId) {
  return request.post(`/families/${familyId}/invite-code/reset`)
}

export function joinFamily(inviteCode) {
  return request.post('/families/join', { inviteCode })
}

export function removeMember(familyId, userId) {
  return request.delete(`/families/${familyId}/members/${userId}`)
}

export function updateMemberRole(familyId, userId, role) {
  return request.put(`/families/${familyId}/members/${userId}/role`, { role })
}
