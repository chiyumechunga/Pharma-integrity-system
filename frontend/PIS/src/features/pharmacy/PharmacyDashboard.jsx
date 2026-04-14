import React, { useState, useEffect } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { useMutation } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import { useNavigate } from 'react-router-dom';
import styles from './PharmacyDashboard.module.css';

export default function PharmacyDashboard() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [isScanning, setIsScanning] = useState(false);
    const [scanMode, setScanMode] = useState(null); // 'RECEIVE' or 'DISPENSE'

    // 1. Verify Authentication before Dispensing
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

    // 2. Finalize Dispensing (Trigger Chaincode DispenseBatch)
    const dispenseMutation = useMutation({
        mutationFn: async (batchNumber) => {
            // Assuming OperationController or BatchController handles the dispense action
            const response = await apiClient.post('/operations/dispense', {
                batchNumber: batchNumber,
                pharmacyId: user?.participantId || 'PHARMACY-DEFAULT'
            });
            return response.data;
        },
        onSuccess: () => {
            alert('Medication successfully dispensed and ledger updated.');
            verifyMutation.reset();
            setScanMode(null);
        },
        onError: (err) => {
            alert(`Dispense failed: ${err.response?.data?.message || err.message}`);
        }
    });

    useEffect(() => {
        let scanner;
        if (isScanning) {
            scanner = new Html5QrcodeScanner("reader", {
                fps: 10,
                qrbox: { width: 250, height: 250 },
                aspectRatio: 1.0
            }, false);

            scanner.render(
                (decodedText) => {
                    scanner.pause();
                    setIsScanning(false);
                    if (scanMode === 'DISPENSE') {
                        verifyMutation.mutate(decodedText);
                    }
                    scanner.clear();
                },
                () => {} // ignore scan errors
            );
        }

        return () => {
            if (scanner) scanner.clear().catch(console.error);
        };
    }, [isScanning, scanMode]);

    const handleStartScan = (mode) => {
        if (mode === 'RECEIVE') {
            // Reroute to our existing Custody Transfer module for logistics handling
            navigate('/handover');
        } else {
            setScanMode(mode);
            setIsScanning(true);
        }
    };

    const scanResult = verifyMutation.data;
    const isSafeToDispense = scanResult?.status === 'PASSED';

    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <div>
                    <h1 className={styles.title}>Pharmacy Operations</h1>
                    <p className={styles.subtitle}>Inventory Reception & Point of Sale</p>
                </div>
                <div className={styles.userProfile}>
                    <span className="material-symbols-outlined">local_pharmacy</span>
                    <span>{user?.username || 'Facility Pharmacist'}</span>
                    <button onClick={logout} className={styles.logoutBtn} title="Secure Logout">
                        <span className="material-symbols-outlined">logout</span>
                    </button>
                </div>
            </header>

            <div className={styles.dashboardGrid}>
                {/* Inventory Reception Link */}
                <div className={styles.actionCard} style={{ cursor: 'pointer' }} onClick={() => handleStartScan('RECEIVE')}>
                    <div className={`${styles.iconCircle} ${styles.bgPrimaryLight}`}>
                        <span className="material-symbols-outlined" style={{ fontSize: '32px' }}>local_shipping</span>
                    </div>
                    <h3 style={{ fontSize: '20px', color: 'var(--primary)', marginBottom: '8px' }}>Receive Inventory</h3>
                    <p style={{ fontFamily: 'var(--font-ui)', color: 'var(--on-surface-variant)', fontSize: '14px' }}>
                        Confirm receipt of batches from distributors and update the permanent custody chain.
                    </p>
                </div>

                {/* Dispense Trigger */}
                <div className={styles.actionCard} style={{ cursor: 'pointer' }} onClick={() => handleStartScan('DISPENSE')}>
                    <div className={`${styles.iconCircle} ${styles.bgSecondaryLight}`}>
                        <span className="material-symbols-outlined" style={{ fontSize: '32px' }}>prescriptions</span>
                    </div>
                    <h3 style={{ fontSize: '20px', color: 'var(--primary)', marginBottom: '8px' }}>Dispense Medication</h3>
                    <p style={{ fontFamily: 'var(--font-ui)', color: 'var(--on-surface-variant)', fontSize: '14px' }}>
                        Scan QR at Point of Sale. Verifies authenticity before marking product as sold to patient.
                    </p>
                </div>

                {/* Active Scanner & Verification Zone */}
                {(isScanning || scanResult || verifyMutation.isPending) && (
                    <div className={styles.scannerZone}>
                        <h2 style={{ fontSize: '18px', color: 'var(--primary)', marginBottom: '24px', textAlign: 'center' }}>
                            {scanResult ? 'Authenticity Verification Result' : 'Point of Sale Scanner'}
                        </h2>

                        {isScanning && (
                            <div className={styles.scannerWrapper}>
                                <div id="reader"></div>
                            </div>
                        )}

                        {verifyMutation.isPending && (
                            <div className={styles.scannerPlaceholder}>
                                <span className="material-symbols-outlined" style={{ animation: 'spin 2s linear infinite', fontSize: '48px', color: 'var(--primary-container)' }}>
                                    sync
                                </span>
                                <p style={{ marginTop: '16px', fontFamily: 'var(--font-ui)', fontWeight: '600' }}>Querying Ledger...</p>
                            </div>
                        )}

                        {scanResult && (
                            <div className={styles.resultCard}>
                                <div className={`${styles.statusBadge} ${isSafeToDispense ? styles.statusPassed : styles.statusDanger}`}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px', fontVariationSettings: "'FILL' 1" }}>
                                        {isSafeToDispense ? 'verified' : 'warning'}
                                    </span>
                                    {scanResult.status}
                                </div>

                                <h3 style={{ fontSize: '22px', color: 'var(--primary)', marginBottom: '16px' }}>
                                    {scanResult.productName || 'Unknown Product'}
                                </h3>

                                <p style={{ fontFamily: 'var(--font-ui)', fontSize: '14px', color: 'var(--on-surface-variant)', marginBottom: '32px' }}>
                                    Batch No: <strong>{scanResult.batchNumber}</strong><br/>
                                    Expiry: {scanResult.expiryDate}
                                </p>

                                {/* Strict Point of Sale Gatekeeper Logic */}
                                {isSafeToDispense ? (
                                    <button
                                        className={styles.btnPrimary}
                                        onClick={() => dispenseMutation.mutate(scanResult.batchNumber)}
                                        disabled={dispenseMutation.isPending}
                                    >
                                        <span className="material-symbols-outlined">check_circle</span>
                                        {dispenseMutation.isPending ? 'Dispensing...' : 'Confirm Dispense'}
                                    </button>
                                ) : (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center' }}>
                                        <button className={styles.btnDanger} disabled>
                                            <span className="material-symbols-outlined">block</span>
                                            Dispense Blocked
                                        </button>
                                        <p style={{ color: '#d62828', fontSize: '13px', fontWeight: '600' }}>
                                            This medication is unsafe and cannot be dispensed. Hand over to facility management immediately.
                                        </p>
                                    </div>
                                )}

                                <div style={{ marginTop: '24px' }}>
                                    <button
                                        style={{ background: 'none', border: 'none', color: 'var(--primary-container)', fontWeight: '600', cursor: 'pointer' }}
                                        onClick={() => { verifyMutation.reset(); setScanMode(null); }}
                                    >
                                        Cancel & Return
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}