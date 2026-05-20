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
import ZammsaDashboard from "./features/distributor/ZammsaDashboard";

// New Standalone Action Screens
import ProvenanceAudit from './features/pharmacy/ProvenanceAudit';
import DispenseItem from './features/pharmacy/DispenseItem';
import MarkDestroyed from './features/pharmacy/MarkDestroyed';

// External imports that might be in your project
// import ReceiveInventory from './features/custody/ReceiveInventory';

import ForgotPassword from './features/auth/ForgotPassword';
import ResetPassword from './features/auth/ResetPassword';
import Footer from './components/Footer';

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
            <Router>
                <AuthProvider>
                    {/* Global Layout Wrapper */}
                    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
                        {/* Main Content Area (Flex: 1 makes it expand and push the footer down) */}
                        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>

                            <Routes>
                                {/* 1. Landing Page is now the default root */}
                                <Route path="/" element={<LandingPage />} />

                                {/* 2. Public Scanner & Login */}
                                <Route path="/verify" element={<PublicVerification />} />
                                <Route path="/login" element={<Login />} />
                                <Route path="/forgot-password" element={<ForgotPassword />} />
                                <Route path="/reset-password" element={<ResetPassword />} />

                                {/* Protected Route: Manufacturer Only */}
                                <Route
                                    path="/manufacturer"
                                    element={
                                        <ProtectedRoute allowedRoles={['MANUFACTURER']}>
                                            <ManufacturerRegistry />
                                        </ProtectedRoute>
                                    }
                                />

                                {/* Protected Route: ZAMMSA Hub Only */}
                                <Route
                                    path="/zammsa"
                                    element={
                                        <ProtectedRoute allowedRoles={['ZAMMSA']}>
                                            <ZammsaDashboard />
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

                                {/* NEW Protected Route: Dispense Screen (Added ? for optional hash) */}
                                <Route
                                    path="/pharmacy/dispense/:qrHash?"
                                    element={
                                        <ProtectedRoute allowedRoles={['PHARMACY']}>
                                            <DispenseItem />
                                        </ProtectedRoute>
                                    }
                                />

                                {/* NEW Protected Route: Mark Destroyed Screen (Added ? for optional hash) */}
                                <Route
                                    path="/pharmacy/destroy/:qrHash?"
                                    element={
                                        <ProtectedRoute allowedRoles={['PHARMACY', 'ZAMMSA']}>
                                            <MarkDestroyed />
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

                                {/* Protected Route: Supply Chain Operations (Outbound Transfer) */}
                                <Route
                                    path="/handover"
                                    element={
                                        <ProtectedRoute allowedRoles={['MANUFACTURER', 'ZAMMSA', 'PHARMACY']}>
                                            <CustodyTransfer />
                                        </ProtectedRoute>
                                    }
                                />

                                {/* UPDATED: Protected Route for Provenance Auditing (Added ? for optional hash) */}
                                <Route
                                    path="/audit/:qrHash?"
                                    element={
                                        <ProtectedRoute allowedRoles={['PHARMACY', 'ZAMMSA', 'ZAMRA']}>
                                            <ProvenanceAudit />
                                        </ProtectedRoute>
                                    }
                                />

                                {/* Protected Route: Receive Inventory (Inbound Verification) */}
                                {/* <Route
                                    path="/receive-inventory"
                                    element={
                                        <ProtectedRoute allowedRoles={['ZAMMSA', 'PHARMACY']}>
                                            <ReceiveInventory />
                                        </ProtectedRoute>
                                    }
                                />
                                */}

                                {/* Default Route redirects to Landing Page */}
                                <Route path="*" element={<Navigate to="/" replace />} />
                            </Routes>

                        </div>

                        {/* Global Footer rendered at the bottom of every page */}
                        <Footer />

                    </div>
                </AuthProvider>
            </Router>
        </QueryClientProvider>
    );
}

export default App;