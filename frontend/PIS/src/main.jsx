import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { registerSW } from 'virtual:pwa-register'
import './index.css'
import App from './App.jsx'

// Auto-update: reloads the page when a new SW version is installed
const updateSW = registerSW({
    onNeedRefresh() {
        // Optional: show a toast/banner instead of auto-reloading
        if (confirm('New version available. Reload?')) updateSW(true)
    },
    onOfflineReady() {
        console.info('[PIS] App is ready to work offline')
    },
    onRegistered(r) {
        console.info('[PIS] SW registered:', r)
    },
    onRegisterError(error) {
        console.error('[PIS] SW registration error:', error)
    },
})


createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
