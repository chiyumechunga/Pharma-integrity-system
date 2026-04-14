import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function ProtectedRoute({ children, allowedRoles }) {
    const { user, isLoading } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <div style={{ padding: '40px', textAlign: 'center', fontFamily: 'var(--font-ui)' }}>Verifying credentials...</div>;
    }

    // If not logged in, redirect to login page with the return url
    if (!user) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // If logged in but doesn't have the right role, redirect to global.public verify
    if (allowedRoles && !allowedRoles.includes(user.role)) {
        console.warn(`Access Denied. Required: ${allowedRoles}, Current: ${user.role}`);
        return <Navigate to="/verify" replace />;
    }

    // Authorized! Render the component
    return children;
}