import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import fs from 'fs'
import path from 'path'
import os from 'os'

const mkcertDir = path.join(os.homedir(), '.local', 'share', 'mkcert')

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,webp,woff2}'],
        runtimeCaching: [
          {
            // Matches  absolute VITE_API_URL backend
            urlPattern: ({ url }) => url.pathname.startsWith('/api/'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'pis-api-cache',
              expiration: { maxEntries: 100, maxAgeSeconds: 86400 },
              networkTimeoutSeconds: 10,
            },
          },
        ],
      },
      manifest: {
        name: 'Pharmaceutical Integrity System',
        short_name: 'PIS',
        description: 'Blockchain-powered pharmaceutical supply chain tracker',
        theme_color: '#01696f',
        background_color: '#f7f6f2',
        display: 'standalone',
        orientation: 'portrait-primary',
        start_url: '/',
        scope: '/',
        id: '/',
        icons: [
          { src: '/icons/pwa-192x192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
          { src: '/icons/pwa-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          { src: '/icons/pwa-512x512-maskable.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
        categories: ['health', 'medical'],
      },
      devOptions: { enabled: true, type: 'module' },
    }),
  ],
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