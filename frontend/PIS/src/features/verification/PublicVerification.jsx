    import React, { useState, useEffect, startTransition } from 'react';
    import { Html5QrcodeScanner } from 'html5-qrcode';
    import { useQuery, useMutation } from '@tanstack/react-query';
    import { apiClient } from '../../services/apiClient';
    import { Link } from 'react-router-dom';
    import styles from './PublicVerification.module.css';

    export default function PublicVerification() {
        const [isScanning, setIsScanning] = useState(false);
        const [scannedHash, setScannedHash] = useState(null);

        // Reporting Form State
        const [showReportForm, setShowReportForm] = useState(false);
        const [reportForm, setReportForm] = useState({
            reporterName: '',
            reporterPhoneOrEmail: '',
            locationInfo: '',
            incidentDescription: '',
            issueType: 'SIDE_EFFECTS' // Default
        });

        // Unified Data Fetching: MERGING VERIFICATION + PROVENANCE
        const { data: scanResult, isPending, isError } = useQuery({
            queryKey: ['provenance', scannedHash],
            queryFn: async () => {
                const getLocation = () => new Promise((resolve) => {
                    if ("geolocation" in navigator) {
                        navigator.geolocation.getCurrentPosition(
                            (position) => resolve(`${position.coords.latitude.toFixed(7)},${position.coords.longitude.toFixed(7)}`),
                            () => resolve('Location Denied (Public)'),
                            { timeout: 4000, maximumAge: 60000 }
                        );
                    } else {
                        resolve('Location Unavailable (Public)');
                    }
                });

                const locString = await getLocation();
                let verificationData = null;

                // STEP 1: Verify the drug and get the AUTHENTIC/COUNTERFEIT status
                try {
                    const verifyRes = await apiClient.post('/verifications', {
                        qrHash: scannedHash,
                        deviceFingerprint: navigator.userAgent,
                        geoLocation: locString
                    });
                    verificationData = verifyRes.data;

                    if (!verificationData.isValid) {
                        throw new Error("Invalid or Counterfeit Product");
                    }
                } catch (err) {
                    console.warn("Verification threw an error, treating as invalid/counterfeit", err);
                    throw new Error("Counterfeit");
                }

                // STEP 2: Fetch the Provenance Chain of Custody
                const provRes = await apiClient.get(`/provenance/${scannedHash}`);
                const provenanceData = provRes.data;

                // STEP 3: Map the Spring Boot DTOs into the format React expects!
                return {
                    verificationStatus: verificationData.isValid ? 'AUTHENTIC' : 'WARNING',
                    message: verificationData.message,
                    productDetails: {
                        genericName: provenanceData.productDetails.genericName,
                        batchNumber: provenanceData.productDetails.batchNumber,
                        manufacturer: provenanceData.productDetails.manufacturer,
                        expiryDate: provenanceData.productDetails.expiryDate,
                        currentStatus: provenanceData.productDetails.currentStatus,
                        serialNumber: provenanceData.productDetails.serialNumber // Dynamically populated
                    },
                    provenanceTimeline: provenanceData.provenanceTimeline || [] // Updated variable name
                };
            },
            enabled: !!scannedHash,
            retry: false
        });

        // Mutation for Submitting the Incident Report
        const reportMutation = useMutation({
            mutationFn: async (reportData) => {
                return await apiClient.post('/verifications/report', reportData);
            },
            onSuccess: () => {
                alert("Report successfully submitted to ZAMRA authorities. Thank you.");
                setShowReportForm(false);
                setReportForm({ reporterName: '', reporterPhoneOrEmail: '', locationInfo: '', incidentDescription: '', issueType: 'SIDE_EFFECTS' });
            },
            onError: () => {
                alert("Failed to submit report. Please check your connection.");
            }
        });

        // Scanner Engine
        useEffect(() => {
            let scanner;
            if (isScanning) {
                scanner = new Html5QrcodeScanner("reader", {
                    fps: 10,
                    qrbox: (videoWidth, videoHeight) => {
                        const minEdge = Math.min(videoWidth, videoHeight);
                        return { width: minEdge * 0.8, height: minEdge * 0.8 };
                    }
                }, false);

                scanner.render(
                    (decodedText) => {
                        try { scanner.pause(); } catch (err) {}
                        scanner.clear().then(() => {
                            startTransition(() => {
                                setIsScanning(false);
                                setScannedHash(decodedText);
                            });
                        }).catch(console.error);
                    },
                    () => {}
                );
            }
            return () => { if (scanner) scanner.clear().catch(console.error); };
        }, [isScanning]);

        const resetScanner = () => {
            setScannedHash(null);
            setIsScanning(true);
            setShowReportForm(false);
        };

        const handleReportSubmit = (e) => {
            e.preventDefault();
            reportMutation.mutate({ ...reportForm, qrHash: scannedHash });
        };

        const getStatusStyle = (status) => {
            if (status === 'AUTHENTIC') return styles.statusAuthentic;
            if (status === 'COUNTERFEIT' || status === 'RECALLED') return styles.statusCounterfeit;
            if (status === 'EXPIRED') return styles.statusExpired;
            return styles.statusUnknown;
        };

        return (
            <div className={styles.container}>
                {!isScanning && !showReportForm && (
                    <Link to="/" className={styles.backLink}>
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                        Back to Home
                    </Link>
                )}

                <header className={styles.header}>
                    <div style={{ display: 'flex', justifyContent: 'center', margin: '0 auto 16px' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '48px', color: 'var(--primary-container)', fontVariationSettings: "'FILL' 1" }}>
                            health_and_safety
                        </span>
                    </div>
                    <h1 className={styles.title}>Verify Medication</h1>
                    <p className={styles.subtitle}>Scan the cryptographic QR code on the packaging to verify its origin and journey.</p>
                </header>

                {/* --- REPORTING FORM OVERLAY --- */}
                {showReportForm && (
                    <div className={styles.resultCard}>
                        <h3 style={{ color: '#d62828', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span className="material-symbols-outlined">report</span> Report an Issue
                        </h3>
                        <form onSubmit={handleReportSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                            <select className={styles.inputField} value={reportForm.issueType} onChange={e => setReportForm({...reportForm, issueType: e.target.value})}>
                                <option value="SIDE_EFFECTS">Abnormal Side Effects</option>
                                <option value="COUNTERFEIT">Suspected Counterfeit</option>
                                <option value="DAMAGED">Damaged / Tampered Packaging</option>
                                <option value="MISSING_QR">No QR Code Found</option>
                                <option value="OTHER">Other</option>
                            </select>
                            <input type="text" placeholder="Where did you buy this? (Pharmacy Name)" className={styles.inputField} required value={reportForm.locationInfo} onChange={e => setReportForm({...reportForm, locationInfo: e.target.value})} />
                            <input type="text" placeholder="Your Name (Optional)" className={styles.inputField} value={reportForm.reporterName} onChange={e => setReportForm({...reportForm, reporterName: e.target.value})} />
                            <input type="text" placeholder="Phone or Email (Optional)" className={styles.inputField} value={reportForm.reporterPhoneOrEmail} onChange={e => setReportForm({...reportForm, reporterPhoneOrEmail: e.target.value})} />
                            <textarea placeholder="Describe the issue..." className={styles.inputField} rows="4" required value={reportForm.incidentDescription} onChange={e => setReportForm({...reportForm, incidentDescription: e.target.value})}></textarea>
                            <button type="submit" className={styles.btnPrimary} style={{ background: '#d62828' }} disabled={reportMutation.isPending}>
                                {reportMutation.isPending ? 'Submitting...' : 'Submit Report to Authorities'}
                            </button>
                            <button type="button" className={styles.btnSecondary} onClick={() => setShowReportForm(false)}>Cancel</button>
                        </form>
                    </div>
                )}

                {/* State 1: Ready to Scan */}
                {!isScanning && !scannedHash && !showReportForm && (
                    <>
                        <div className={styles.scannerPlaceholder}>
                            <span className="material-symbols-outlined" style={{ fontSize: '64px', color: 'rgba(0, 70, 85, 0.2)' }}>qr_code_scanner</span>
                        </div>
                        <button className={styles.btnPrimary} onClick={() => setIsScanning(true)}>
                            <span className="material-symbols-outlined">camera</span>
                            Tap to Scan
                        </button>
                        <button onClick={() => { setReportForm({...reportForm, issueType: 'MISSING_QR'}); setShowReportForm(true); }} style={{ marginTop: '24px', background: 'transparent', border: 'none', color: '#666', textDecoration: 'underline', cursor: 'pointer' }}>
                            Product missing a QR Code? Report it here.
                        </button>
                    </>
                )}

                {/* State 2: Active Camera View */}
                {isScanning && !showReportForm && (
                    <div className={styles.scannerWrapper}>
                        <div id="reader"></div>
                        <button onClick={() => setIsScanning(false)} style={{ width: '100%', padding: '16px', background: 'transparent', border: 'none', color: 'white', fontWeight: '600', cursor: 'pointer', marginTop: '8px' }}>
                            Cancel Scan
                        </button>
                    </div>
                )}

                {/* State 3: Loading Data */}
                {scannedHash && isPending && !showReportForm && (
                    <div className={styles.messageZone}>
                        <span className="material-symbols-outlined" style={{ fontSize: '48px', color: 'var(--primary)', animation: 'spin 2s linear infinite' }}>hourglass_empty</span>
                        <p style={{ marginTop: '16px', fontWeight: '600', color: 'var(--primary)' }}>Querying National Ledger...</p>
                    </div>
                )}

                {/* State 4: Error / Counterfeit */}
                {scannedHash && isError && !showReportForm && (
                    <div className={styles.errorCard}>
                        <span className="material-symbols-outlined" style={{ fontSize: '48px', color: '#d62828' }}>gpp_bad</span>
                        <h2 style={{ color: '#d62828', marginTop: '12px' }}>WARNING</h2>
                        <p>UNREGISTERED / POTENTIAL COUNTERFEIT</p>
                        <p style={{ fontSize: '12px', color: '#666', marginTop: '8px', marginBottom: '24px' }}>
                            This QR code does not exist on the national registry. Do not consume this product.
                        </p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <button className={styles.btnPrimary} style={{ background: '#d62828' }} onClick={() => { setReportForm({...reportForm, issueType: 'COUNTERFEIT'}); setShowReportForm(true); }}>
                                <span className="material-symbols-outlined">report</span> Report to Authorities
                            </button>
                            <button className={styles.btnSecondary} onClick={resetScanner}>Scan Another Product</button>
                        </div>
                    </div>
                )}

                {/* State 5: Success / Provenance View */}
                {scanResult && !isError && !showReportForm && (
                    <div className={styles.resultCard}>

                        {scanResult.message && scanResult.message.includes('Warning') && (
                            <div style={{ background: '#fff3cd', color: '#856404', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px', fontWeight: '600' }}>
                                {scanResult.message}
                            </div>
                        )}

                        <div className={`${styles.statusBanner} ${getStatusStyle(scanResult.verificationStatus)}`}>
                            <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>
                                {scanResult.verificationStatus === 'AUTHENTIC' ? 'verified_user' : 'warning'}
                            </span>
                            {scanResult.verificationStatus}
                        </div>

                        <h3 style={{ textAlign: 'center', fontSize: '20px', color: 'var(--primary)', marginBottom: '24px' }}>
                            {scanResult.productDetails.genericName || "Unknown Product"}
                        </h3>

                        <div className={styles.detailRow}>
                            <span className={styles.detailLabel}>Batch No.</span>
                            <span className={styles.detailValue}>{scanResult.productDetails.batchNumber || "N/A"}</span>
                        </div>

                        {/* NEW: Serial Number Row */}
                        <div className={styles.detailRow}>
                            <span className={styles.detailLabel}>Serial No.</span>
                            <span className={styles.detailValue}>{scanResult.productDetails.serialNumber || "N/A"}</span>
                        </div>

                        <div className={styles.detailRow}>
                            <span className={styles.detailLabel}>Manufacturer</span>
                            <span className={styles.detailValue}>{scanResult.productDetails.manufacturer || "N/A"}</span>
                        </div>
                        <div className={styles.detailRow}>
                            <span className={styles.detailLabel}>Expiry Date</span>
                            <span className={styles.detailValue}>{scanResult.productDetails.expiryDate || "N/A"}</span>
                        </div>
                        {scanResult.productDetails.currentStatus && (
                            <div className={styles.detailRow}>
                                <span className={styles.detailLabel}>Current System Status</span>
                                <span className={styles.detailValue}>{scanResult.productDetails.currentStatus}</span>
                            </div>
                        )}

                        {/* Provenance Timeline Section (Updated to reverse and prioritize Human-Readable Names) */}
                        <div className={styles.timelineSection}>
                            <h4 className={styles.timelineTitle}>Chain of Custody</h4>

                            {scanResult.provenanceTimeline && scanResult.provenanceTimeline.length > 0 ? (
                                [...scanResult.provenanceTimeline].reverse().map((event, index) => {
                                    const rawTime = event.eventTimestamp || event.timestamp;
                                    const date = rawTime ? new Date(rawTime).toLocaleString('en-GB', {
                                        day: 'numeric', month: 'short', year: 'numeric',
                                        hour: '2-digit', minute: '2-digit'
                                    }) : 'Unknown Time';

                                    // Look for 'Name' fields first. If null, fallback to the raw ID/hash.
                                    const fromName = event.fromParticipantName || event.fromParticipant || 'System';
                                    const toName = event.toParticipantName || event.toParticipant || 'Unknown';

                                    return (
                                        <div className={styles.timelineItem} key={index}>
                                            <div className={styles.timelineConnector}></div>
                                            <div className={styles.timelineDot}></div>
                                            <div className={styles.timelineContent}>
                                                <span className={styles.eventTime}>{date}</span>
                                                <h4 className={styles.eventType}>{(event.eventType || 'UNKNOWN').replace('_', ' ')}</h4>

                                                {event.eventType === 'MANUFACTURED' ? (
                                                    <p className={styles.participants}>Origin: {fromName === 'System' ? toName : fromName}</p>
                                                ) : event.eventType === 'TRANSFER' ? (
                                                    <p className={styles.participants}>
                                                        From: {fromName} <br/>
                                                        To: <strong>{toName}</strong>
                                                    </p>
                                                ) : (
                                                    <p className={styles.participants}>Location: {toName}</p>
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

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '32px' }}>
                            <button className={styles.btnPrimary} onClick={resetScanner}>
                                <span className="material-symbols-outlined">qr_code_scanner</span>
                                Scan Another Package
                            </button>
                            <button className={styles.btnSecondary} style={{ color: '#d62828', border: '1px solid #d62828', background: 'transparent' }} onClick={() => setShowReportForm(true)}>
                                <span className="material-symbols-outlined">flag</span>
                                Report Issue (Side Effects / Damage)
                            </button>
                            <Link to="/" className={styles.btnSecondary} style={{ textDecoration: 'none', border: 'none' }}>
                                Exit to Home
                            </Link>
                        </div>
                    </div>
                )}
            </div>
        );
    }