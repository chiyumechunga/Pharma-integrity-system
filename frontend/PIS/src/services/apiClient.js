// frontend/PIS/src/services/apiClient.js
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1/';

if (import.meta.env.DEV) {
    console.info('[apiClient] Base URL:', API_BASE_URL);
}

export const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 20000, // 20s — Firefly/blockchain calls can be slow
    headers: {
        'Content-Type': 'application/json',
    },
});

// ── Request: inject JWT ───────────────────────────────────────────────────────
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('authToken'); // matches AuthContext key
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        // QR image endpoints return a PNG blob — detect by URL pattern
        if (config.url?.includes('/qr/')) {
            config.responseType = 'blob';
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// ── Response: normalise all errors into a consistent shape ───────────────────
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {

        // No response at all = network down or Spring Boot not running
        if (!error.response) {
            const networkError = new Error(
                'Cannot reach the server. Check your network or that the backend is running.'
            );
            networkError.isNetworkError = true;
            return Promise.reject(networkError);
        }

        const { status, data } = error.response;

        // Attach the backend's message to the error so components can display it
        error.userMessage = data?.message ?? fallbackMessage(status);
        error.validationErrors = data?.validationErrors ?? null; // from GlobalExceptionHandler

        // 401 — token expired or invalid: clear session
        // Use a custom event so React Router (not window.location) handles the redirect
        if (status === 401) {
            localStorage.removeItem('authToken');
            localStorage.removeItem('userRole');
            window.dispatchEvent(new CustomEvent('auth:logout'));
        }

        return Promise.reject(error);
    }
);

function fallbackMessage(status) {
    const messages = {
        400: 'Invalid request. Check your input.',
        403: 'You do not have permission to perform this action.',
        404: 'The requested resource was not found.',
        405: 'Action not supported.',
        409: 'A conflict occurred — this resource may already exist.',
        422: 'Validation failed. Check your input fields.',
        500: 'Server error. Please try again.',
    };
    return messages[status] ?? `Unexpected error (HTTP ${status}).`;
}

export default apiClient;