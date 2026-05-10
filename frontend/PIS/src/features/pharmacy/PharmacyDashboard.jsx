// src/features/pharmacy/PharmacyDashboard.jsx
import React, { useState, useEffect } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { useMutation, useQuery } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import { useNavigate } from 'react-router-dom';
import styles from './PharmacyDashboard.module.css';

export default function PharmacyDashboard() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [isScanning, setIsScanning] = useState(false);
    const [scanMode, setScanMode] = useState(null);
    const [activeTab, setActiveTab] = useState('operations');

    // ── Security Alerts Query ──
    const { data: alerts } = useQuery({
        queryKey: ['securityAlerts'],
        queryFn: async () => (await apiClient.get('/analytics/suspicious-scans')).data,
        enabled: activeTab === 'alerts'
    });

    // ── Mutations ──
    const verifyMutation = useMutation({
        mutationFn: async (qrHash) => {
            const response = await apiClient.post('/verifications', {
                qrHash: qrHash,
                deviceFingerprint: navigator.userAgent,
                geoLocation: 'Pharmacy POS'
            });
            return response.data;
        }
    });

    const dispenseMutation = useMutation({
        mutationFn: async (batchNumber) => {
            const response = await apiClient.post('/operations/dispense', {
                batchNumber: batchNumber,
                pharmacyId: user?.participantId
            });
            return response.data;
        },
        onSuccess: () => {
            alert('Success: Ledger updated. Medication dispensed.');
            closeScanner();
        }
    });

    const destroyMutation = useMutation({
        mutationFn: async (batchNumber) => {
            const response = await apiClient.post('/operations/destroy', {
                batchNumber: batchNumber,
                reason: 'Expired/Damaged',
                participantId: user?.participantId
            });
            return response.data;
        },
        onSuccess: () => {
            alert('Blockchain updated: Batch marked as DESTROYED.');
            closeScanner();
        }
    });

    // ── QR Scanner Engine ──
    useEffect(() => {
        let scanner;
        if (isScanning) {
            scanner = new Html5QrcodeScanner("reader", {
                fps: 10,
                qrbox: { width: 250, height: 250 }
            }, false);

            scanner.render(
                (decodedText) => {
                    // 1. Try to pause to prevent double-scans
                    try {
                        scanner.pause();
                    } catch (err) {
                        console.log("Static image detected, skipping pause.");
                    }

                    // 2. Clear the scanner UI from the screen
                    scanner.clear().then(() => {
                        // 3. Update React state and route the logic
                        setIsScanning(false);
                        if (scanMode === 'DISPENSE') verifyMutation.mutate(decodedText);
                        if (scanMode === 'DESTROY') destroyMutation.mutate(decodedText);
                        if (scanMode === 'AUDIT') navigate(`/provenance/${decodedText}`);
                    }).catch(console.error);
                },
                () => {} // ignore ongoing scan errors
            );
        }
        return () => scanner?.clear().catch(() => {});
    }, [isScanning, scanMode]);


    const handleActionClick = (mode) => {
        if (mode === 'RECEIVE') return navigate('/receive-inventory');
        if (mode === 'RETURN') return navigate('/handover');
        setScanMode(mode);
        setIsScanning(true);
    };

    const closeScanner = () => {
        setIsScanning(false);
        setScanMode(null);
        verifyMutation.reset();
    };

    const scanResult = verifyMutation.data;
    const isSafe = scanResult?.status === 'PASSED' || scanResult?.status === 'AUTHENTIC';

    return (
        <div className={styles.dashboardWrapper}>
            <aside className={styles.sidebar}>
                <div className={styles.sidebarBrand}>
                    <span className="material-symbols-outlined">verified</span>
                    <span>Blockchain-Based Pharmaceutical Integrity System</span>
                </div>
                <div className={`${styles.navItem} ${activeTab === 'operations' && styles.activeNav}`} onClick={() => setActiveTab('operations')}>
                    <span className="material-symbols-outlined">point_of_sale</span> Operations
                </div>
                <div className={`${styles.navItem} ${activeTab === 'alerts' && styles.activeNav}`} onClick={() => setActiveTab('alerts')}>
                    <span className="material-symbols-outlined">warning</span> Security Alerts
                </div>
                <button onClick={logout} className={`${styles.navItem} ${styles.logoutBtn}`}>
                    <span className="material-symbols-outlined">logout</span> Sign Out
                </button>
            </aside>

            <main className={styles.mainContent}>
                <header className={styles.header}>
                    <h1 className={styles.title}>
                        {activeTab === 'operations' ? "Pharmacy Hub" : "Security Monitoring"}
                    </h1>
                    <p className={styles.subtitle}>{user?.username} • Facility Node</p>
                </header>

                {activeTab === 'operations' && (
                    <div className={styles.dashboardGrid}>
                        <div className={styles.actionCard} onClick={() => handleActionClick('RECEIVE')}>
                            <div className={`${styles.iconCircle} ${styles.bgPrimaryLight}`}><span className="material-symbols-outlined">local_shipping</span></div>
                            <h3>Receive Inventory</h3>
                        </div>
                        <div className={styles.actionCard} onClick={() => handleActionClick('DISPENSE')}>
                            <div className={`${styles.iconCircle} ${styles.bgSecondaryLight}`}><span className="material-symbols-outlined">prescriptions</span></div>
                            <h3>Dispense Item</h3>
                        </div>
                        <div className={styles.actionCard} onClick={() => handleActionClick('RETURN')}>
                            <div className={styles.iconCircle} style={{backgroundColor: '#FFEFEF', color: '#D62828'}}><span className="material-symbols-outlined">assignment_return</span></div>
                            <h3>Return Stock</h3>
                        </div>
                        <div className={styles.actionCard} onClick={() => handleActionClick('AUDIT')}>
                            <div className={styles.iconCircle} style={{backgroundColor: '#E8F5E9', color: '#2E7D32'}}><span className="material-symbols-outlined">history</span></div>
                            <h3>Provenance Audit</h3>
                        </div>
                        <div className={styles.actionCard} onClick={() => handleActionClick('DESTROY')}>
                            <div className={styles.iconCircle} style={{backgroundColor: '#F5F5F5', color: '#424242'}}><span className="material-symbols-outlined">delete_forever</span></div>
                            <h3>Mark Destroyed</h3>
                        </div>

                        {/* Reusable Scanner Zone */}
                        {isScanning && (
                            <div className={styles.scannerZone}>
                                <h2 style={{textAlign:'center', marginBottom: '16px'}}>Scanning for {scanMode}...</h2>
                                <div className={styles.scannerWrapper}><div id="reader"></div></div>
                                <div style={{textAlign:'center', marginTop: '16px'}}><button className={styles.btnPrimary} onClick={closeScanner}>Cancel</button></div>
                            </div>
                        )}

                        {/* RESTORED: Result View UI */}
                        {scanResult && (
                            <div className={styles.scannerZone}>
                                <div className={styles.resultCard}>
                                    <div className={`${styles.statusBadge} ${isSafe ? styles.statusPassed : styles.statusDanger}`}>
                                        {isSafe ? 'AUTHENTIC' : 'FLAGGED'}
                                    </div>
                                    <h3>{scanResult.productName}</h3>
                                    <p>Batch: <strong>{scanResult.batchNumber}</strong></p>

                                    {isSafe ? (
                                        <button className={styles.btnPrimary} onClick={() => dispenseMutation.mutate(scanResult.batchNumber)}>
                                            Confirm Dispense
                                        </button>
                                    ) : (
                                        <p style={{color: '#d62828'}}>This batch is unsafe for dispensing.</p>
                                    )}
                                    <button onClick={closeScanner} style={{marginTop: '16px', background: 'none', border: 'none', color: 'blue', cursor: 'pointer'}}>Close</button>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'alerts' && (
                    <div className={styles.scannerZone}>
                        <h3>Regional Security Alerts</h3>
                        {alerts?.length > 0 ? (
                            alerts.map((alert, i) => (
                                <div key={i} className={styles.resultCard} style={{marginBottom: '10px', borderLeft: '5px solid red'}}>
                                    <strong>{alert.productName}</strong> - {alert.riskLevel} Risk
                                    <p>Multiple scans detected for Batch {alert.batchNumber}</p>
                                </div>
                            ))
                        ) : <p>No suspicious patterns detected.</p>}
                    </div>
                )}
            </main>
        </div>
    );
}