import React, { useState } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import styles from './PharmacyDashboard.module.css';

export default function MarkDestroyed() {
    const { qrHash } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { user } = useAuth();
    const queryClient = useQueryClient();

    const productInfo = location.state?.product || {};
    const [reason, setReason] = useState('EXPIRED');
    const [isSuccess, setIsSuccess] = useState(false);

    const destroyMutation = useMutation({
        mutationFn: async () => {
            const response = await apiClient.post('/operations/destroy', {
                qrHash: qrHash,
                batchNumber: productInfo.batchNumber,
                reason: reason,
                participantId: user?.participantId
            });
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries(['pharmacyBatches']);
            queryClient.invalidateQueries(['pharmacyHistory']);
            setIsSuccess(true);
        }
    });

    return (
        <div className={styles.dashboardWrapper}>
            <main className={styles.mainContent} style={{ maxWidth: '800px' }}>
                <header className={styles.header}>
                    <div>
                        <h1 className={styles.title} style={{ color: '#d62828' }}>Mark as Destroyed</h1>
                        <p className={styles.subtitle}>Irreversible Quarantine Action</p>
                    </div>
                    <button className={styles.btnSecondary} onClick={() => navigate('/pharmacy')}>
                        Cancel Action
                    </button>
                </header>

                <div className={styles.scannerZone} style={{ padding: '48px 24px' }}>
                    {isSuccess ? (
                        <div style={{ textAlign: 'center', color: '#d62828' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '64px', marginBottom: '16px' }}>delete_forever</span>
                            <h2>Destruction Logged</h2>
                            <p style={{ color: '#666', marginBottom: '32px' }}>
                                The unit has been permanently flagged as <strong>{reason}</strong> on the blockchain.
                            </p>
                            <button className={styles.btnPrimary} style={{ backgroundColor: '#666' }} onClick={() => navigate('/pharmacy')}>
                                Return to Dashboard
                            </button>
                        </div>
                    ) : (
                        <div style={{ maxWidth: '500px', margin: '0 auto' }}>
                            <div className={styles.alertBanner} style={{ backgroundColor: '#fce8e8', color: '#c5221f' }}>
                                <span className="material-symbols-outlined">warning</span>
                                This action permanently blocks this QR code from future use.
                            </div>

                            <div style={{ marginBottom: '24px', background: '#f8f9fa', padding: '16px', borderRadius: '8px', border: '1px solid #eaeaea' }}>
                                <h3 style={{ margin: '0 0 8px 0' }}>{productInfo.productName || 'Unknown Product'}</h3>
                                <p style={{ margin: 0, color: '#666', fontFamily: 'monospace' }}>Batch: {productInfo.batchNumber || 'N/A'}</p>
                            </div>

                            <div style={{ marginBottom: '32px' }}>
                                <label style={{ display: 'block', fontWeight: '600', marginBottom: '8px' }}>Reason for Destruction</label>
                                <select
                                    value={reason}
                                    onChange={(e) => setReason(e.target.value)}
                                    style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #ccc', fontSize: '15px' }}
                                >
                                    <option value="EXPIRED">Expired Medication</option>
                                    <option value="DAMAGED">Physical Damage / Broken Seal</option>
                                    <option value="RECALLED">Manufacturer Recall</option>
                                    <option value="COUNTERFEIT">Suspected Counterfeit</option>
                                </select>
                            </div>

                            {destroyMutation.isError && (
                                <p style={{ color: '#d62828', marginBottom: '16px', fontWeight: 'bold' }}>
                                    Error: {destroyMutation.error?.response?.data?.message || 'Ledger update failed.'}
                                </p>
                            )}

                            <button
                                className={`${styles.btnAction} ${styles.btnDanger}`}
                                onClick={() => destroyMutation.mutate()}
                                disabled={destroyMutation.isPending}
                                style={{ width: '100%', padding: '16px', fontSize: '16px', justifyContent: 'center' }}
                            >
                                <span className="material-symbols-outlined">delete</span>
                                {destroyMutation.isPending ? 'Logging to Ledger...' : 'Confirm Destruction'}
                            </button>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}