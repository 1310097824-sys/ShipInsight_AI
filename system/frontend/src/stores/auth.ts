import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ACCESS_TOKEN_KEY, PROFILE_KEY } from '@/constants/auth'
import { login } from '@/api/auth'
import type { AuthProfile } from '@/types/gsmv'

function isLikelyMojibake(value: string) {
  return /[�ÃÂâÅåÇç]/.test(value) || /[\u00c0-\u00ff]{2,}/.test(value)
}

function normalizeProfile(profile: AuthProfile) {
  const displayName = profile.displayName?.trim()
  return {
    ...profile,
    displayName: displayName && !isLikelyMojibake(displayName) ? displayName : profile.username,
  }
}

function readProfile() {
  const raw = localStorage.getItem(PROFILE_KEY)
  return raw ? normalizeProfile(JSON.parse(raw) as AuthProfile) : null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(ACCESS_TOKEN_KEY) ?? '')
  const profile = ref<AuthProfile | null>(readProfile())
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const authorities = computed(() => profile.value?.authorities ?? [])

  /** 统一提取角色代码，兼容 roles 为 string[] 或 {code:string}[] 两种格式 */
  const roleCodes = computed(() => {
    const raw = profile.value?.roles
    if (!Array.isArray(raw)) return []
    return raw.map((r: unknown) =>
      typeof r === 'string' ? r : ((r as Record<string, unknown>)?.code as string) ?? ''
    ).filter(Boolean)
  })

  function persistProfile(nextProfile: AuthProfile | null) {
    const normalizedProfile = nextProfile ? normalizeProfile(nextProfile) : null
    profile.value = normalizedProfile
    if (normalizedProfile) {
      localStorage.setItem(PROFILE_KEY, JSON.stringify(normalizedProfile))
    } else {
      localStorage.removeItem(PROFILE_KEY)
    }
  }

  async function performLogin(username: string, password: string) {
    loading.value = true
    try {
      const payload = await login({ username, password })
      token.value = payload.accessToken
      persistProfile(payload.user)
      localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken)
    } finally {
      loading.value = false
    }
  }

  function patchProfile(patch: Partial<AuthProfile>) {
    if (!profile.value) {
      return
    }
    persistProfile({
      ...profile.value,
      ...patch,
    })
  }

  function logout() {
    token.value = ''
    persistProfile(null)
    localStorage.removeItem(ACCESS_TOKEN_KEY)
  }

  return {
    token,
    profile,
    loading,
    isAuthenticated,
    authorities,
    roleCodes,
    performLogin,
    patchProfile,
    logout,
  }
})
