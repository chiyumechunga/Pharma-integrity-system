// frontend/PIS/src/services/apiClient.js
import axios from 'axios';

// 1. Dynamically grab the IP address currently in the browser's address bar
const currentHost = window.location.hostname;

// 2. Construct the backend URL using that same IP, but pointing to port 8080
const dynamicBaseUrl = `https://${currentHost}:8080/api/v1/`;

export const apiClient = axios.create({
    baseURL: dynamicBaseUrl,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Request: inject JWT
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

        // FIXED: Catch raw text/HTML errors from Spring Boot before they break React
        if (typeof data === 'string') {
            console.error("Raw Backend Error:", data); // Logs the stack trace
            error.userMessage = 'A server error occurred. Please check the backend console.';
            error.validationErrors = null;
        } else {
            // Standard JSON error handling
            error.userMessage = data?.message ?? fallbackMessage(status);
            error.validationErrors = data?.validationErrors ?? null;
        }

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