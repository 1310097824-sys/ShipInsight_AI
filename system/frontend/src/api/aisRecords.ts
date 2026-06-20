import { http, unwrap } from '@/api/http'
import type {
  AisBatchOperationPayload,
  AisBatchOperationResult,
  AisBatchUpdatePayload,
  AisImportProgress,
  AisImportResult,
  AisRecordView,
  PageResponse,
} from '@/types/gsmv'

export function fetchAisRecords(params: {
  keyword?: string
  observedFrom?: string
  observedTo?: string
  page: number
  size: number
}) {
  return unwrap<PageResponse<AisRecordView>>(http.get('/v1/ais-records', { params }))
}

export function fetchAisMapRecords(params: {
  keyword?: string
  datasetDate?: string
  observedFrom?: string
  observedTo?: string
  limit?: number
}) {
  return unwrap<PageResponse<AisRecordView>>(http.get('/v1/ais-records/map', { params }))
}

export function fetchAisDatasetDates() {
  return unwrap<string[]>(http.get('/v1/ais-records/dataset-dates'))
}

export function fetchAisVesselTrack(mmsi: string, limit = 5000) {
  return unwrap<PageResponse<AisRecordView>>(http.get(`/v1/ais-records/${encodeURIComponent(mmsi)}/track`, { params: { limit } }))
}

export function fetchAisImportProgress(taskId: string) {
  return unwrap<AisImportProgress>(http.get(`/v1/ais-records/import/progress/${encodeURIComponent(taskId)}`))
}

export function importAisRecords(file: File, limit?: number | null, taskId?: string) {
  const form = new FormData()
  form.append('file', file)
  if (typeof limit === 'number') {
    form.append('limit', String(limit))
  }
  if (taskId) {
    form.append('taskId', taskId)
  }
  return unwrap<AisImportResult>(
    http.post('/v1/ais-records/import', form, {
      timeout: 0,
    }),
  )
}

export function deleteAisRecords(payload: AisBatchOperationPayload) {
  return unwrap<AisBatchOperationResult>(http.delete('/v1/ais-records/batch', { data: payload }))
}

export function updateAisRecords(payload: AisBatchUpdatePayload) {
  return unwrap<AisBatchOperationResult>(http.patch('/v1/ais-records/batch', payload))
}
