import { http, unwrap } from '@/api/http'
import type {
  AisBatchOperationPayload,
  AisBatchOperationResult,
  AisConvertedCsvSaveResult,
  AisBatchUpdatePayload,
  AisDatasetDateStat,
  AisImportProgress,
  AisImportResult,
  AisRankingStat,
  AisRecordView,
  AisRiskSummary,
  AisVesselDraftBatchRequest,
  AisVesselDraftBatchResult,
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

export function fetchAisDatasetDateStats(params: {
  keyword?: string
  observedFrom?: string
  observedTo?: string
}) {
  return unwrap<AisDatasetDateStat[]>(http.get('/v1/ais-records/stats/dataset-dates', { params }))
}

export function fetchAisImporterStats(params: {
  keyword?: string
  observedFrom?: string
  observedTo?: string
  limit?: number
}) {
  return unwrap<AisRankingStat[]>(http.get('/v1/ais-records/stats/importers', { params }))
}

export function fetchAisRiskSummary(params: {
  keyword?: string
  observedFrom?: string
  observedTo?: string
}) {
  return unwrap<AisRiskSummary>(http.get('/v1/ais-records/stats/risk-summary', { params }))
}

export function fetchAisVesselTrack(mmsi: string, limit = 5000) {
  return unwrap<PageResponse<AisRecordView>>(http.get(`/v1/ais-records/${encodeURIComponent(mmsi)}/track`, { params: { limit } }))
}

export function fetchAisImportProgress(taskId: string) {
  return unwrap<AisImportProgress>(http.get(`/v1/ais-records/import/progress/${encodeURIComponent(taskId)}`))
}

export function generateAisVesselDrafts(payload: AisVesselDraftBatchRequest) {
  return unwrap<AisVesselDraftBatchResult>(http.post('/v1/ais-records/vessel-drafts', payload, { timeout: 0 }))
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

export function saveConvertedAisCsv(file: File) {
  const form = new FormData()
  form.append('file', file)
  return unwrap<AisConvertedCsvSaveResult>(
    http.post('/v1/ais-records/converted-csv/save', form, {
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
