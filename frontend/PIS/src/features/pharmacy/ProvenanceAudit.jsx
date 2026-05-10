import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import styles from './PharmacyDashboard.module.css';

export default function ProvenanceAudit() {
    const { qrHash } = useParams(); // Gets the hash from the URL /provenance/:qrHash
    const navigate = useNavigate();

    // 1. Fetch the full history of this specific batch
    const { data: history, isLoading, error } = useQuery({
        queryKey: ['provenance', qrHash],
        queryFn: async () => {
            // This endpoint should call the SQL function fn_get_product_provenance
            const response = await apiClient.get(`/provenance/${qrHash}`);
            return response.data;
        }
    });

    return (
        <div className={styles.dashboardWrapper}>
            <main className={styles.mainContent}>
                <header className={styles.header}>
                    <div>
                        <h1 className={styles.title}>Provenance Audit</h1>
                        <p className={styles.subtitle}>Immutable Chain of Custody History</p>
                    </div>
                    <button className={styles.btnPrimary} onClick={() => navigate(-1)}>
                        Back
                    </button>
                </header>

                <div className={styles.scannerZone}>
                    <div style={{ marginBottom: '24px' }}>
                        <small style={{ color: 'var(--on-surface-variant)' }}>QR HASH ID:</small>
                        <code style={{ display: 'block', wordBreak: 'break-all', background: '#f5f5f5', padding: '8px', borderRadius: '4px' }}>
                            {qrHash}
                        </code>
                    </div>

                    {isLoading && <p>Loading immutable records...</p>}
                    {error && <p style={{ color: 'red' }}>Failed to retrieve blockchain provenance.</p>}

                    <div className="timeline">
                        {history?.map((event, index) => (
                            <div key={index} style={{
                                borderLeft: '3px solid var(--primary-container)',
                                paddingLeft: '20px',
                                marginBottom: '30px',
                                position: 'relative'
                            }}>
                                {/* Timeline Dot */}
                                <div style={{
                                    width: '12px',
                                    height: '12px',
                                    backgroundColor: 'var(--primary-container)',
                                    borderRadius: '50%',
                                    position: 'absolute',
                                    left: '-7.5px',
                                    top: '0'
                                }}></div>

                                <strong style={{ color: 'var(--primary)', fontSize: '16px' }}>
                                    {event.event_type}
                                </strong>
                                <p style={{ margin: '4px 0', fontSize: '14px' }}>
                                    {event.from_participant_name} → {event.to_participant_name}
                                </p>
                                <small style={{ color: 'var(--on-surface-variant)' }}>
                                    {new Date(event.event_timestamp).toLocaleString()}
                                </small>

                                <div style={{ marginTop: '8px' }}>
                                    <small style={{ display: 'block', fontSize: '11px', color: '#666' }}>
                                        Blockchain TX:
                                        <a href={`https://explorer.firefly.local/transactions/${event.blockchain_tx}`} target="_blank" rel="noreferrer" style={{ marginLeft: '5px', color: 'var(--primary-container)' }}>
                                            {event.blockchain_tx?.substring(0, 16)}...
                                        </a>
                                    </small>
                                </div>
                            </div>
                        ))}
                    </div>

                    {!isLoading && history?.length === 0 && (
                        <p>No custody events found for this hash.</p>
                    )}
                </div>
            </main>
        </div>
    );
}