// src/features/pharmacy/ReceiveInventory.jsx
import React, { useState, useEffect } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { apiClient } from '../../services/apiClient';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import styles from './PharmacyDashboard.module.css';

export default function ReceiveInventory() {
    const { user } = useAuth();
    const navigate = useNavigate();

    const [isScanning, setIsScanning] = useState(true);
    const [scanResult, setScanResult] = useState(null);
    const [isError, setIsError] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

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
                    // 1. Tries to pause to prevent double-scans (ignores error for image files)
                    try {
                        scanner.pause();
                    } catch (err) {
                        console.log("Static image detected, skipping pause.");
                    }

                    // 2. Clear the scanner UI from the screen
                    scanner.clear().then(() => {
                        // 3. Update React state and trigger your backend logic
                        setIsScanning(false);
                        handleScan(decodedText);
                    }).catch(console.error);
                },
                () => {} // ignore ongoing scan errors
            );
        }

        return () => {
            if (scanner) scanner.clear().catch(console.error);
        };
    }, [isScanning]);

    const handleScan = async (scannedHash) => {
        setIsLoading(true);
        try {
            // 1. Fetch the batch from your backend
            // NOTE: Make sure this URL matches your actual backend URL/Port
            const response = await apiClient.get(`/batches/hash/${scannedHash}`);
            const batchData = response.data;

            // 2. Get the logged-in pharmacy's ID
            const myPharmacyId = user?.participantId || 'PHARMACY-DEFAULT';

            // 3. Check if ZAMMSA actually transferred it to us
            if (batchData.currentOwnerId === myPharmacyId) {
                setScanResult(`Success! Custody of ${batchData.productName} verified. You are the official owner.`);
                setIsError(false);
            } else {
                setScanResult("Warning: The blockchain shows this batch is still owned by the distributor. Do not accept delivery.");
                setIsError(true);
            }
        } catch (error) {
            setIsError(true);
            if (error.response && error.response.status === 404) {
                setScanResult("Invalid QR Code: Batch not found in the national registry.");
            } else {
                setScanResult("A network error occurred while verifying the batch.");
            }
        } finally {
            setIsLoading(false);
        }
    };


    const resetScanner = () => {
        setScanResult(null);
        setIsError(false);
        setIsScanning(true);
    };

    return (
        <div className={styles.dashboardWrapper}>
            <main className={styles.mainContent}>
                <header className={styles.header}>
                    <div>
                        <h1 className={styles.title}>Verify Delivery</h1>
                        <p className={styles.subtitle}>Scan incoming batches to confirm blockchain custody</p>
                    </div>
                </header>

                <div className={styles.scannerZone}>
                    {isScanning && (
                        <div className={styles.scannerWrapper}>
                            <div id="reader"></div>
                        </div>
                    )}

                    {isLoading && (
                        <div className={styles.scannerPlaceholder}>
                            <span className="material-symbols-outlined" style={{ animation: 'spin 2s linear infinite', fontSize: '48px', color: 'var(--primary-container)' }}>
                                sync
                            </span>
                            <p style={{ marginTop: '16px', fontFamily: 'var(--font-ui)', fontWeight: '600' }}>Verifying Ledger...</p>
                        </div>
                    )}

                    {scanResult && (
                        <div className={styles.resultCard}>
                            <div className={`${styles.statusBadge} ${!isError ? styles.statusPassed : styles.statusDanger}`}>
                                <span className="material-symbols-outlined" style={{ fontSize: '18px', fontVariationSettings: "'FILL' 1" }}>
                                    {!isError ? 'verified' : 'warning'}
                                </span>
                                {!isError ? 'VERIFIED' : 'UNAUTHORIZED'}
                            </div>

                            <h3 style={{ fontSize: '18px', color: !isError ? 'var(--primary)' : '#d62828', marginBottom: '32px' }}>
                                {scanResult}
                            </h3>

                            <div style={{ display: 'flex', gap: '16px', justifyContent: 'center' }}>
                                <button className={styles.btnPrimary} onClick={() => navigate('/pharmacy')}>
                                    Return to Dashboard
                                </button>
                                <button
                                    style={{ background: 'none', border: 'none', color: 'var(--on-surface-variant)', fontWeight: '600', cursor: 'pointer' }}
                                    onClick={resetScanner}
                                >
                                    Scan Another
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}