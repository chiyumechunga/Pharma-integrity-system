// src/features/auth/ForgotPassword.jsx
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../../services/apiClient';

export default function ForgotPassword() {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setMessage('');

        try {
            const { data } = await apiClient.post('auth/forgot-password', { email });
            setMessage(data.message || 'If the email exists, a reset link has been sent.');
        } catch (error) {
            setMessage('If the email exists, a reset link has been sent.'); // Standard anti-enumeration response
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: 'var(--surface)', padding: '24px' }}>
            <div style={{ background: 'white', padding: '40px', borderRadius: 'var(--radius-xl)', boxShadow: '0 8px 32px rgba(0, 70, 85, 0.04)', width: '100%', maxWidth: '400px' }}>
                <div style={{ marginBottom: '32px' }}>
                    <Link to="/login" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', color: 'var(--on-surface-variant)', textDecoration: 'none', fontSize: '14px', fontWeight: '600', fontFamily: 'var(--font-ui)' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                        Back to Login
                    </Link>
                </div>

                <h1 style={{ color: 'var(--primary)', marginBottom: '16px', textAlign: 'center', fontSize: '28px', fontWeight: '700', letterSpacing: '-0.02em' }}>
                    Reset Password
                </h1>
                <p style={{ textAlign: 'center', fontFamily: 'var(--font-ui)', fontSize: '14px', color: 'var(--on-surface-variant)', marginBottom: '24px' }}>
                    Enter your email address and we'll send you a link to reset your password.
                </p>

                {message && (
                    <div style={{ color: '#106e41', backgroundColor: '#c4eed0', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', fontFamily: 'var(--font-ui)', textAlign: 'center' }}>
                        {message}
                    </div>
                )}

                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <input
                        type="email"
                        placeholder="Email Address"
                        style={{ padding: '14px', border: '1px solid var(--border)', borderRadius: '8px', fontFamily: 'var(--font-ui)', fontSize: '15px', width: '100%', boxSizing: 'border-box' }}
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <button
                        type="submit"
                        disabled={isLoading}
                        style={{ padding: '16px', backgroundColor: 'var(--primary-container)', color: 'white', border: 'none', borderRadius: '999px', fontWeight: '600', fontFamily: 'var(--font-ui)', fontSize: '15px', cursor: isLoading ? 'not-allowed' : 'pointer', marginTop: '8px', opacity: isLoading ? 0.7 : 1 }}
                    >
                        {isLoading ? 'Sending...' : 'Send Reset Link'}
                    </button>
                </form>
            </div>
        </div>
    );
}