import { http, unwrap } from '@/api/http'
import type { Ecosystem, PageResponse, ShippingZone } from '@/types/gsmv'

export function fetchShippingZones(params: { keyword?: string; type?: string; page: number; size: number }) {
  return unwrap<PageResponse<ShippingZone>>(http.get('/v1/shipping-zones', { params }))
}

export function fetchAllShippingZones() {
  return unwrap<ShippingZone[]>(http.get('/v1/shipping-zones/all'))
}

export async function fetchAllEcosystems() {
  const zones = await fetchAllShippingZones()
  return zones.map((zone): Ecosystem => ({ ...zone }))
}

export function createShippingZone(payload: Record<string, unknown>) {
  return unwrap<ShippingZone>(http.post('/v1/shipping-zones', payload))
}

export function updateShippingZone(id: number, payload: Record<string, unknown>) {
  return unwrap<ShippingZone>(http.put(`/v1/shipping-zones/${id}`, payload))
}

export function deleteShippingZone(id: number) {
  return unwrap<void>(http.delete(`/v1/shipping-zones/${id}`))
}
