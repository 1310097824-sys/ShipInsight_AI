import { http, unwrap } from '@/api/http'
import type {
  AisRecordMapPoint,
  DashboardSummary,
  NameValuePoint,
  ShippingZoneStats,
  VesselDistributionPoint,
} from '@/types/gsmv'

export function fetchDashboardSummary() {
  return unwrap<DashboardSummary>(http.get('/v1/reports/summary'))
}

export function fetchRiskLevelDistribution() {
  return unwrap<NameValuePoint[]>(http.get('/v1/reports/risk-level'))
}

export function fetchOperationalStatusDistribution() {
  return unwrap<NameValuePoint[]>(http.get('/v1/reports/operational-status'))
}

export function fetchVesselTypeDistribution(level: string) {
  return unwrap<NameValuePoint[]>(http.get('/v1/reports/vessel-type-distribution', { params: { level } }))
}

export function fetchAisRecordTrend(days = 30) {
  return unwrap<NameValuePoint[]>(http.get('/v1/reports/ais-record-trend', { params: { days } }))
}

export function fetchAisRecordActivity(days = 30) {
  return unwrap<NameValuePoint[]>(http.get('/v1/reports/ais-record-activity', { params: { days } }))
}

export function fetchShippingZoneStats() {
  return unwrap<ShippingZoneStats[]>(http.get('/v1/reports/shipping-zone-stats'))
}

export function fetchVesselDistributionPoints() {
  return unwrap<VesselDistributionPoint[]>(http.get('/v1/reports/vessel-distribution'))
}

export function fetchAisRecordMapPoints() {
  return unwrap<AisRecordMapPoint[]>(http.get('/v1/reports/ais-record-map'))
}

export async function downloadReportExport(format: 'excel' | 'pdf', days = 30) {
  const response = await http.get(`/v1/reports/export/${format}`, {
    params: { days },
    responseType: 'blob',
  })

  return {
    blob: response.data as Blob,
    fileName: readFileName(response.headers['content-disposition']) || `shipinsight-report.${format === 'excel' ? 'xlsx' : 'pdf'}`,
  }
}

function readFileName(contentDisposition?: string) {
  if (!contentDisposition) {
    return ''
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
  return plainMatch?.[1] || ''
}
