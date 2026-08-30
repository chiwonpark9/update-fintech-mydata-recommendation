export interface HealthResponse {
  status: string
  service: string
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

function isHealthResponse(value: unknown): value is HealthResponse {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.status === 'string' &&
    typeof candidate.service === 'string'
  )
}

export async function getBackendHealth(
  signal?: AbortSignal,
): Promise<HealthResponse> {
  const response = await fetch(`${apiBaseUrl}/api/v1/health`, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) {
    throw new Error(`백엔드 상태 확인에 실패했습니다. HTTP ${response.status}`)
  }

  const body: unknown = await response.json()
  if (!isHealthResponse(body)) {
    throw new Error('백엔드 상태 응답 형식이 올바르지 않습니다.')
  }

  return body
}
