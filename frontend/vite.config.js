import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.API_PROXY_TARGET || 'http://127.0.0.1:8080'
  const proxy = {
    '/api': {
      target,
      changeOrigin: true,
      configure(proxy) {
        proxy.on('proxyReq', (proxyReq) => {
          proxyReq.removeHeader('origin')
        })
      },
    },
  }

  return {
    plugins: [vue(), tailwindcss()],
    server: {
      port: 5173,
      proxy,
    },
    preview: {
      port: 4173,
      proxy,
    },
  }
})
