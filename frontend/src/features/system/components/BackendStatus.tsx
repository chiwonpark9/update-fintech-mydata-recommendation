import { useEffect, useState } from 'react'
import { getBackendHealth, type HealthResponse } from '../api/health'

type ConnectionState =
  | { status: 'loading' }
  | { status: 'connected'; data: HealthResponse }
  | { status: 'error'; message: string }

export function BackendStatus() {
  const [attempt, setAttempt] = useState(0)
  const [connection, setConnection] = useState<ConnectionState>({
    status: 'loading',
  })

  useEffect(() => {
    const controller = new AbortController()

    getBackendHealth(controller.signal)
      .then((data) => setConnection({ status: 'connected', data }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }

        const message =
          error instanceof Error ? error.message : '연결 상태를 확인할 수 없습니다.'
        setConnection({ status: 'error', message })
      })

    return () => controller.abort()
  }, [attempt])

  const retry = () => {
    setConnection({ status: 'loading' })
    setAttempt((value) => value + 1)
  }

  const title =
    connection.status === 'connected'
      ? 'API 연결됨'
      : connection.status === 'error'
        ? '백엔드 연결 실패'
        : '백엔드 연결 확인 중'

  return (
    <div
      className="backend-status"
      data-state={connection.status}
      aria-live="polite"
    >
      <span className="status-light" aria-hidden="true" />
      <div>
        <strong>{title}</strong>
        {connection.status === 'connected' && (
          <span>
            {connection.data.service} · {connection.data.status}
          </span>
        )}
        {connection.status === 'loading' && <span>서비스 응답을 기다리고 있어요.</span>}
        {connection.status === 'error' && <span>{connection.message}</span>}
      </div>
      {connection.status === 'error' && (
        <button type="button" onClick={retry}>
          다시 확인
        </button>
      )}
    </div>
  )
}
