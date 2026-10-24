import { ref } from 'vue'
import { listDestinations, listTrips } from '../services/pricing'

/**
 * @param {{ pageSize?: number }} options
 */
export function useTrips(options = {}) {
  const pageSize = options.pageSize ?? 20
  const trips = ref([])
  const destinations = ref([])
  const loading = ref(false)
  const error = ref('')
  const catalogError = ref('')
  const page = ref(0)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const hasNext = ref(false)
  const hasPrevious = ref(false)

  async function load(targetPage = 0) {
    loading.value = true
    error.value = ''
    catalogError.value = ''
    const [cityResult, tripResult] = await Promise.allSettled([
      listDestinations(),
      listTrips({ page: targetPage, size: pageSize }),
    ])
    if (cityResult.status === 'fulfilled') {
      destinations.value = cityResult.value
    } else {
      destinations.value = []
      catalogError.value = cityResult.reason?.message || 'İl listesi alınamadı'
    }
    if (tripResult.status === 'fulfilled') {
      const tripPage = tripResult.value
      trips.value = tripPage.content ?? []
      page.value = tripPage.page ?? targetPage
      totalPages.value = tripPage.totalPages ?? 0
      totalElements.value = tripPage.totalElements ?? trips.value.length
      hasNext.value = Boolean(tripPage.hasNext)
      hasPrevious.value = Boolean(tripPage.hasPrevious)
    } else {
      trips.value = []
      error.value = tripResult.reason?.message || 'Kayıtlı seyahatler alınamadı'
    }
    loading.value = false
  }

  async function goToPage(nextPage) {
    if (nextPage < 0 || (totalPages.value > 0 && nextPage >= totalPages.value)) return
    await load(nextPage)
  }

  return {
    trips,
    destinations,
    loading,
    error,
    catalogError,
    page,
    totalPages,
    totalElements,
    hasNext,
    hasPrevious,
    load,
    goToPage,
    pageSize,
  }
}
