# Üretim notları

## Zorunlu ortam değişkenleri

| Değişken | Açıklama |
|----------|----------|
| `POSTGRES_PASSWORD` | Veritabanı parolası (`vacation` kullanmayın) |
| `JWT_SECRET` | En az 32 karakter |
| `SCRAPER_API_KEY` | Scraper → API yazma anahtarı |
| `SCRAPER_TRIGGER_KEY` | API → scraper tetikleme anahtarı |

İsteğe bağlı: `GROQ_API_KEY`, `CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`.

Örnek yapılandırma: `api/src/main/resources/application-prod.yml.example`.

## Sağlık ve izleme

Spring Boot Actuator: `GET /actuator/health`, `GET /actuator/info`. Üretimde `management.endpoints.web.exposure` ile metrikleri açabilirsiniz.

## Veritabanı

Şema yalnız Liquibase ile (`api/src/main/resources/db/changelog/`). `ddl-auto: validate`.
