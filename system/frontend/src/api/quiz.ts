import { http, unwrap } from '@/api/http'
import { ACCESS_TOKEN_KEY } from '@/constants/auth'
import type { PageResponse, QuizQuestion, QuizRecord } from '@/types/gsmv'

// ==================== Question ====================

export function fetchQuestions(params: {
  category?: string
  type?: string
  difficulty?: string
  keyword?: string
  page: number
  size: number
}) {
  return unwrap<PageResponse<QuizQuestion>>(http.get('/v1/quiz/questions', { params }))
}

export function fetchQuestion(id: number) {
  return unwrap<QuizQuestion>(http.get(`/v1/quiz/questions/${id}`))
}

export function createQuestion(payload: Record<string, unknown>) {
  return unwrap<QuizQuestion>(http.post('/v1/quiz/questions', payload))
}

export function updateQuestion(id: number, payload: Record<string, unknown>) {
  return unwrap<QuizQuestion>(http.put(`/v1/quiz/questions/${id}`, payload))
}

export function deleteQuestion(id: number) {
  return unwrap<void>(http.delete(`/v1/quiz/questions/${id}`))
}

export function toggleQuestion(id: number) {
  return unwrap<void>(http.put(`/v1/quiz/questions/${id}/toggle`))
}

export function fetchQuestionStats() {
  return unwrap<{ ship: number; weather: number; seaArea: number }>(
    http.get('/v1/quiz/questions/stats'),
  )
}

// ==================== Exam ====================

export interface QuizExamStartResponse {
  recordId: number
  questions: QuizQuestion[]
}

export interface QuizResultResponse {
  recordId: number
  score: number
  total: number
  grade: string
  details: QuizResultDetail[]
}

export interface QuizResultDetail {
  questionId: number
  title: string
  type: string
  options: string
  correctAnswer: string
  userAnswer: string
  correct: boolean
  explanation: string
}

export function startExam(payload: {
  categories?: string[]
  count?: number
  mode?: string
  difficulty?: string
}) {
  return unwrap<QuizExamStartResponse>(http.post('/v1/quiz/exam/start', payload))
}

export function submitExam(payload: {
  recordId: number
  answers: { questionId: number; userAnswer: string }[]
}) {
  return unwrap<QuizResultResponse>(http.post('/v1/quiz/exam/submit', payload))
}

export function fetchExamResult(recordId: number) {
  return unwrap<QuizResultResponse>(http.get(`/v1/quiz/exam/result/${recordId}`))
}

// ==================== History ====================

export function fetchRecords(params: { userId?: number; page: number; size: number }) {
  return unwrap<PageResponse<QuizRecord>>(http.get('/v1/quiz/records', { params }))
}

// ==================== AI Assistant ====================

export interface QuizAiMessage {
  role: string
  content: string
}

export interface QuizAiChatResponse {
  answer: string
  mode: string
}

export interface QuizAiHistoryItem {
  id: number
  role: string
  content: string
  createdAt: string
}

export interface QuizAiHistoryResponse {
  messages: QuizAiHistoryItem[]
}

export interface QuizAiStreamEvent {
  type: string
  content: string
  response: QuizAiChatResponse | null
}

const QUIZ_AI_TIMEOUT = 90000

export function askQuizAi(payload: { message: string; history?: QuizAiMessage[] }) {
  return unwrap<QuizAiChatResponse>(
    http.post('/v1/quiz/ai/chat', payload, { timeout: QUIZ_AI_TIMEOUT }),
  )
}

export function getQuizAiHistory() {
  return unwrap<QuizAiHistoryResponse>(http.get('/v1/quiz/ai/messages'))
}

export function clearQuizAiHistory() {
  return unwrap<void>(http.delete('/v1/quiz/ai/messages'))
}

// ==================== AI Generate Questions ====================

export interface GeneratedQuestion {
  category: string
  type: string
  title: string
  options: string
  answer: string
  explanation: string
  difficulty: string
}

export interface GenerateQuestionsResponse {
  saved: GeneratedQuestion[]
  duplicates: GeneratedQuestion[]
  totalSaved: number
  totalDuplicates: number
}

export function generateQuizQuestions(payload: {
  category: string
  type: string
  difficulty: string
  count: number
}) {
  return unwrap<GenerateQuestionsResponse>(
    http.post('/v1/quiz/ai/generate', payload, { timeout: 90000 }),
  )
}

// ==================== Weather (态势总览) ====================

export interface WeatherInterpretResponse {
  city: string
  date: string
  datetime: string
  akConfigured: boolean
  error: string | null
  weatherData: string | null
  aiInterpretation: string | null
}

export function fetchWeatherInterpret(city: string = '湛江') {
  return unwrap<WeatherInterpretResponse>(
    http.get('/v1/quiz/ai/weather/interpret', { params: { city } }),
  )
}

export async function askQuizAiStream(
  payload: { message: string; history?: QuizAiMessage[] },
  onEvent: (event: QuizAiStreamEvent) => void,
  signal?: AbortSignal,
) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  const response = await fetch(`${baseUrl}/v1/quiz/ai/chat/stream`, {
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
      return 'AI 助手暂时不可用'
    }
    try {
      const json = JSON.parse(text)
      return json.message || json.error || text
    } catch {
      return text
    }
  } catch {
    return 'AI 助手暂时不可用'
  }
}

function consumeSseBuffer(buffer: string, onEvent: (event: QuizAiStreamEvent) => void) {
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

function parseSseFrame(frame: string): QuizAiStreamEvent | null {
  const dataLines = frame
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())

  if (!dataLines.length) {
    return null
  }

  try {
    return JSON.parse(dataLines.join('\n')) as QuizAiStreamEvent
  } catch {
    return {
      type: 'error',
      content: '流式响应解析失败',
      response: null,
    }
  }
}
