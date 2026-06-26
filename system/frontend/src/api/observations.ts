import { http, unwrap } from '@/api/http'
import type {
  AisRecordManualDetailView,
  AisRecordManualView,
  EntityVersionView,
  ObservationDetailView,
  ObservationView,
  PageResponse,
} from '@/types/gsmv'

export function fetchManualRecords(params: {
  shippingZoneId?: number
  keyword?: string
  observedFrom?: string
  observedTo?: string
  page: number
  size: number
}) {
  return unwrap<PageResponse<AisRecordManualView>>(http.get('/v1/ais-records-manual', { params }))
}

export function fetchManualRecordDetail(id: number) {
  return unwrap<AisRecordManualDetailView>(http.get(`/v1/ais-records-manual/${id}`))
}

export async function fetchObservations(params: {
  ecosystemId?: number
  keyword?: string
  observedFrom?: string
  observedTo?: string
  page: number
  size: number
}) {
  const pageData = await fetchManualRecords({
    shippingZoneId: params.ecosystemId,
    keyword: params.keyword,
    observedFrom: params.observedFrom,
    observedTo: params.observedTo,
    page: params.page,
    size: params.size,
  })
  return {
    ...pageData,
    items: pageData.items.map(toObservationView),
  } satisfies PageResponse<ObservationView>
}

export async function fetchObservationDetail(id: number) {
  return toObservationDetailView(await fetchManualRecordDetail(id))
}

export function fetchManualRecordVersions(id: number) {
  return unwrap<EntityVersionView[]>(http.get(`/v1/ais-records-manual/${id}/versions`))
}

export function createManualRecord(payload: Record<string, unknown>) {
  return unwrap<AisRecordManualDetailView>(http.post('/v1/ais-records-manual', payload))
}

export function updateManualRecord(id: number, payload: Record<string, unknown>) {
  return unwrap<AisRecordManualDetailView>(http.put(`/v1/ais-records-manual/${id}`, payload))
}

export function rollbackManualRecordVersion(id: number, versionId: number) {
  return unwrap<AisRecordManualDetailView>(http.post(`/v1/ais-records-manual/${id}/versions/${versionId}/rollback`))
}

export function deleteManualRecord(id: number) {
  return unwrap<void>(http.delete(`/v1/ais-records-manual/${id}`))
}

function toObservationView(record: AisRecordManualView): ObservationView {
  return {
    ...record,
    ecosystemId: record.shippingZoneId,
    ecosystemName: record.shippingZoneName,
    observerUserId: record.recorderUserId,
    observerName: record.recorderName,
    observedAt: record.recordedAt,
    envJson: record.environmentJson,
  }
}

function toObservationDetailView(record: AisRecordManualDetailView): ObservationDetailView {
  return {
    ...toObservationView(record),
    speciesItems: record.linkedVessels.map((item) => ({
      vesselId: item.vesselId,
      scientificName: item.displayName,
      chineseName: item.vesselName,
      countEstimated: item.countEstimated,
      behavior: item.behavior,
      comment: item.comment,
    })),
  }
}
