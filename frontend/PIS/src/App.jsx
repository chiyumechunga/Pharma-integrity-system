import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { AuthProvider } from './features/auth/AuthContext';
import ProtectedRoute from './features/auth/ProtectedRoute';

// Import our components
import LandingPage from './features/landing/LandingPage';
import Login from './features/auth/Login';
import PublicVerification from './features/verification/PublicVerification';
import ManufacturerRegistry from './features/batch-registry/ManufacturerRegistry';
import CustodyTransfer from './features/custody/CustodyTransfer';
import RegulatoryDashboard from './features/regulatory/RegulatoryDashboard';
import PharmacyDashboard from './features/pharmacy/PharmacyDashboard';

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: 1,
            refetchOnWindowFocus: true,
        },
    },
});

function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <AuthProvider>
                <Router>
                    <Routes>
                        {/* 1. Landing Page is now the default root */}
                        <Route path="/" element={<LandingPage />} />

                        {/* 2. Public Scanner & Login */}
                        <Route path="/verify" element={<PublicVerification />} />
                        <Route path="/login" element={<Login />} />

                        {/* Protected Route: Manufacturer Only */}
                        <Route
                            path="/manufacturer"
                            element={
                                <ProtectedRoute allowedRoles={['MANUFACTURER']}>
                                    <ManufacturerRegistry />
                                </ProtectedRoute>
                            }
                        />

                        {/* Protected Route: Supply Chain Operations */}
                        <Route
                            path="/handover"
                            element={
                                <ProtectedRoute allowedRoles={['MANUFACTURER', 'ZAMMSA', 'PHARMACY']}>
                                    <CustodyTransfer />
                                </ProtectedRoute>
                            }
                        />

                        {/* Protected Route: Regulator Only */}
                        <Route
                            path="/regulator"
                            element={
                                <ProtectedRoute allowedRoles={['ZAMRA']}>
                                    <RegulatoryDashboard />
                                </ProtectedRoute>
                            }
                        />
                        {/* Protected Route: Pharmacy Operations */}
                        <Route
                            path="/pharmacy"
                            element={
                                <ProtectedRoute allowedRoles={['PHARMACY']}>
                                    <PharmacyDashboard />
                                </ProtectedRoute>
                            }
                        />

                        {/* Default Route redirects to Landing Page now instead of /verify */}
                        <Route path="*" element={<Navigate to="/" replace />} />
                    </Routes>
                </Router>
            </AuthProvider>
        </QueryClientProvider>
    );
}

export default App;