import React, { useEffect, useState } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { Link } from 'react-router-dom';
import styles from './PublicVerification.module.css';

export default function PublicVerification() {
    const [isScanning, setIsScanning] = useState(false);
    const [scannedHash, setScannedHash] = useState(null);

    // 1. Fire-and-forget: Grab LIVE location and log the scan to the backend
    useEffect(() => {
        if (scannedHash) {
            const logVerification = (locationString) => {
                apiClient.post('/verifications', {
                    qrHash: scannedHash,
                    deviceFingerprint: navigator.userAgent,
                    geoLocation: locationString
                }).catch(err => console.error("Audit log failed:", err));
            };

            // Request live GPS coordinates from the user's device
            if ("geolocation" in navigator) {
                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        // Success: Format the coordinates
                        const lat = position.coords.latitude.toFixed(7);
                        const lng = position.coords.longitude.toFixed(7);
                        logVerification(`${lat},${lng}`);
                    },
                    (error) => {
                        // Failed or Denied by user
                        console.warn("Geolocation denied or failed.", error);
                        logVerification('Location Denied (Public)');
                    },
                    { timeout: 10000, maximumAge: 60000 } // Wait up to 10 seconds for a GPS lock
                );
            } else {
                // Browser doesn't support GPS
                logVerification('Location Unavailable (Public)');
            }
        }
    }, [scannedHash]);

    // 2. Fetch the rich Provenance Data from Spring Boot
    const { data: scanResult, isPending, isError } = useQuery({
        queryKey: ['provenance', scannedHash],
        queryFn: async () => {
            const response = await apiClient.get(`/provenance/${scannedHash}`);
            return response.data;
        },
        enabled: !!scannedHash, // Only run this when we have a hash
        retry: false // Don't retry if it's a 404 (Counterfeit)
    });

    // 3. Scanner Engine
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
                    setIsScanning(false);
                    setScannedHash(decodedText);
                    scanner.clear();
                },
                () => { /* ignore background scan errors */ }
            );
        }

        return () => {
            if (scanner) scanner.clear().catch(console.error);
        };
    }, [isScanning]);

    const resetScanner = () => {
        setScannedHash(null);
        setIsScanning(true);
    };

    // Helper for Banner Styling
    const getStatusStyle = (status) => {
        if (status === 'AUTHENTIC') return styles.statusAuthentic;
        if (status === 'COUNTERFEIT' || status === 'RECALLED') return styles.statusCounterfeit;
        if (status === 'EXPIRED') return styles.statusExpired;
        return styles.statusUnknown;
    };

    return (
        <div className={styles.container}>

            {/* Back to Home Navigation */}
            {!isScanning && !scannedHash && (
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
            {!isScanning && !scannedHash && (
                <>
                    <div className={styles.scannerPlaceholder}>
                        <span className="material-symbols-outlined" style={{ fontSize: '64px', color: 'rgba(0, 70, 85, 0.2)' }}>qr_code_scanner</span>
                    </div>
                    <button className={styles.btnPrimary} onClick={() => setIsScanning(true)}>
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

            {/* State 3: Loading Data */}
            {scannedHash && isPending && (
                <div className={styles.messageZone}>
                    <span className="material-symbols-outlined" style={{ fontSize: '48px', color: 'var(--primary)', animation: 'spin 2s linear infinite' }}>hourglass_empty</span>
                    <p style={{ marginTop: '16px', fontWeight: '600', color: 'var(--primary)' }}>Querying National Ledger...</p>
                </div>
            )}

            {/* State 4: Error / Counterfeit (404 Not Found) */}
            {scannedHash && isError && (
                <div className={styles.errorCard}>
                    <span className="material-symbols-outlined" style={{ fontSize: '48px', color: '#d62828' }}>gpp_bad</span>
                    <h2 style={{ color: '#d62828', marginTop: '12px' }}>WARNING</h2>
                    <p>UNREGISTERED / POTENTIAL COUNTERFEIT</p>
                    <p style={{ fontSize: '12px', color: '#666', marginTop: '8px', marginBottom: '24px' }}>
                        This QR code does not exist on the national registry. Do not consume this product.
                    </p>
                    <button className={styles.btnSecondary} onClick={resetScanner}>Scan Another Product</button>
                </div>
            )}

            {/* State 5: Success / Provenance View */}
            {scanResult && !isError && (
                <div className={styles.resultCard}>

                    {/* Dynamic Status Banner */}
                    <div className={`${styles.statusBanner} ${getStatusStyle(scanResult.verificationStatus)}`}>
                        <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>
                            {scanResult.verificationStatus === 'AUTHENTIC' ? 'verified_user' : 'warning'}
                        </span>
                        {scanResult.verificationStatus}
                    </div>

                    <h3 style={{ textAlign: 'center', fontSize: '20px', color: 'var(--primary)', marginBottom: '24px' }}>
                        {scanResult.productDetails.genericName}
                    </h3>

                    <div className={styles.detailRow}>
                        <span className={styles.detailLabel}>Batch No.</span>
                        <span className={styles.detailValue}>{scanResult.productDetails.batchNumber}</span>
                    </div>
                    <div className={styles.detailRow}>
                        <span className={styles.detailLabel}>Manufacturer</span>
                        <span className={styles.detailValue}>{scanResult.productDetails.manufacturer}</span>
                    </div>
                    <div className={styles.detailRow}>
                        <span className={styles.detailLabel}>Expiry Date</span>
                        <span className={styles.detailValue}>{scanResult.productDetails.expiryDate}</span>
                    </div>

                    {/* Provenance Timeline Section */}
                    <div className={styles.timelineSection}>
                        <h4 className={styles.timelineTitle}>Chain of Custody</h4>

                        {scanResult.provenanceTimeline && scanResult.provenanceTimeline.length > 0 ? (
                            [...scanResult.provenanceTimeline].reverse().map((event, index) => {
                                const date = new Date(event.timestamp).toLocaleString('en-GB', {
                                    day: 'numeric', month: 'short', year: 'numeric',
                                    hour: '2-digit', minute: '2-digit'
                                });

                                return (
                                    <div className={styles.timelineItem} key={index}>
                                        <div className={styles.timelineConnector}></div>
                                        <div className={styles.timelineDot}></div>
                                        <div className={styles.timelineContent}>
                                            <span className={styles.eventTime}>{date}</span>
                                            <h4 className={styles.eventType}>{event.eventType.replace('_', ' ')}</h4>

                                            {event.eventType === 'MANUFACTURED' ? (
                                                <p className={styles.participants}>Origin: {event.fromParticipant || event.toParticipant}</p>
                                            ) : event.eventType === 'TRANSFER' ? (
                                                <p className={styles.participants}>
                                                    From: {event.fromParticipant} <br/>
                                                    To: <strong>{event.toParticipant}</strong>
                                                </p>
                                            ) : (
                                                <p className={styles.participants}>Location: {event.toParticipant}</p>
                                            )}

                                            <p className={styles.eventTx}>TX: {event.blockchainTxId?.substring(0, 16)}...</p>
                                        </div>
                                    </div>
                                );
                            })
                        ) : (
                            <p style={{ fontSize: '14px', color: 'var(--on-surface-variant)' }}>
                                No journey history available for this asset.
                            </p>
                        )}
                    </div>

                    <button className={styles.btnPrimary} style={{ marginTop: '32px' }} onClick={resetScanner}>
                        <span className="material-symbols-outlined">qr_code_scanner</span>
                        Scan Another Package
                    </button>
                </div>
            )}
        </div>
    );
}