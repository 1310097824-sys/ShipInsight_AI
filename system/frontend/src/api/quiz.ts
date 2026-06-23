import { http, unwrap } from '@/api/http'
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
