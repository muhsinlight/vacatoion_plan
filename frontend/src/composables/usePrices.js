import { ref } from 'vue'
import { listDestinations, previewTrip } from '../services/pricing'

export function usePrices() {
  const destinations = ref([])
  const quote = ref(null)
  const loadingDestinations = ref(false)
  const loadingQuote = ref(false)
  const destinationError = ref('')
  const quoteError = ref('')

  async function loadDestinations() {
    loadingDestinations.value = true
    destinationError.value = ''
    try {
      destinations.value = await listDestinations()
    } catch (error) {
      destinations.value = []
      destinationError.value = error.message || 'İller alınamadı'
    } finally {
      loadingDestinations.value = false
    }
  }

  let quoteSeq = 0

  async function loadQuote(body) {
    const seq = ++quoteSeq
    loadingQuote.value = true
    quoteError.value = ''
    try {
      const next = await previewTrip(body)
      if (seq !== quoteSeq) {
        return
      }
      quote.value = next
    } catch (error) {
      if (seq !== quoteSeq) {
        return
      }
      quote.value = null
      quoteError.value = error.message || 'Fiyatlar alınamadı'
    } finally {
      if (seq === quoteSeq) {
        loadingQuote.value = false
      }
    }
  }

  return {
    destinations,
    quote,
    loadingDestinations,
    loadingQuote,
    destinationError,
    quoteError,
    loadDestinations,
    loadQuote,
  }
}
