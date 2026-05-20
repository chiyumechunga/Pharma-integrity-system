// src/features/pharmacy/DispenseItem.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import styles from './PharmacyDashboard.module.css';

export default function DispenseItem() {
    const { qrHash: routeHash } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { user } = useAuth();
    const queryClient = useQueryClient();

    // Prioritize the hash from the scanner, fallback to URL parameter
    const [scannedHash, setScannedHash] = useState(routeHash || null);
    const [isSuccess, setIsSuccess] = useState(false);

    // Fallback product info if passed from another screen
    const initialProductInfo = location.state?.product || {};

    // 1. Verify the drug authenticity & status before dispensing
    const verifyMutation = useMutation({
        mutationFn: async (hash) => {
            const response = await apiClient.post('/verifications', {
                qrHash: hash,
                deviceFingerprint: navigator.userAgent,
                geoLocation: 'Pharmacy POS'
            });
            return response.data;
        }
    });

    // 2. Execute the final Dispense
    const dispenseMutation = useMutation({
        mutationFn: async () => {
            const response = await apiClient.post('/units/dispense', {
                qrHash: scannedHash,
                // Use verified data if available, otherwise fallback to route state
                batchNumber: verifyMutation.data?.batchNumber || initialProductInfo.batchNumber,
                pharmacyId: user?.participantId
            });
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries(['pharmacyBatches']);
            queryClient.invalidateQueries(['pharmacyHistory']);
            setIsSuccess(true);
        }
    });

    // 3. Scanner Engine (Runs only if no hash is acquired yet)
    useEffect(() => {
        let scanner;
        if (!scannedHash) {
            scanner = new Html5QrcodeScanner("dispense-reader", {
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
                        setScannedHash(decodedText);
                        verifyMutation.mutate(decodedText);
                    }).catch(console.error);
                },
                () => {}
            );
        } else if (!verifyMutation.data && !verifyMutation.isPending && !verifyMutation.isError) {
            // If we navigated here WITH a hash but haven't verified it yet
            verifyMutation.mutate(scannedHash);
        }

        return () => {
            if (scanner) {
                scanner.clear().catch(() => {});
            }
        };
    }, [scannedHash, verifyMutation]);

    const handleCancel = () => {
        setScannedHash(null);
        verifyMutation.reset();
        dispenseMutation.reset();
        navigate('/pharmacy');
    };

    const productName = verifyMutation.data?.productName || initialProductInfo.productName || 'Unknown Medication';
    const batchNumber = verifyMutation.data?.batchNumber || initialProductInfo.batchNumber || 'Unknown';
    const isDispensed = verifyMutation.data?.status === 'DISPENSED';

    return (
        <div className={styles.dashboardWrapper}>
            <main className={styles.mainContent} style={{ maxWidth: '800px' }}>
                <header className={styles.header}>
                    <div>
                        <h1 className={styles.title}>Dispense Medication</h1>
                        <p className={styles.subtitle}>Patient Handover Authorization</p>
                    </div>
                    <button className={styles.btnSecondary} onClick={handleCancel} style={{ padding: '8px 16px', borderRadius: '8px', cursor: 'pointer' }}>
                        Cancel & Return
                    </button>
                </header>

                <div className={styles.scannerZone} style={{ textAlign: 'center', padding: '48px 24px', background: 'white', borderRadius: '16px', border: '1px solid #eaeaea' }}>

                    {/* STATE 1: Waiting for Scan */}
                    {!scannedHash && (
                        <>
                            <h2 style={{ marginBottom: '16px', color: '#333' }}>Scan Medication to Dispense</h2>
                            <p style={{ color: '#666', marginBottom: '24px' }}>Position the unit QR code within the camera frame.</p>
                            <div className={styles.scannerWrapper} style={{ maxWidth: '400px', margin: '0 auto', overflow: 'hidden', borderRadius: '12px' }}>
                                <div id="dispense-reader"></div>
                            </div>
                        </>
                    )}

                    {/* STATE 2: Verifying Hash */}
                    {verifyMutation.isPending && (
                        <div style={{ padding: '40px 0' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '48px', color: '#1976d2', animation: 'spin 2s linear infinite' }}>sync</span>
                            <p style={{ marginTop: '16px', color: '#666', fontWeight: '500' }}>Verifying cryptogram on the distributed ledger...</p>
                        </div>
                    )}

                    {/* STATE 3: Verified & Ready to Dispense */}
                    {verifyMutation.data && !isSuccess && (
                        <>
                            <div style={{ backgroundColor: '#e3f2fd', display: 'inline-block', padding: '16px', borderRadius: '50%', color: '#1976d2', marginBottom: '24px' }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '48px' }}>prescriptions</span>
                            </div>

                            <h2 style={{ marginBottom: '8px', color: '#333' }}>{productName}</h2>
                            <p style={{ color: '#666', marginBottom: '8px' }}>Batch: <strong style={{ fontFamily: 'monospace' }}>{batchNumber}</strong></p>
                            <p style={{ color: '#aaa', fontSize: '12px', wordBreak: 'break-all', padding: '0 24px', marginBottom: '32px', fontFamily: 'monospace' }}>
                                Hash: {scannedHash}
                            </p>

                            {isDispensed && (
                                <div style={{ backgroundColor: '#fff8e1', color: '#b06000', padding: '16px', borderRadius: '8px', marginBottom: '24px', textAlign: 'left', borderLeft: '4px solid #ffb300' }}>
                                    <span className="material-symbols-outlined" style={{ verticalAlign: 'middle', marginRight: '8px' }}>warning</span>
                                    <strong>Warning:</strong> This item is already marked as dispensed on the ledger. Do not hand out.
                                </div>
                            )}

                            {dispenseMutation.isError && (
                                <div style={{ backgroundColor: '#fce8e8', color: '#c5221f', padding: '16px', borderRadius: '8px', marginBottom: '24px', textAlign: 'left', borderLeft: '4px solid #d32f2f' }}>
                                    <span className="material-symbols-outlined" style={{ verticalAlign: 'middle', marginRight: '8px' }}>error</span>
                                    Transaction Failed: {dispenseMutation.error?.response?.data || dispenseMutation.error?.message || 'Unable to communicate with ledger.'}
                                </div>
                            )}

                            <button
                                className={styles.btnPrimary}
                                onClick={() => dispenseMutation.mutate()}
                                disabled={dispenseMutation.isPending || isDispensed}
                                style={{ width: '100%', maxWidth: '300px', justifyContent: 'center', padding: '16px', fontSize: '16px', opacity: isDispensed ? 0.5 : 1 }}
                            >
                                {dispenseMutation.isPending ? 'Writing to Ledger...' : 'Confirm Patient Dispense'}
                            </button>
                        </>
                    )}

                    {/* STATE 4: Success */}
                    {isSuccess && (
                        <div style={{ padding: '24px 0' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '80px', color: '#1e8e3e', marginBottom: '16px' }}>check_circle</span>
                            <h2 style={{ color: '#1e8e3e', marginBottom: '8px' }}>Dispense Successful</h2>
                            <p style={{ color: '#666', marginBottom: '32px' }}>
                                <strong>{productName}</strong> has been securely logged to the patient ledger.
                            </p>
                            <button className={styles.btnPrimary} onClick={handleCancel} style={{ padding: '12px 32px' }}>
                                Return to Dashboard
                            </button>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}