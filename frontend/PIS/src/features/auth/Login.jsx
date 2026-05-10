// src/features/auth/Login.jsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { apiClient } from '../../services/apiClient';
import { useAuth } from './AuthContext';
import styles from './Auth.module.css';

export default function Login() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [credentials, setCredentials] = useState({ email: '', password: '' });
    const [errorMsg, setErrorMsg] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setErrorMsg("");

        try {
            const { data } = await apiClient.post('auth/login', credentials);
            await login(data);

            const roleRoutes = {
                MANUFACTURER: '/manufacturer',
                ZAMRA:         '/regulator',
                ZAMMSA:        '/zammsa',
                PHARMACY:      '/pharmacy',
            };
            navigate(roleRoutes[data.role] ?? '/');
        } catch (error) {
            setErrorMsg(error.response?.data?.detail || 'Login failed. Please check your credentials.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className={styles.authWrapper}>
            <div className={styles.authCard}>

                <Link to="/" className={styles.backLink}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                    Back to Home
                </Link>

                <h1 className={styles.title}>Welcome back</h1>
                <p className={styles.subtitle}>Please enter your details to sign in.</p>

                {errorMsg && (
                    <div className={styles.errorBox}>
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>error</span>
                        {errorMsg}
                    </div>
                )}

                <form onSubmit={handleSubmit} className={styles.form}>
                    <div className={styles.inputGroup}>
                        <input
                            type="email"
                            placeholder="Email Address"
                            className={styles.input}
                            value={credentials.email}
                            onChange={(e) => setCredentials({ ...credentials, email: e.target.value })}
                            required
                        />
                    </div>

                    <div className={styles.inputGroup}>
                        <input
                            type={showPassword ? "text" : "password"}
                            placeholder="Password"
                            className={styles.input}
                            style={{ paddingRight: '48px' }}
                            value={credentials.password}
                            onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
                            required
                        />
                        <button
                            type="button"
                            className={styles.iconBtn}
                            onClick={() => setShowPassword(!showPassword)}
                            tabIndex="-1"
                        >
                            <span className="material-symbols-outlined">
                                {showPassword ? 'visibility_off' : 'visibility'}
                            </span>
                        </button>
                    </div>

                    <Link to="/forgot-password" className={styles.forgotLink}>
                        Forgot Password?
                    </Link>

                    <button type="submit" disabled={isLoading} className={styles.submitBtn}>
                        {isLoading ? 'Authenticating...' : 'Sign In'}
                    </button>
                </form>
            </div>
        </div>
    );
}