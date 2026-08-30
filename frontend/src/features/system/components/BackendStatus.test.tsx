import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BackendStatus } from './BackendStatus'

const successResponse = () =>
  new Response(
    JSON.stringify({
      status: 'UP',
      service: 'mydata-card-recommendation-api',
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )

describe('BackendStatus', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('로딩 후 연결 성공 정보를 보여준다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(successResponse()))

    render(<BackendStatus />)

    expect(screen.getByText('백엔드 연결 확인 중')).toBeInTheDocument()
    expect(await screen.findByText('API 연결됨')).toBeInTheDocument()
    expect(screen.getByText(/mydata-card-recommendation-api/)).toBeInTheDocument()
  })

  it('실패 후 다시 연결할 수 있다', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockResolvedValueOnce(successResponse())
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<BackendStatus />)

    expect(await screen.findByText('백엔드 연결 실패')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '다시 확인' }))

    expect(await screen.findByText('API 연결됨')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
