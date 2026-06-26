import type { AisRecordEnvironment } from '@/types/gsmv'

const NUMBER_KEYS = ['waterDepth'] as const

export function createEmptyAisRecordEnvironment(): AisRecordEnvironment {
  return {
    waterDepth: null,
    weatherCondition: '',
    seaCondition: '',
  }
}

export function parseAisRecordEnvironment(envJson?: string): AisRecordEnvironment {
  if (!envJson) {
    return createEmptyAisRecordEnvironment()
  }

  try {
    const parsed = JSON.parse(envJson) as Record<string, unknown>
    const next = createEmptyAisRecordEnvironment()

    NUMBER_KEYS.forEach((key) => {
      const raw = parsed[key]
      next[key] = typeof raw === 'number' ? raw : raw == null ? null : Number(raw)
      if (Number.isNaN(next[key])) {
        next[key] = null
      }
    })

    next.weatherCondition = typeof parsed.weatherCondition === 'string' ? parsed.weatherCondition : ''
    next.seaCondition = typeof parsed.seaCondition === 'string' ? parsed.seaCondition : ''
    return next
  } catch {
    return createEmptyAisRecordEnvironment()
  }
}

export function stringifyAisRecordEnvironment(env: AisRecordEnvironment): string | undefined {
  const payload: Record<string, number | string> = {}

  NUMBER_KEYS.forEach((key) => {
    const value = env[key]
    if (typeof value === 'number' && Number.isFinite(value)) {
      payload[key] = value
    }
  })

  if (env.weatherCondition?.trim()) {
    payload.weatherCondition = env.weatherCondition.trim()
  }
  if (env.seaCondition?.trim()) {
    payload.seaCondition = env.seaCondition.trim()
  }

  return Object.keys(payload).length ? JSON.stringify(payload) : undefined
}

export function describeAisRecordEnvironment(envJson?: string): string {
  const env = parseAisRecordEnvironment(envJson)
  const parts: string[] = []

  if (typeof env.waterDepth === 'number') {
    parts.push(`水深 ${env.waterDepth} m`)
  }
  if (env.weatherCondition?.trim()) {
    parts.push(`天气 ${env.weatherCondition.trim()}`)
  }
  if (env.seaCondition?.trim()) {
    parts.push(`海况 ${env.seaCondition.trim()}`)
  }

  return parts.length ? parts.join(' / ') : '-'
}

export function aisRecordEnvironmentEntries(envJson?: string) {
  const env = parseAisRecordEnvironment(envJson)

  return [
    { label: '水深', value: typeof env.waterDepth === 'number' ? `${env.waterDepth} m` : '-' },
    { label: '天气', value: env.weatherCondition?.trim() || '-' },
    { label: '海况', value: env.seaCondition?.trim() || '-' },
  ]
}
