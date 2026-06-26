const GPXTPX_NAMESPACE = 'http://www.garmin.com/xmlschemas/TrackPointExtension/v1'
const MS_TO_KNOTS = 1.9438444924406046
const DEFAULT_TIMEZONE = 'Asia/Shanghai'

const OUTPUT_HEADERS = [
  'MMSI',
  'BaseDateTime',
  'LAT',
  'LON',
  'SOG',
  'COG',
  'Heading',
  'VesselName',
  'IMO',
  'CallSign',
  'VesselType',
  'Status',
  'Length',
  'Width',
  'Draft',
  'Cargo',
  'Transceiver',
  'SourceFile',
]

export interface GpxToAisCsvOptions {
  mmsi: string
  vesselName: string
  imo?: string
  callSign?: string
  transceiver?: string
  timeZone?: string
  includeNonGps?: boolean
}

export interface GpxConversionResult {
  blob: Blob
  fileName: string
  rowCount: number
}

interface ParsedTrackPoint {
  baseDateTime: string
  latitude: string
  longitude: string
  sog: string
  cog: string
  heading: string
}

export async function convertGpxFileToAisCsv(file: File, options: GpxToAisCsvOptions): Promise<GpxConversionResult> {
  const xmlText = normalizeGpxXml(await file.text())
  const xml = new DOMParser().parseFromString(xmlText, 'application/xml')
  const parserError = xml.querySelector('parsererror')
  if (parserError) {
    throw new Error('GPX 文件解析失败，请确认文件格式正确。')
  }

  const points = extractTrackPoints(xml, options)
  if (!points.length) {
    throw new Error('未在 GPX 中找到可转换的轨迹点。')
  }

  const csvBody = [
    OUTPUT_HEADERS.join(','),
    ...points.map((point) =>
      [
        options.mmsi.trim(),
        point.baseDateTime,
        point.latitude,
        point.longitude,
        point.sog,
        point.cog,
        point.heading,
        options.vesselName.trim(),
        cleanText(options.imo),
        cleanText(options.callSign),
        '',
        '',
        '',
        '',
        '',
        '',
        cleanText(options.transceiver) || 'GPSLogger-Mobile',
        file.name,
      ]
        .map(escapeCsvValue)
        .join(','),
    ),
  ].join('\r\n')

  return {
    blob: new Blob([csvBody], { type: 'text/csv;charset=utf-8' }),
    fileName: `${stripExtension(file.name)}_ais.csv`,
    rowCount: points.length,
  }
}

function normalizeGpxXml(text: string) {
  if (text.includes('gpxtpx:') && !text.includes('xmlns:gpxtpx=')) {
    return text.replace('<gpx', `<gpx xmlns:gpxtpx="${GPXTPX_NAMESPACE}"`)
  }
  return text
}

function extractTrackPoints(xml: XMLDocument, options: GpxToAisCsvOptions) {
  const points = Array.from(xml.getElementsByTagName('*')).filter(
    (element): element is Element => element instanceof Element && element.localName === 'trkpt',
  )

  return points
    .map((point) => parseTrackPoint(point, options))
    .filter((point): point is ParsedTrackPoint => point !== null)
}

function parseTrackPoint(point: Element, options: GpxToAisCsvOptions): ParsedTrackPoint | null {
  const latitude = Number.parseFloat(point.getAttribute('lat') || '')
  const longitude = Number.parseFloat(point.getAttribute('lon') || '')
  const observedAt = getChildText(point, 'time')
  const source = (getChildText(point, 'src') || '').trim().toLowerCase()

  if (!Number.isFinite(latitude) || !Number.isFinite(longitude) || !observedAt) {
    return null
  }
  if (!options.includeNonGps && source && source !== 'gps') {
    return null
  }

  const bearing = parseFloatOrNull(getDescendantText(point, 'bearing'))
  const speedMs = parseFloatOrNull(getDescendantText(point, 'speed'))

  return {
    baseDateTime: formatGpxTimestamp(observedAt, options.timeZone || DEFAULT_TIMEZONE),
    latitude: formatNumber(latitude, 8),
    longitude: formatNumber(longitude, 8),
    sog: speedMs == null ? '' : formatNumber(speedMs * MS_TO_KNOTS, 2),
    cog: bearing == null ? '' : formatNumber(normalizeBearing(bearing), 1),
    heading: bearing == null ? '' : String(Math.round(normalizeBearing(bearing))),
  }
}

function getChildText(element: Element, name: string) {
  const child = Array.from(element.children).find((entry) => entry.localName === name)
  return child?.textContent?.trim() || ''
}

function getDescendantText(element: Element, name: string) {
  const descendant = Array.from(element.getElementsByTagName('*')).find((entry) => entry.localName === name)
  return descendant?.textContent?.trim() || ''
}

function formatGpxTimestamp(value: string, timeZone: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    throw new Error(`无效的轨迹时间：${value}`)
  }

  try {
    const formatter = new Intl.DateTimeFormat('zh-CN', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
    const parts = formatter.formatToParts(date)
    const byType = Object.fromEntries(parts.map((part) => [part.type, part.value]))
    return `${byType.year}-${byType.month}-${byType.day} ${byType.hour}:${byType.minute}:${byType.second}`
  } catch {
    return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())} ${padNumber(date.getHours())}:${padNumber(date.getMinutes())}:${padNumber(date.getSeconds())}`
  }
}

function normalizeBearing(value: number) {
  return ((value % 360) + 360) % 360
}

function parseFloatOrNull(value: string) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : null
}

function formatNumber(value: number, digits: number) {
  const fixed = value.toFixed(digits)
  return fixed.includes('.') ? fixed.replace(/\.?0+$/, '') : fixed
}

function escapeCsvValue(value: string) {
  if (!/[",\r\n]/.test(value)) {
    return value
  }
  return `"${value.replaceAll('"', '""')}"`
}

function stripExtension(name: string) {
  return name.replace(/\.[^.]+$/, '')
}

function cleanText(value?: string) {
  return value?.trim() || ''
}

function padNumber(value: number) {
  return String(value).padStart(2, '0')
}
