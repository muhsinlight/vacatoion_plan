/*
 * Planla ekraninin ornek verisi.
 *
 * Yalnizca gercek API bos dondugunde devreye girer ve arayuzde her zaman
 * "Ornek veri" etiketiyle gosterilir. Gercek fiyatin yerine sessizce
 * gecmemesi icin `isMock: true` isareti veriyle birlikte tasinir.
 *
 * Gercek API'ye baglarken: bu dosyayi import etmeyi birakmak yeterli,
 * bileşenler ham `quote` / `activities` sekliyle de calisir.
 */

export const MOCK_QUOTE = {
  isMock: true,
  priceNote: 'Bu tutarlar örnek veridir, canlı fiyatlardan gelmemiştir.',
  budget: {
    total: { min: 13386, max: 16597, currency: 'TRY' },
    perPerson: { min: 6693, max: 8299, currency: 'TRY' },
    dailyAverage: { min: 3347, max: 4149, currency: 'TRY' },
    lines: [
      {
        label: 'Şehirler arası otobüs',
        category: 'TRANSPORT',
        min: 2400,
        max: 2960,
        currency: 'TRY',
        sourceType: 'SCRAPE',
      },
      {
        label: 'Otel, 3 gece',
        category: 'LODGING',
        min: 5400,
        max: 6900,
        currency: 'TRY',
        sourceType: 'SCRAPE',
      },
      {
        label: 'Yeme-içme',
        category: 'FOOD',
        min: 3200,
        max: 3900,
        currency: 'TRY',
        sourceType: 'ESTIMATE',
      },
      {
        label: 'Şehir içi ulaşım (kişi başı günlük)',
        category: 'LOCAL_TRANSPORT',
        min: 1168,
        max: 1328,
        currency: 'TRY',
        sourceType: 'ESTIMATE',
      },
      {
        label: 'Aktivite ve hediyelik',
        category: 'OTHER',
        min: 1218,
        max: 1509,
        currency: 'TRY',
        sourceType: 'ESTIMATE',
      },
    ],
  },
  assumptions: [
    { field: 'transportMode', message: 'Otobüs fiyatı son 7 günün en düşük biletinden alındı.' },
    { field: 'accommodationType', message: 'Otel tutarı çift kişilik oda varsayımıyla hesaplandı.' },
  ],
  budgetSuggestions: [
    { message: 'Konaklamayı pansiyona çevirmek üst sınırı yaklaşık 1.900 TRY indirir.' },
  ],
  busLegs: [],
  foodPlaces: [],
  itinerary: [],
  lodging: null,
}

export const MOCK_ACTIVITIES = [
  {
    id: 'mock-1',
    name: 'Tarihi çarşı yürüyüşü',
    description: 'Kapalı çarşı ve çevresindeki el sanatları atölyeleri.',
    estimatedMinutes: 120,
    entryFeePerPerson: { min: 0, max: 150, currency: 'TRY' },
    isMock: true,
  },
  {
    id: 'mock-2',
    name: 'Kent müzesi',
    description: 'Şehrin kuruluşundan bugüne kalan koleksiyon.',
    estimatedMinutes: 90,
    priceMin: 120,
    priceMax: 220,
    currency: 'TRY',
    isMock: true,
  },
  {
    id: 'mock-3',
    name: 'Tekne turu',
    description: 'Koy boyunca iki saatlik gezi, mola dahil.',
    estimatedMinutes: 150,
    priceMin: 450,
    priceMax: 800,
    currency: 'TRY',
    isMock: true,
  },
  {
    id: 'mock-4',
    name: 'Yerel mutfak tadım turu',
    description: 'Rehberli dört duraklı sokak lezzeti turu.',
    estimatedMinutes: 180,
    priceMin: 600,
    priceMax: 1100,
    currency: 'TRY',
    isMock: true,
  },
  {
    id: 'mock-5',
    name: 'Gün batımı tepesi',
    description: 'Panoramik seyir noktası, ulaşım hariç.',
    estimatedMinutes: 60,
    priceMin: 0,
    priceMax: 80,
    currency: 'TRY',
    isMock: true,
  },
  {
    id: 'mock-6',
    name: 'Doğa parkı patikası',
    description: 'İşaretli kısa parkur, giriş ücreti dahil.',
    estimatedMinutes: 210,
    priceMin: 80,
    priceMax: 180,
    currency: 'TRY',
    isMock: true,
  },
]
