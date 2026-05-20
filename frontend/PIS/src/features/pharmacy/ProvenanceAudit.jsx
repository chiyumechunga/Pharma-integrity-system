import React, { useState, useEffect, startTransition } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { apiClient } from '../../services/apiClient';
import styles from './PharmacyDashboard.module.css';

export default function ProvenanceAudit() {
    const { qrHash: routeHash } = useParams();
    const navigate = useNavigate();

    // Initialize state with URL param if available, otherwise null to trigger scanner
    const [scannedHash, setScannedHash] = useState(routeHash || null);

    const { data: history, isLoading, error } = useQuery({
        queryKey: ['provenance', scannedHash],
        queryFn: async () => {
            const response = await apiClient.get(`/provenance/${scannedHash}`);
            let responseData = response.data?.data || response.data;

            if (!Array.isArray(responseData)) {
                if (responseData && typeof responseData === 'object') {
                    responseData = [responseData];
                } else {
                    return [];
                }
            }

            // Parse Firefly stringified payloads if necessary
            if (responseData.length > 0 && typeof responseData[0] === 'string') {
                responseData = responseData.map(item => JSON.parse(item));
            }

            return responseData;
        },
        enabled: !!scannedHash // Only run the query if we have a hash
    });

    // Handle Scanner Initialization
    useEffect(() => {
        let scanner;
        if (!scannedHash) {
            scanner = new Html5QrcodeScanner("audit-reader", {
                fps: 10,
                qrbox: { width: 250, height: 250 }
            }, false);

            scanner.render(
                (decodedText) => {
                    try {
                        scanner.pause();
                    } catch (err) {
                        console.log("Static image detected, skipping pause.");
                    }

                    scanner.clear().then(() => {

                        // THE FIX: Wrap the suspending state update in startTransition
                        startTransition(() => {
                            setScannedHash(decodedText);
                        });

                    }).catch(console.error);
                },
                () => {} // Ignore background read errors
            );
        }

        return () => {
            if (scanner) {
                scanner.clear().catch(() => {});
            }
        };
    }, [scannedHash]);

    const handleReset = () => {
        setScannedHash(null);
        if (routeHash) {
            // Clean up the URL if we started with a direct link
            navigate('/audit', { replace: true });
        }
    };

    return (
        <div className={styles.dashboardWrapper}>
            <main className={styles.mainContent} style={{ maxWidth: '900px' }}>
                <header className={styles.header}>
                    <div>
                        {/* Dynamic Back Button */}
                        <button
                            onClick={() => navigate(-1)}
                            style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '8px',
                                background: 'none',
                                border: 'none',
                                color: 'var(--on-surface-variant)',
                                cursor: 'pointer',
                                fontWeight: '600',
                                fontFamily: 'var(--font-ui)',
                                padding: 0,
                                marginBottom: '16px',
                                transition: 'color 0.2s'
                            }}
                            onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary)'}
                            onMouseLeave={(e) => e.currentTarget.style.color = 'var(--on-surface-variant)'}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>arrow_back</span>
                            Back
                        </button>
                        <h1 className={styles.title}>Provenance Audit</h1>
                        <p className={styles.subtitle}>Immutable Chain of Custody History</p>
                    </div>
                </header>

                <div className={styles.scannerZone}>
                    {/* STATE 1: Waiting for Scan */}
                    {!scannedHash ? (
                        <div style={{ textAlign: 'center', padding: '24px 0' }}>
                            <h2 style={{ marginBottom: '16px', color: '#333' }}>Scan Item to View Provenance</h2>
                            <p style={{ color: '#666', marginBottom: '24px' }}>Position the QR code within the frame to pull records from the distributed ledger.</p>
                            <div className={styles.scannerWrapper} style={{ maxWidth: '400px', margin: '0 auto', overflow: 'hidden', borderRadius: '12px' }}>
                                <div id="audit-reader"></div>
                            </div>
                        </div>
                    ) : (
                        /* STATE 2: Displaying Audit Data */
                        <>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '32px', paddingBottom: '24px', borderBottom: '1px solid #eaeaea' }}>
                                <div style={{ flex: 1, marginRight: '16px' }}>
                                    <span style={{ fontSize: '12px', fontWeight: 'bold', color: '#666', textTransform: 'uppercase', letterSpacing: '1px' }}>Cryptographic Hash Identifier</span>
                                    <code style={{ display: 'block', wordBreak: 'break-all', background: '#f8f9fa', padding: '12px', borderRadius: '8px', marginTop: '8px', color: 'var(--primary)', border: '1px solid #eaeaea' }}>
                                        {scannedHash}
                                    </code>
                                </div>
                                <button
                                    onClick={handleReset}
                                    style={{
                                        background: 'var(--primary-container)', color: 'white', border: 'none',
                                        padding: '10px 20px', borderRadius: '8px', cursor: 'pointer',
                                        fontWeight: '600', display: 'flex', alignItems: 'center', gap: '8px',
                                        flexShrink: 0
                                    }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>qr_code_scanner</span>
                                    Scan Another
                                </button>
                            </div>

                            {isLoading && (
                                <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '32px', animation: 'spin 2s linear infinite' }}>sync</span>
                                    <p>Querying immutable records from the distributed ledger...</p>
                                </div>
                            )}

                            {error && (
                                <div className={styles.alertBanner} style={{ backgroundColor: '#fce8e8', color: '#c5221f' }}>
                                    <span className="material-symbols-outlined">error</span>
                                    Failed to retrieve blockchain provenance data.
                                </div>
                            )}

                            <div style={{ marginLeft: '16px' }}>
                                {history?.map((event, index) => {
                                    const eventType = event.event_type || event.eventType || event.docType || 'EVENT_LOGGED';
                                    const fromName = event.from_participant_name || event.fromParticipantId || event.manufacturerId || 'System';
                                    const toName = event.to_participant_name || event.toParticipantId || event.currentOwnerId || 'Unknown Destination';
                                    const txId = event.blockchain_tx || event.txId || event.blockchainTxId || 'Pending_Tx';

                                    let eventDate = 'Unknown Date';
                                    if (event.event_timestamp || event.createdAt) {
                                        eventDate = new Date(event.event_timestamp || event.createdAt).toLocaleString();
                                    } else if (event.timestampNanos) {
                                        eventDate = new Date(parseInt(event.timestampNanos) / 1000000).toLocaleString();
                                    }

                                    return (
                                        <div key={index} style={{
                                            borderLeft: '2px solid var(--primary-container)',
                                            paddingLeft: '24px',
                                            paddingBottom: '32px',
                                            position: 'relative'
                                        }}>
                                            <div style={{
                                                width: '16px', height: '16px', backgroundColor: 'white',
                                                border: '4px solid var(--primary-container)', borderRadius: '50%',
                                                position: 'absolute', left: '-9px', top: '0'
                                            }}></div>

                                            <div style={{ background: '#f8f9fa', padding: '16px', borderRadius: '8px', border: '1px solid #eaeaea', marginTop: '-4px' }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                                                    <strong style={{ color: 'var(--primary)', fontSize: '16px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                                        {eventType === 'DISPENSED' ? <span className="material-symbols-outlined" style={{color: '#1e8e3e'}}>prescriptions</span> : <span className="material-symbols-outlined">local_shipping</span>}
                                                        {eventType}
                                                    </strong>
                                                    <span style={{ fontSize: '13px', color: '#666', background: '#fff', padding: '4px 8px', borderRadius: '4px', border: '1px solid #ddd' }}>
                                                        {eventDate}
                                                    </span>
                                                </div>

                                                <p style={{ margin: '0 0 12px 0', fontSize: '14px', color: '#333' }}>
                                                    <strong>From:</strong> {fromName} <br/>
                                                    <strong>To:</strong> {toName}
                                                </p>

                                                <div style={{ background: '#fff', padding: '8px', borderRadius: '4px', border: '1px solid #eee', display: 'inline-block' }}>
                                                    <small style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: '#666' }}>
                                                        <span className="material-symbols-outlined" style={{ fontSize: '14px' }}>link</span>
                                                        TX:
                                                        <a href={`https://explorer.firefly.local/transactions/${txId}`} target="_blank" rel="noreferrer" style={{ color: 'var(--primary-container)', textDecoration: 'none', fontFamily: 'monospace' }}>
                                                            {txId}
                                                        </a>
                                                    </small>
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>

                            {!isLoading && (!history || history.length === 0) && (
                                <div style={{ padding: '32px', textAlign: 'center', background: '#f8f9fa', borderRadius: '12px', color: '#666', border: '1px dashed #ccc' }}>
                                    <span className="material-symbols-outlined" style={{ fontSize: '48px', marginBottom: '16px', color: '#ccc' }}>history_toggle_off</span>
                                    <h3 style={{ margin: '0 0 8px 0' }}>No Audit Trail Found</h3>
                                    <p style={{ margin: 0, fontSize: '14px' }}>This QR hash has no recorded history on the distributed ledger.</p>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </main>
        </div>
    );
}