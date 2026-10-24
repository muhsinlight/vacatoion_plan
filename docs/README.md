# Dokümantasyon

Çalıştırma ve `vacation.*` anahtarları kök [README.md](../README.md) içindedir.

Tarayıcı arayüzü ayrı bir süreçtir. Vue 3 uygulaması `frontend/` altındadır ve geliştirmede `http://localhost:5173` adresinden gelir. Thymeleaf kullanılmaz. API `http://localhost:8080` yalnızca JSON sunar.

| Dosya | Ne anlatır |
|-------|------------|
| [sistem.md](sistem.md) | Süreçler, paketler, tablolar |
| [onizleme.md](onizleme.md) | Önizleme, kayıt, destinasyon, Groq, HTTP hataları |
| [scraper.md](scraper.md) | İki yönlü çağrı, uçlar, zamanlanmış işler |
| [deploy.md](deploy.md) | Üretim ortamı, sağlık uçları, Liquibase |

Paket fikri kök `package-info.java` içindedir; `controller`, `dto` ve `exception` kendi özetini tutar. Metot sözleşmesi türün üstündeki Javadoc’tadır. HTML site `api` dizininde `mvnw.cmd javadoc:javadoc` ile üretilir; çıktı `api/target/reports/apidocs/index.html`. Alan listesi `http://localhost:8080/swagger-ui.html` üzerindedir.
