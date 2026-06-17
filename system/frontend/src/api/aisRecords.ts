import { http, unwrap } from '@/api/http'
import type {
  AisBatchOperationPayload,
  AisBatchOperationResult,
  AisBatchUpdatePayload,
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

export function importAisRecords(file: File, limit = 10) {
  const form = new FormData()
  form.append('file', file)
  form.append('limit', String(limit))
  return unwrap<AisImportResult>(
    http.post('/v1/ais-records/import', form, {
      timeout: 180000,
    }),
  )
}

export function deleteAisRecords(payload: AisBatchOperationPayload) {
  return unwrap<AisBatchOperationResult>(http.delete('/v1/ais-records/batch', { data: payload }))
}

export function updateAisRecords(payload: AisBatchUpdatePayload) {
  return unwrap<AisBatchOperationResult>(http.patch('/v1/ais-records/batch', payload))
}
