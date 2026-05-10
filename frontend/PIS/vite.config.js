import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'fs'
import path from 'path'
import os from 'os'

const mkcertDir = path.join(os.homedir(), '.local', 'share', 'mkcert')

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',   // expose on all interfaces — required for phone access
    port: 5173,
    https: {
      key:  fs.readFileSync('./localhost+2-key.pem'),
      cert: fs.readFileSync('./localhost+2.pem'),
    },
    // No proxy — VITE_API_URL is absolute (https://192.168.0.142:8080/api/v1/)
    // Proxy only makes sense when VITE_API_URL=/api/v1/ (relative)
  },
})