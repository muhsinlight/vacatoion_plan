---
name: change-frontend
description: >-
  Vacation Plan Vue arayüzüne sayfa, oturum veya fiyat görünümü ekler. frontend,
  Vue, rota, çerez veya Tailwind değişince uygula. Pinia ve Axios ekleme.
---

# Arayüz değişikliği

Önce `source-map`. Vue kaynağı yalnız `frontend/src`. Geliştirme `npm run dev` (5173). Ayrıntı kök `README.md` Arayüz bölümü.

- HTTP `services/http.js` üzerinden `fetch`. `credentials: 'include'`. Pinia ve Axios yok. Oturum `stores/auth.js` composable’ındadır.
- Yeni sayfa `router/index.js` rotası ve `views/` bileşeni ister. Mevcut yollar: `/login`, `/register`, `/`, `/pricing`, `/trips`, `/trips/:id`, `/profile`.
- Stil Tailwind CSS 4. `VITE_API_BASE_URL` boş kalır; aksi halde çerez oturumu taşınmaz.
- `npm run build` çıktısı `frontend/dist`. Repoya girmez.

Test: `frontend/` içinde `npm test`. Görünen davranış için tarayıcıda `http://localhost:5173` akışını çalıştır.
