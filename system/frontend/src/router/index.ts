import { createRouter, createWebHistory } from 'vue-router'
import { ACCESS_TOKEN_KEY, PROFILE_KEY } from '@/constants/auth'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    authority?: string
    role?: string
  }
}

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/register', component: () => import('@/views/RegisterView.vue'), meta: { public: true } },
  { path: '/marine-traffic', component: () => import('@/views/MarineTrafficMapView.vue'), meta: { authority: 'OBS_READ' } },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { authority: 'REPORT_READ' } },
      { path: 'vessels', component: () => import('@/views/VesselsView.vue'), meta: { authority: 'VESSEL_READ' } },
      { path: 'vessels/:id', component: () => import('@/views/VesselDetailView.vue'), meta: { authority: 'VESSEL_READ' } },
      { path: 'species', redirect: '/vessels' },
      { path: 'species/:id', redirect: '/vessels' },
      { path: 'ecosystems', redirect: '/dashboard' },
      { path: 'route-map', component: () => import('@/views/RouteMapView.vue'), meta: { authority: 'OBS_READ' } },
      { path: 'observations', component: () => import('@/views/ObservationView.vue'), meta: { authority: 'OBS_READ' } },
      { path: 'assistant', component: () => import('@/views/AiAssistantView.vue') },
      { path: 'ai-reviews', component: () => import('@/views/AiReviewTicketsView.vue'), meta: { authority: 'AI_REVIEW_READ' } },
      { path: 'ai-reports', component: () => import('@/views/AiReportsView.vue'), meta: { authority: 'REPORT_READ' } },
      { path: 'rag-knowledge', component: () => import('@/views/RagKnowledgeView.vue'), meta: { authority: 'RAG_READ' } },
      { path: 'reports', component: () => import('@/views/ReportsView.vue'), meta: { authority: 'REPORT_READ' } },
      { path: 'audits', component: () => import('@/views/AuditView.vue'), meta: { authority: 'AUDIT_READ' } },
      { path: 'users', component: () => import('@/views/UsersView.vue'), meta: { authority: 'USER_ADMIN' } },
      { path: 'roles', component: () => import('@/views/RoleView.vue'), meta: { role: 'ADMIN' } },
      { path: 'profile', component: () => import('@/views/ProfileView.vue') },
      { path: 'quiz', component: () => import('@/views/QuizHome.vue'), meta: { authority: 'QUIZ_READ' } },
      { path: 'quiz/ai', component: () => import('@/views/QuizAiAssistant.vue') },
      { path: 'quiz/exam', component: () => import('@/views/QuizExam.vue'), meta: { authority: 'QUIZ_READ' } },
      { path: 'quiz/result', component: () => import('@/views/QuizResult.vue'), meta: { authority: 'QUIZ_READ' } },
      { path: 'quiz/manage', component: () => import('@/views/QuizManage.vue'), meta: { authority: 'QUIZ_WRITE' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (to.meta.public) {
    if (to.path === '/login' && to.query.fresh === '1') {
      return true
    }
    if ((to.path === '/login' || to.path === '/register') && token) {
      return '/dashboard'
    }
    return true
  }
  if (!token) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }
  const raw = localStorage.getItem(PROFILE_KEY)
  const profile = raw ? JSON.parse(raw) : {}
  const authorities: string[] = Array.isArray(profile.authorities) ? profile.authorities : []
  const rolesRaw: unknown[] = Array.isArray(profile.roles) ? profile.roles : []
  const roles: string[] = rolesRaw.map((r: unknown) =>
    typeof r === 'string' ? r : (r as Record<string, unknown>)?.code as string ?? ''
  ).filter(Boolean)
  if (to.meta.role && !roles.includes(to.meta.role as string)) {
    return '/dashboard'
  }
  const hasRequiredAuthority =
    !to.meta.authority ||
    authorities.includes(to.meta.authority as string) ||
    roles.includes('ADMIN') ||
    (to.meta.authority === 'VESSEL_READ' && authorities.includes('SPECIES_READ'))
  if (!hasRequiredAuthority) {
    return '/dashboard'
  }
  return true
})

export default router
