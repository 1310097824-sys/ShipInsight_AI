import { http, unwrap } from '@/api/http'
import type { EntityVersionView, PageResponse, VesselProfileDetailView, VesselProfileImageView, VesselProfileView, VesselTypeCategoryOption } from '@/types/gsmv'

export function fetchVesselProfiles(params: {
  keyword?: string
  status?: number
  riskLevel?: string
  operationalStatus?: string
  routeKeyword?: string
  typeCategoryId?: number
  page: number
  size: number
}) {
  return unwrap<PageResponse<VesselProfileView>>(http.get('/v1/vessel-profiles', { params }))
}

export function fetchVesselProfileDetail(id: number) {
  return unwrap<VesselProfileDetailView>(http.get(`/v1/vessel-profiles/${id}`))
}

export function fetchVesselProfileVersions(id: number) {
  return unwrap<EntityVersionView[]>(http.get(`/v1/vessel-profiles/${id}/versions`))
}

export function fetchVesselTypeCategories() {
  return unwrap<VesselTypeCategoryOption[]>(http.get('/v1/vessel-profiles/taxa'))
}

export function createVesselProfile(payload: Record<string, unknown>) {
  return unwrap<VesselProfileDetailView>(http.post('/v1/vessel-profiles', payload))
}

export function updateVesselProfile(id: number, payload: Record<string, unknown>) {
  return unwrap<VesselProfileDetailView>(http.put(`/v1/vessel-profiles/${id}`, payload))
}

export function rollbackVesselProfileVersion(id: number, versionId: number) {
  return unwrap<VesselProfileDetailView>(http.post(`/v1/vessel-profiles/${id}/versions/${versionId}/rollback`))
}

export function deleteVesselProfile(id: number) {
  return unwrap<void>(http.delete(`/v1/vessel-profiles/${id}`))
}

export async function uploadVesselProfileImage(id: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrap<VesselProfileImageView>(
    http.post(`/v1/vessel-profiles/${id}/images`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
  )
}
