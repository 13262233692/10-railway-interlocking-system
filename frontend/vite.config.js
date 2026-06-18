import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite配置文件
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      // API请求代理到后端8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
