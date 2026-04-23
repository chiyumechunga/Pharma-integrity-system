import apiClient from './apiClient';

export const authService = {
    login: (email, password) =>
        apiClient.post('auth/login', { email, password }).then(r => r.data),

    getMe: () =>
        apiClient.get('auth/me').then(r => r.data),

    logout: () => {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userRole');
    },
};