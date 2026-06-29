import { http, unwrap } from '@/api/http'
import type { PageResponse, RoleDetail, PermissionOption } from '@/types/gsmv'

export function fetchRoles(params: { keyword?: string; page?: number; size?: number } = {}) {
  return unwrap<PageResponse<RoleDetail>>(http.get('/v1/roles', { params }))
}

export function fetchRole(id: number) {
  return unwrap<RoleDetail>(http.get(`/v1/roles/${id}`))
}

export function fetchPermissions() {
  return unwrap<PermissionOption[]>(http.get('/v1/roles/permissions'))
}

export function createRole(payload: { code: string; name: string; description?: string; permissionIds?: number[] }) {
  return unwrap<RoleDetail>(http.post('/v1/roles', payload))
}

export function updateRole(id: number, payload: { name: string; description?: string; permissionIds?: number[] }) {
  return unwrap<RoleDetail>(http.put(`/v1/roles/${id}`, payload))
}

export function deleteRole(id: number) {
  return unwrap<void>(http.delete(`/v1/roles/${id}`))
}
