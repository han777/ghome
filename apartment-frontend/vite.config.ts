import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    host: '127.0.0.1',
    port: 3003,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:10080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://127.0.0.1:10080',
        changeOrigin: true
      }
    }
  }
})
