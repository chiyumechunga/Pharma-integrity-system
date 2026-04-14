import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import styles from './RegulatoryDashboard.module.css';

export default function RegulatoryDashboard() {
    const { user, logout } = useAuth();

    // States
    const [recallBatchId, setRecallBatchId] = useState('');
    const [labForm, setLabForm] = useState({
        batchNumber: '',
        status: 'PENDING',
        notes: ''
    });

    // 1. DASHBOARD STATS
    const { data: stats } = useQuery({
        queryKey: ['dashboardStats'],
        queryFn: async () => {
            try {
                const res = await apiClient.get('/analytics/dashboard');
                return res.data;
            } catch (err) {
                return { totalScans24h: 0, suspiciousActivityCount: 0, batchesNearExpiry: 0 };
            }
        },
        refetchInterval: 30000
    });

    // 2. NETWORK ALERTS
    const { data: alerts } = useQuery({
        queryKey: ['networkAlerts'],
        queryFn: async () => {
            try {
                const res = await apiClient.get('/analytics/alerts');
                return res.data;
            } catch (err) {
                return [];
            }
        },
        refetchInterval: 15000
    });

    // 3. EMERGENCY RECALL MUTATION
    const recallMutation = useMutation({
        mutationFn: async (batchNumber) => {
            const res = await apiClient.post(`/batches/${batchNumber}/recall`);
            return res.data;
        },
        onSuccess: () => {
            alert(`Recall successfully initiated for ${recallBatchId}`);
            setRecallBatchId('');
        },
        onError: (err) => {
            alert(`Failed to recall: ${err.response?.data?.message || err.message}`);
        }
    });

    // 4. QUALITY INSPECTION (LAB TEST) MUTATION
    const labMutation = useMutation({
        mutationFn: async (data) => {
            // Hits RegulatoryController for Lab Inspections
            const res = await apiClient.post('/regulatory/scrutiny', data);
            return res.data;
        },
        onSuccess: () => {
            alert('Lab inspection results securely recorded on the ledger!');
            setLabForm({ batchNumber: '', status: 'PENDING', notes: '' });
        },
        onError: (err) => {
            alert(`Failed to record lab result: ${err.response?.data?.message || err.message}`);
        }
    });

    const handleRecallSubmit = (e) => {
        e.preventDefault();
        if (!recallBatchId) return;
        const confirmed = window.confirm(`WARNING: You are about to initiate a network-wide recall for Batch ${recallBatchId}. This will immediately lock the batch. Proceed?`);
        if (confirmed) {
            recallMutation.mutate(recallBatchId);
        }
    };

    const handleLabSubmit = (e) => {
        e.preventDefault();
        if (!labForm.batchNumber) return;
        labMutation.mutate({
            batchNumber: labForm.batchNumber,
            status: labForm.status,
            notes: labForm.notes,
            inspectorId: user?.participantId || 'ZAMRA-INSPECTOR-1'
        });
    };

    return (
        <div className={styles.container}>

            <header className={styles.header}>
                <div>
                    <h1 className={styles.title}>Regulatory Oversight</h1>
                    <p className={styles.subtitle}>National Pharmaceutical Ledger • Live Status</p>
                </div>
                <div className={styles.userProfile}>
                    <span className="material-symbols-outlined">shield_person</span>
                    <span>{user?.username || 'ZAMRA Official'}</span>
                    <button onClick={logout} className={styles.logoutBtn} title="Secure Logout" style={{ marginLeft: '12px' }}>
                        <span className="material-symbols-outlined">logout</span>
                    </button>
                </div>
            </header>

            <main>
                <div className={styles.metricsGrid}>
                    <div className={styles.metricCard}>
                        <div className={styles.metricLabel}>Total Scans (24h)</div>
                        <div className={styles.metricValue}>{stats?.totalScans24h?.toLocaleString() || '0'}</div>
                    </div>
                    <div className={styles.metricCard}>
                        <div className={styles.metricLabel}>Suspicious Activity</div>
                        <div className={`${styles.metricValue} ${styles.warningText}`}>{stats?.suspiciousActivityCount || '0'}</div>
                    </div>
                    <div className={styles.metricCard}>
                        <div className={styles.metricLabel}>Batches Near Expiry</div>
                        <div className={styles.metricValue}>{stats?.batchesNearExpiry || '0'}</div>
                    </div>
                </div>

                {/* NEW: Lab Quality Inspection Zone */}
                <div className={styles.labZone}>
                    <div className={styles.labHeader}>
                        <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>
                            biotech
                        </span>
                        <h2>Quality Assurance & Lab Scrutiny</h2>
                    </div>
                    <p className={styles.labDescription}>
                        Record official laboratory test results to update a batch's compliance status. Batches marked as "FAILED" will be automatically restricted from being dispensed.
                    </p>

                    <form onSubmit={handleLabSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                        <div className={styles.formRow}>
                            <div className={styles.inputWrapper}>
                                <input
                                    type="text"
                                    className={styles.input}
                                    placeholder="Batch Number (e.g., BAT-1024-ZM)"
                                    value={labForm.batchNumber}
                                    onChange={(e) => setLabForm({ ...labForm, batchNumber: e.target.value })}
                                    required
                                    disabled={labMutation.isPending}
                                />
                            </div>
                            <div className={styles.inputWrapper}>
                                <select
                                    className={styles.input}
                                    value={labForm.status}
                                    onChange={(e) => setLabForm({ ...labForm, status: e.target.value })}
                                    disabled={labMutation.isPending}
                                >
                                    <option value="PENDING">PENDING (Quarantine/Testing)</option>
                                    <option value="PASSED">PASSED (Clear for Distribution)</option>
                                    <option value="FAILED">FAILED (Reject & Destroy)</option>
                                </select>
                            </div>
                        </div>
                        <div className={styles.inputWrapper} style={{ maxWidth: '100%' }}>
                            <textarea
                                className={styles.input}
                                placeholder="Inspection notes, lab findings, or test IDs..."
                                value={labForm.notes}
                                onChange={(e) => setLabForm({ ...labForm, notes: e.target.value })}
                                rows="2"
                                style={{ resize: 'vertical' }}
                                disabled={labMutation.isPending}
                            ></textarea>
                        </div>
                        <button type="submit" className={styles.labBtn} disabled={labMutation.isPending}>
                            {labMutation.isPending ? 'Recording on Ledger...' : 'Submit Lab Result'}
                        </button>
                    </form>
                </div>

                {/* The Danger Zone */}
                <div className={styles.dangerZone}>
                    <div className={styles.dangerHeader}>
                        <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>
                          warning
                        </span>
                        <h2>Emergency Recall Initialization</h2>
                    </div>
                    <p className={styles.dangerDescription}>
                        Input a Batch Number below to immediately trigger a network-wide override.
                        This writes a terminal "RECALLED" state to the Fabric ledger, permanently blocking any pharmacy from dispensing this medication.
                    </p>
                    <form className={styles.recallForm} onSubmit={handleRecallSubmit}>
                        <div className={styles.inputWrapper}>
                            <input
                                type="text"
                                className={styles.input}
                                placeholder="Enter Batch Number (e.g., BAT-9942-XY)"
                                value={recallBatchId}
                                onChange={(e) => setRecallBatchId(e.target.value)}
                                disabled={recallMutation.isPending}
                            />
                        </div>
                        <button type="submit" className={styles.recallBtn} disabled={recallMutation.isPending}>
                            {recallMutation.isPending ? 'Processing...' : 'Initiate Recall'}
                        </button>
                    </form>
                </div>
            </main>

            <aside className={styles.alertsPanel}>
                <div className={styles.alertsHeader}>
                    <h3>Live Network Alerts</h3>
                    <div className={styles.pulseDot}></div>
                </div>

                <div>
                    {alerts && alerts.length > 0 ? (
                        alerts.map((alert, index) => (
                            <div className={styles.timelineItem} key={index}>
                                <div className={styles.timelineConnector}></div>
                                <div className={`${styles.timelineDot} ${alert.severity === 'CRITICAL' ? styles.timelineDotAlert : ''}`}></div>
                                <div className={styles.timelineContent}>
                                    <h4>{alert.title || 'System Alert'}</h4>
                                    <p>{new Date(alert.timestamp).toLocaleTimeString()} • {alert.description || 'Action recorded on ledger.'}</p>
                                </div>
                            </div>
                        ))
                    ) : (
                        <p style={{ fontFamily: 'var(--font-ui)', fontSize: '13px', color: 'var(--on-surface-variant)' }}>
                            No recent alerts. The network is secure.
                        </p>
                    )}
                </div>
            </aside>

        </div>
    );
}