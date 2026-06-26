import { http, unwrap } from '@/api/http'
import type {
  AisRecordView,
  AisVesselSummaryView,
  EntityVersionView,
  PageResponse,
  VesselDetailView,
  VesselImageView,
  VesselSavePayload,
  VesselTypeOption,
  VesselView,
} from '@/types/gsmv'

export function fetchVessels(params: {
  keyword?: string
  status?: number
  typeId?: number
  riskLevel?: string
  navigationStatus?: string
  routeKeyword?: string
  page: number
  size: number
}) {
  return unwrap<PageResponse<VesselView>>(http.get('/v1/vessels', { params }))
}

export function fetchVesselDetail(id: number) {
  return unwrap<VesselDetailView>(http.get(`/v1/vessels/${id}`))
}

export function fetchVesselVersions(id: number) {
  return unwrap<EntityVersionView[]>(http.get(`/v1/vessels/${id}/versions`))
}

export function fetchVesselAisSummary(id: number) {
  return unwrap<AisVesselSummaryView>(http.get(`/v1/vessels/${id}/ais-summary`))
}

export function fetchVesselAisRecords(id: number, params: { page: number; size: number }) {
  return unwrap<PageResponse<AisRecordView>>(http.get(`/v1/vessels/${id}/ais-records`, { params }))
}

export function fetchVesselTypes() {
  return unwrap<VesselTypeOption[]>(http.get('/v1/vessels/types'))
}

export function createVessel(payload: VesselSavePayload) {
  return unwrap<VesselDetailView>(http.post('/v1/vessels', payload))
}

export function updateVessel(id: number, payload: VesselSavePayload) {
  return unwrap<VesselDetailView>(http.put(`/v1/vessels/${id}`, payload))
}

export function rollbackVesselVersion(id: number, versionId: number) {
  return unwrap<VesselDetailView>(http.post(`/v1/vessels/${id}/versions/${versionId}/rollback`))
}

export function archiveVessel(id: number) {
  return unwrap<void>(http.delete(`/v1/vessels/${id}`))
}

export async function uploadVesselImage(id: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrap<VesselImageView>(
    http.post(`/v1/vessels/${id}/images`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
  )
}
