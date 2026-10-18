import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Browser requests to /api reach Spring Boot while keeping the session cookie.
    proxy: { '/api': 'http://localhost:8080' },
  },
})
