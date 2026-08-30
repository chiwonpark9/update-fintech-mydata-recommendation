import { afterEach, describe, expect, it, vi } from 'vitest'
import { getBackendHealth } from './health'

describe('getBackendHealth', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('정상적인 Health 응답을 반환한다', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          status: 'UP',
          service: 'mydata-card-recommendation-api',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getBackendHealth()).resolves.toEqual({
      status: 'UP',
      service: 'mydata-card-recommendation-api',
    })
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/health',
      expect.objectContaining({ headers: { Accept: 'application/json' } }),
    )
  })

  it('응답 형식이 계약과 다르면 실패한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: true }), { status: 200 }),
      ),
    )

    await expect(getBackendHealth()).rejects.toThrow(
      '백엔드 상태 응답 형식이 올바르지 않습니다.',
    )
  })
})
