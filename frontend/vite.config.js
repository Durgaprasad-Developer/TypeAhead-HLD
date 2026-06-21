import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    proxy: {
      // Proxy all backend API calls to Spring Boot so no hardcoded URLs are
      // needed in frontend code and no CORS issues arise in development.
      '/suggest': 'http://localhost:8080',
      '/search': 'http://localhost:8080',
      '/cache': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
      '/api-docs': 'http://localhost:8080',
    },
  },
})
