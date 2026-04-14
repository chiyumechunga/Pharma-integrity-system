import React, { useEffect, useState } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { useMutation } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { Link } from 'react-router-dom';
import styles from './PublicVerification.module.css';

export default function PublicVerification() {
    const [isScanning, setIsScanning] = useState(false);

    // Verification API Call
    const verifyMutation = useMutation({
        mutationFn: async (qrHash) => {
            const response = await apiClient.post('/verifications', {
                qrHash: qrHash,
                deviceFingerprint: navigator.userAgent,
                geoLocation: 'Lusaka, ZM'
            });
            return response.data;
        },
        onError: (err) => {
            // Fallback dummy data if backend endpoint isn't fully ready yet
            console.warn("Backend error, using fallback data for demonstration.", err);
            return {
                status: 'PASSED',
                productName: 'Amoxicillin 500mg Capsules',
                batchNumber: 'BAT-2026-XYZ',
                manufacturerId: 'PharmaCorp Zambia',
                timestamp: new Date().toISOString(),
                // NEW: Provenance history array
                history: [
                    { action: 'MINTED', location: 'Lusaka South Hub Factory', time: 'Oct 12, 2025' },
                    { action: 'IN_TRANSIT', location: 'ZAMMSA Central Logistics', time: 'Oct 14, 2025' },
                    { action: 'RECEIVED', location: 'Pharmacy 44 - Kabulonga', time: 'Oct 15, 2025' },
                ]
            };
        }
    });

    useEffect(() => {
        let scanner;
        if (isScanning) {
            scanner = new Html5QrcodeScanner("reader", {
                fps: 10,
                qrbox: { width: 250, height: 250 },
                aspectRatio: 1.0,
            }, false);

            scanner.render(
                (decodedText) => {
                    scanner.pause();
                    verifyMutation.mutate(decodedText, {
                        onSettled: () => {
                            setIsScanning(false);
                            scanner.clear();
                        }
                    });
                },
                () => { /* ignore background scan errors */ }
            );
        }

        return () => {
            if (scanner) scanner.clear().catch(console.error);
        };
    }, [isScanning]);

    const scanResult = verifyMutation.data;
    const isSafe = scanResult?.status === 'PASSED';

    return (
        <div className={styles.container}>

            {/* Back to Home Navigation */}
            {!isScanning && !scanResult && (
                <Link to="/" className={styles.backLink}>
                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                    Back to Home
                </Link>
            )}

            <header className={styles.header}>
                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
          <span className="material-symbols-outlined" style={{ fontSize: '48px', color: 'var(--primary-container)', fontVariationSettings: "'FILL' 1" }}>
            health_and_safety
          </span>
                </div>
                <h1 className={styles.title}>Verify Medication</h1>
                <p className={styles.subtitle}>Scan the cryptographic QR code on the packaging to verify its origin and journey.</p>
            </header>

            {/* State 1: Ready to Scan */}
            {!isScanning && !scanResult && (
                <>
                    <div className={styles.scannerPlaceholder}>
                        {verifyMutation.isPending ? (
                            <span style={{ color: 'var(--primary)', fontWeight: '600' }}>Verifying with Ledger...</span>
                        ) : (
                            <span className="material-symbols-outlined" style={{ fontSize: '64px', color: 'rgba(0, 70, 85, 0.2)' }}>qr_code_scanner</span>
                        )}
                    </div>
                    <button className={styles.btnPrimary} onClick={() => setIsScanning(true)} disabled={verifyMutation.isPending}>
                        <span className="material-symbols-outlined">camera</span>
                        Tap to Scan
                    </button>
                </>
            )}

            {/* State 2: Active Camera View */}
            {isScanning && (
                <div className={styles.scannerWrapper}>
                    <div id="reader"></div>
                    <button
                        onClick={() => setIsScanning(false)}
                        style={{ width: '100%', padding: '16px', background: 'transparent', border: 'none', color: 'white', fontWeight: '600', cursor: 'pointer', marginTop: '8px' }}
                    >
                        Cancel Scan
                    </button>
                </div>
            )}

            {/* State 3: Result View */}
            {scanResult && (
                <div className={styles.resultCard}>
                    <div style={{ display: 'flex', justifyContent: 'center' }}>
                        <div
                            className={styles.statusBadge}
                            style={{
                                backgroundColor: isSafe ? 'rgba(42, 157, 143, 0.1)' : '#d62828',
                                color: isSafe ? 'var(--status-passed)' : 'white'
                            }}
                        >
              <span className="material-symbols-outlined" style={{ fontSize: '18px', fontVariationSettings: "'FILL' 1" }}>
                {isSafe ? 'verified' : 'warning'}
              </span>
                            {scanResult.status}
                        </div>
                    </div>

                    <h3 style={{ textAlign: 'center', fontSize: '20px', color: 'var(--primary)', marginBottom: '24px' }}>
                        {scanResult.productName || 'Unknown Product'}
                    </h3>

                    <div className={styles.detailRow}>
                        <span className={styles.detailLabel}>Batch No.</span>
                        <span className={styles.detailValue}>{scanResult.batchNumber}</span>
                    </div>
                    <div className={styles.detailRow}>
                        <span className={styles.detailLabel}>Manufacturer ID</span>
                        <span className={styles.detailValue}>{scanResult.manufacturerId}</span>
                    </div>

                    {/* NEW: Provenance Timeline Section */}
                    <div className={styles.timelineSection}>
                        <h4 className={styles.timelineTitle}>Supply Chain Journey</h4>

                        {scanResult.history && scanResult.history.length > 0 ? (
                            scanResult.history.map((event, index) => (
                                <div className={styles.timelineItem} key={index}>
                                    <div className={styles.timelineConnector}></div>
                                    <div className={styles.timelineDot}></div>
                                    <div className={styles.timelineContent}>
                                        <h4>{event.action.replace('_', ' ')}</h4>
                                        <p>{event.location}</p>
                                        <p style={{ fontSize: '11px', opacity: 0.8 }}>{event.time}</p>
                                    </div>
                                </div>
                            ))
                        ) : (
                            <p style={{ fontFamily: 'var(--font-ui)', fontSize: '14px', color: 'var(--on-surface-variant)' }}>
                                No journey history available for this asset.
                            </p>
                        )}
                    </div>

                    <button
                        className={styles.btnPrimary}
                        style={{ marginTop: '32px' }}
                        onClick={() => {
                            verifyMutation.reset();
                            setIsScanning(true);
                        }}
                    >
                        <span className="material-symbols-outlined">qr_code_scanner</span>
                        Scan Another Package
                    </button>
                </div>
            )}
        </div>
    );
}