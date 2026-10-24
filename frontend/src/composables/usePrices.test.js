import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../services/pricing.js', () => ({
  listDestinations: vi.fn(),
  previewTrip: vi.fn(),
}))

describe('usePrices', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('stores destinations and surfaces a load error', async () => {
    const pricing = await import('../services/pricing.js')
    const { usePrices } = await import('./usePrices.js')
    pricing.listDestinations.mockResolvedValueOnce([{ cityKey: 'izmir' }])
    const prices = usePrices()
    await prices.loadDestinations()
    expect(prices.destinations.value).toEqual([{ cityKey: 'izmir' }])
    expect(prices.loadingDestinations.value).toBe(false)

    pricing.listDestinations.mockRejectedValueOnce(new Error('İller alınamadı'))
    await prices.loadDestinations()
    expect(prices.destinations.value).toEqual([])
    expect(prices.destinationError.value).toBe('İller alınamadı')
  })

  it('ignores an older quote that finishes last', async () => {
    const pricing = await import('../services/pricing.js')
    const { usePrices } = await import('./usePrices.js')
    let finishFirst
    pricing.previewTrip
      .mockImplementationOnce(() => new Promise((resolve) => {
        finishFirst = resolve
      }))
      .mockResolvedValueOnce({ total: 2 })
    const prices = usePrices()
    const first = prices.loadQuote({ seq: 1 })
    const second = prices.loadQuote({ seq: 2 })
    await second
    finishFirst({ total: 1 })
    await first
    expect(prices.quote.value).toEqual({ total: 2 })
    expect(prices.loadingQuote.value).toBe(false)
    expect(prices.quoteError.value).toBe('')
  })
})
