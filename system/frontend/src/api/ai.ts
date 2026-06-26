import { http, unwrap } from '@/api/http'
import { ACCESS_TOKEN_KEY, PROFILE_KEY } from '@/constants/auth'
import type {
  AiAssistantChatResponse,
  AiAssistantHistoryResponse,
  AiAssistantMessage,
  AiAssistantStreamEvent,
  AiIdentifyImageResponse,
  AiRecordAnalysisResponse,
  AiRecordQualityResponse,
  AiRecordVesselItem,
  AiPolishTextResponse,
  AiVesselAutocompleteResponse,
  AiTranslateVesselResponse,
  AisRecordEnvironment,
} from '@/types/gsmv'

const AI_REQUEST_TIMEOUT = 90000

export async function identifyVesselByImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrap<AiIdentifyImageResponse>(
    http.post('/v1/ai/vessels/identify', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: AI_REQUEST_TIMEOUT,
    }),
  )
}

export function autocompleteVesselProfile(payload: {
  chineseName?: string
  scientificName?: string
  description?: string
  morphology?: string
  habit?: string
  habitat?: string
  distribution?: string
  geoRangeText?: string
}) {
  return unwrap<AiVesselAutocompleteResponse>(
    http.post('/v1/ai/vessels/autocomplete', payload, { timeout: AI_REQUEST_TIMEOUT }),
  )
}

export function polishVesselText(payload: {
  fieldName: string
  text: string
}) {
  return unwrap<AiPolishTextResponse>(http.post('/v1/ai/vessels/polish', payload, { timeout: AI_REQUEST_TIMEOUT }))
}

export function translateVesselProfile(payload: {
  chineseName?: string
  scientificName?: string
  description?: string
  morphology?: string
  habit?: string
  habitat?: string
  distribution?: string
  geoRangeText?: string
  targetLanguage: string
}) {
  return unwrap<AiTranslateVesselResponse>(
    http.post('/v1/ai/vessels/translate', payload, { timeout: AI_REQUEST_TIMEOUT }),
  )
}

export function analyzeRecordWithAi(payload: {
  shippingZoneId?: number
  shippingZoneName: string
  observedAt: string
  locationLat: number
  locationLng: number
  locationName?: string
  note?: string
  environment: AisRecordEnvironment
  vesselItems: AiRecordVesselItem[]
}) {
  return unwrap<AiRecordAnalysisResponse>(
    http.post('/v1/ai/records/analyze', payload, { timeout: AI_REQUEST_TIMEOUT }),
  )
}

export function qualityCheckRecordWithAi(id: number) {
  return unwrap<AiRecordQualityResponse>(
    http.post(`/v1/ai/records/${id}/quality-check`, undefined, { timeout: AI_REQUEST_TIMEOUT }),
  )
}

export function askAiAssistant(payload: {
  message: string
  history?: AiAssistantMessage[]
}) {
  return unwrap<AiAssistantChatResponse>(http.post('/v1/ai/assistant/chat', payload, { timeout: 60000 }))
}

export function getAiAssistantHistory() {
  return unwrap<AiAssistantHistoryResponse>(http.get('/v1/ai/assistant/messages'))
}

export function clearAiAssistantHistory() {
  return unwrap<void>(http.delete('/v1/ai/assistant/messages'))
}

export async function askAiAssistantStream(
  payload: {
    message: string
    history?: AiAssistantMessage[]
  },
  onEvent: (event: AiAssistantStreamEvent) => void,
  signal?: AbortSignal,
) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  const response = await fetch(`${baseUrl}/v1/ai/assistant/chat/stream`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
    signal,
  })

  if (response.status === 401) {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(PROFILE_KEY)
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    throw new Error('请先登录')
  }

  if (!response.ok) {
    throw new Error(await readStreamError(response))
  }

  if (!response.body) {
    throw new Error('浏览器不支持流式读取')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    buffer = consumeSseBuffer(buffer, onEvent)
  }

  buffer += decoder.decode()
  consumeSseBuffer(`${buffer}\n\n`, onEvent)
}

async function readStreamError(response: Response) {
  try {
    const text = await response.text()
    if (!text.trim()) {
      return '智能分析服务暂时不可用'
    }
    try {
      const json = JSON.parse(text)
      return json.message || json.error || text
    } catch {
      return text
    }
  } catch {
    return '智能分析服务暂时不可用'
  }
}

function consumeSseBuffer(buffer: string, onEvent: (event: AiAssistantStreamEvent) => void) {
  const normalized = buffer.replace(/\r\n/g, '\n')
  const frames = normalized.split('\n\n')
  const rest = frames.pop() ?? ''

  for (const frame of frames) {
    const event = parseSseFrame(frame)
    if (event) {
      onEvent(event)
    }
  }

  return rest
}

function parseSseFrame(frame: string): AiAssistantStreamEvent | null {
  const dataLines = frame
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())

  if (!dataLines.length) {
    return null
  }

  try {
    return JSON.parse(dataLines.join('\n')) as AiAssistantStreamEvent
  } catch {
    return {
      type: 'error',
      content: '流式响应解析失败',
    }
  }
}
