import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      // 告诉 Vite：所有以 /api 开头的请求，都转发给本地开启的后端（3000端口）
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true
      }
    }
  }
})