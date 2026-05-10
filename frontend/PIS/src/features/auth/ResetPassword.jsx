// src/features/auth/ResetPassword.jsx
import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { apiClient } from '../../services/apiClient';

export default function ResetPassword() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get("token"); // Extracts the token from the URL

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [message, setMessage] = useState('');
    const [errorMsg, setErrorMsg] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrorMsg('');
        setMessage('');

        if (newPassword !== confirmPassword) {
            setErrorMsg("Passwords do not match.");
            return;
        }

        if (!token) {
            setErrorMsg("Invalid or missing password reset token.");
            return;
        }

        setIsLoading(true);
        try {
            const { data } = await apiClient.post('auth/reset-password', { token, newPassword });
            setMessage(data.message || 'Password successfully reset.');
            setTimeout(() => navigate('/login'), 3000); // Redirect to login after 3 seconds
        } catch (error) {
            setErrorMsg(error.response?.data?.detail || 'Failed to reset password. The token may be expired.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: 'var(--surface)', padding: '24px' }}>
            <div style={{ background: 'white', padding: '40px', borderRadius: 'var(--radius-xl)', boxShadow: '0 8px 32px rgba(0, 70, 85, 0.04)', width: '100%', maxWidth: '400px' }}>
                <h1 style={{ color: 'var(--primary)', marginBottom: '16px', textAlign: 'center', fontSize: '28px', fontWeight: '700', letterSpacing: '-0.02em' }}>
                    Create New Password
                </h1>

                {errorMsg && (
                    <div style={{ color: '#ba1a1a', backgroundColor: '#ffdad6', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', fontFamily: 'var(--font-ui)' }}>
                        {errorMsg}
                    </div>
                )}
                {message && (
                    <div style={{ color: '#106e41', backgroundColor: '#c4eed0', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', fontFamily: 'var(--font-ui)', textAlign: 'center' }}>
                        {message} Redirecting...
                    </div>
                )}

                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <div style={{ position: 'relative', width: '100%' }}>
                        <input
                            type={showPassword ? "text" : "password"}
                            placeholder="New Password"
                            style={{ padding: '14px', paddingRight: '48px', border: '1px solid var(--border)', borderRadius: '8px', fontFamily: 'var(--font-ui)', fontSize: '15px', width: '100%', boxSizing: 'border-box' }}
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            required
                            minLength={6}
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--on-surface-variant)' }}
                            tabIndex="-1"
                        >
                            <span className="material-symbols-outlined">{showPassword ? 'visibility_off' : 'visibility'}</span>
                        </button>
                    </div>

                    <div style={{ position: 'relative', width: '100%' }}>
                        <input
                            type={showPassword ? "text" : "password"}
                            placeholder="Confirm New Password"
                            style={{ padding: '14px', paddingRight: '48px', border: '1px solid var(--border)', borderRadius: '8px', fontFamily: 'var(--font-ui)', fontSize: '15px', width: '100%', boxSizing: 'border-box' }}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                            minLength={6}
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={isLoading || !token}
                        style={{ padding: '16px', backgroundColor: 'var(--primary-container)', color: 'white', border: 'none', borderRadius: '999px', fontWeight: '600', fontFamily: 'var(--font-ui)', fontSize: '15px', cursor: (isLoading || !token) ? 'not-allowed' : 'pointer', marginTop: '8px', opacity: (isLoading || !token) ? 0.7 : 1 }}
                    >
                        {isLoading ? 'Resetting...' : 'Save New Password'}
                    </button>
                </form>
            </div>
        </div>
    );
}