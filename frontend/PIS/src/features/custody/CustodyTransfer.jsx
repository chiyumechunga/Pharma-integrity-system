import React, { useState, useEffect } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { useNavigate } from 'react-router-dom';
import styles from './CustodyTransfer.module.css';
import { apiClient } from "../../services/apiClient.js";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useAuth } from '../auth/AuthContext';

export default function CustodyTransfer() {
    const { user, logout } = useAuth();
    const navigate = useNavigate(); // Initialize navigation stack
    const [isScanning, setIsScanning] = useState(false);
    const [scannedBatch, setScannedBatch] = useState('');
    const [selectedRecipient, setSelectedRecipient] = useState('');

    const { data: participants } = useQuery({
        queryKey: ['participants'],
        queryFn: async () => {
            const response = await apiClient.get('/participants');
            return response.data.filter(p => p.participantId !== user?.participantId);
        }
    });

    const transferMutation = useMutation({
        mutationFn: async (transferData) => {
            const response = await apiClient.post('/custody', transferData);
            return response.data;
        },
        onSuccess: () => {
            alert(`Custody of ${scannedBatch} successfully transferred to recipient.`);
            setScannedBatch('');
            setSelectedRecipient('');
        },
        onError: (error) => {
            alert("Transfer failed: " + (error.response?.data?.message || error.message));
        }
    });

    useEffect(() => {
        let scanner;
        if (isScanning) {
            scanner = new Html5QrcodeScanner("handover-reader", {
                fps: 10,
                qrbox: { width: 250, height: 250 }
            }, false);

            scanner.render((decodedText) => {
                setScannedBatch(decodedText);
                setIsScanning(false);
                scanner.clear();
            }, () => { /* ignore background errors */ });
        }
        return () => { if (scanner) scanner.clear().catch(console.error); };
    }, [isScanning]);

    const handleTransferSubmit = (e) => {
        e.preventDefault();
        if (!scannedBatch || !selectedRecipient) return alert("Please scan a batch and select a recipient.");

        transferMutation.mutate({
            batchNumber: scannedBatch,
            fromParticipantId: user?.participantId,
            toParticipantId: selectedRecipient,
            eventType: 'DISTRIBUTED'
        });
    };

    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <div>
                    {/* The Dynamic Back Button (History Stack Pop) */}
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
                        Back to Dashboard
                    </button>

                    <h1 className={styles.title}>Handover</h1>
                    <p className={styles.subtitle}>{user?.username} • {user?.role}</p>
                </div>

                <button onClick={logout} className={styles.logoutBtn} title="Secure Logout">
                    <span className="material-symbols-outlined">logout</span>
                </button>
            </header>

            <section className={styles.actionSection}>
                <div className={styles.formCard}>
                    <h2 style={{ marginBottom: '16px', fontSize: '18px', color: 'var(--primary)' }}>Logistics Transfer</h2>

                    {isScanning ? (
                        <div id="handover-reader" style={{ width: '100%', marginBottom: '16px' }}></div>
                    ) : (
                        <div className={styles.inputGroup}>
                            <label className={styles.label}>Scanned Batch Number</label>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                <input className={styles.input} value={scannedBatch} readOnly placeholder="Click icon to scan..." />
                                <button className={styles.btnScan} onClick={() => setIsScanning(true)}>
                                    <span className="material-symbols-outlined">qr_code_scanner</span>
                                </button>
                            </div>
                        </div>
                    )}

                    <div className={styles.inputGroup} style={{ marginTop: '16px' }}>
                        <label className={styles.label}>Recipient Participant</label>
                        <select className={styles.select} value={selectedRecipient} onChange={(e) => setSelectedRecipient(e.target.value)}>
                            <option value="">Select recipient...</option>
                            {participants?.map(p => (
                                <option key={p.participantId} value={p.participantId}>
                                    {p.participantName} ({p.participantType})
                                </option>
                            ))}
                        </select>
                    </div>

                    <button
                        className={styles.btnPrimary}
                        onClick={handleTransferSubmit}
                        disabled={transferMutation.isPending || !scannedBatch || !selectedRecipient}
                        style={{ marginTop: '24px', width: '100%' }}
                    >
                        {transferMutation.isPending ? 'Writing to Ledger...' : 'Confirm Handover'}
                    </button>
                </div>
            </section>
        </div>
    );
}