import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom'; // 1. Added Link import
import { useMutation } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from './AuthContext';

export default function Login() {
    const navigate = useNavigate();
    const { login } = useAuth();
    const [credentials, setCredentials] = useState({ email: '', password: '' });

    const loginMutation = useMutation({
        mutationFn: async (data) => {
            const response = await apiClient.post('/auth/login', data);
            return response.data;
        },
        onSuccess: (data) => {
            login({ username: data.username, role: data.role, participantId: data.participantId }, data.token);

            if (data.role === 'MANUFACTURER') navigate('/manufacturer');
            else if (data.role === 'ZAMMSA' || data.role === 'PHARMACY') navigate('/handover');
            else if (data.role === 'ZAMRA') navigate('/regulator');
            else navigate('/verify');
        },
        onError: (error) => {
            console.error("Login failed:", error);
            alert("Login failed. Please check your credentials.");
        }
    });

    // Inside Login.jsx onSubmit handler
    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // POST /auth/login → returns { token, tokenType, participantId, role }
            const { data } = await apiClient.post('auth/login', { email, password });
            await login(data);              // AuthContext handles /auth/me fetch + state

            // Redirect by role
            const roleRoutes = {
                MANUFACTURER: '/manufacturer',
                ZAMRA:         '/regulator',
                ZAMMSA:        '/handover',
                PHARMACY:      '/pharmacy',
            };
            navigate(roleRoutes[data.role] ?? '/');
        } catch (error) {
            setErrorMsg(error.userMessage ?? 'Login failed.'); // from apiClient interceptor
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: 'var(--surface)', padding: '24px' }}>
            <div style={{ background: 'white', padding: '40px', borderRadius: 'var(--radius-xl)', boxShadow: '0 8px 32px rgba(0, 70, 85, 0.04)', width: '100%', maxWidth: '400px' }}>

                {/* 2. NEW: Forward/Backward Navigation Stack */}
                <div style={{ marginBottom: '32px' }}>
                    <Link
                        to="/"
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '8px',
                            color: 'var(--on-surface-variant)',
                            textDecoration: 'none',
                            fontSize: '14px',
                            fontWeight: '600',
                            fontFamily: 'var(--font-ui)',
                            transition: 'color 0.2s'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary)'}
                        onMouseLeave={(e) => e.currentTarget.style.color = 'var(--on-surface-variant)'}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                        Back to Home
                    </Link>
                </div>

                <h1 style={{ color: 'var(--primary)', marginBottom: '24px', textAlign: 'center', fontSize: '28px', fontWeight: '700', letterSpacing: '-0.02em' }}>
                    System Login
                </h1>

                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <input
                        type="email"
                        placeholder="Email Address"
                        style={{ padding: '14px', border: '1px solid var(--border)', borderRadius: '8px', fontFamily: 'var(--font-ui)', fontSize: '15px' }}
                        value={credentials.email}
                        onChange={(e) => setCredentials({ ...credentials, email: e.target.value })}
                        required
                    />
                    <input
                        type="password"
                        placeholder="Password"
                        style={{ padding: '14px', border: '1px solid var(--border)', borderRadius: '8px', fontFamily: 'var(--font-ui)', fontSize: '15px' }}
                        value={credentials.password}
                        onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
                        required
                    />

                    <button
                        type="submit"
                        disabled={loginMutation.isPending}
                        style={{
                            padding: '16px',
                            backgroundColor: 'var(--primary-container)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '999px',
                            fontWeight: '600',
                            fontFamily: 'var(--font-ui)',
                            fontSize: '15px',
                            cursor: loginMutation.isPending ? 'not-allowed' : 'pointer',
                            marginTop: '8px',
                            transition: 'background-color 0.2s'
                        }}
                    >
                        {loginMutation.isPending ? 'Authenticating...' : 'Secure Login'}
                    </button>
                </form>
            </div>
        </div>
    );
}