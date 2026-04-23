import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../services/apiClient';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser]       = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();

    // ── Restore session on app load ───────────────────────────────────────
    useEffect(() => {
        const token = localStorage.getItem('authToken'); // ← fixed key
        if (token) {
            apiClient.get('auth/me')  // ← no leading slash
                .then(response => {
                    setUser(response.data);
                })
                .catch(() => {
                    // Token is invalid or expired — clean up silently
                    localStorage.removeItem('authToken');
                    localStorage.removeItem('userRole');
                    setUser(null);
                })
                .finally(() => setIsLoading(false));
        } else {
            setIsLoading(false);
        }
    }, []);

    // ── Listen for forced 401 logout from apiClient interceptor ──────────
    useEffect(() => {
        const handleForcedLogout = () => {
            setUser(null);
            navigate('/login', { replace: true }); // React Router — no hard reload
        };

        window.addEventListener('auth:logout', handleForcedLogout);
        return () => window.removeEventListener('auth:logout', handleForcedLogout);
    }, [navigate]);

    // ── login: called after successful POST /auth/login ──────────────────
    // Accepts the full AuthResponseDto from Spring Boot:
    // { token, tokenType, participantId, role }
    const login = useCallback(async (authResponse) => {
        localStorage.setItem('authToken', authResponse.token);   // ← fixed key
        localStorage.setItem('userRole', authResponse.role);     // ← store role too

        // Fetch full profile from /auth/me to get username, country, etc.
        try {
            const { data } = await apiClient.get('auth/me');
            setUser(data);
        } catch {
            // Fallback: build minimal user from the login response itself
            setUser({
                participantId: authResponse.participantId,
                role: authResponse.role,
            });
        }
    }, []);

    // ── logout: user-initiated ────────────────────────────────────────────
    const logout = useCallback(() => {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userRole');
        setUser(null);
        navigate('/login', { replace: true });
    }, [navigate]);

    return (
        <AuthContext.Provider value={{ user, login, logout, isLoading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);