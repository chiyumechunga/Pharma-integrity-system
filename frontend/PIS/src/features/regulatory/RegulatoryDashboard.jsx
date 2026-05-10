import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import styles from './RegulatoryDashboard.module.css';

export default function RegulatoryDashboard() {
    const { user, logout } = useAuth();
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState('lab'); // lab, recalls, approvals, threats

    // Forms
    const [labForm, setLabForm] = useState({ batchNumber: '', status: 'PENDING', notes: '' });
    const [recallForm, setRecallForm] = useState({ batchNumber: '', severityLevel: 'CLASS_II', reason: '' });

    // ── Queries ──
    const { data: stats } = useQuery({
        queryKey: ['dashboardStats'],
        queryFn: async () => (await apiClient.get('/analytics/dashboard')).data,
        refetchInterval: 30000
    });

    const { data: alerts } = useQuery({
        queryKey: ['networkAlerts'],
        // FIXED: Point to the correct Spring Boot endpoint
        queryFn: async () => (await apiClient.get('/analytics/suspicious-patterns')).data,
        refetchInterval: 15000
    });

    // NEW: Query for unapproved drugs in product_master
    const { data: pendingDrugs, isLoading: loadingDrugs } = useQuery({
        queryKey: ['pendingDrugs'],
        queryFn: async () => (await apiClient.get('/products/pending')).data,
        enabled: activeTab === 'approvals'
    });

    // NEW: Query for vw_suspicious_patterns
    const { data: threatData, isLoading: loadingThreats } = useQuery({
        queryKey: ['threatData'],
        queryFn: async () => (await apiClient.get('/analytics/suspicious-patterns')).data,
        enabled: activeTab === 'threats'
    });

    // ── Mutations ──
    const labMutation = useMutation({
        mutationFn: async (data) => (await apiClient.post('/regulatory/scrutiny', data)).data,
        onSuccess: () => {
            alert('Lab inspection results securely recorded on the ledger!');
            setLabForm({ batchNumber: '', status: 'PENDING', notes: '' });
        }
    });

    const recallMutation = useMutation({
        mutationFn: async (data) => (await apiClient.post(`/operations/recall`, data)).data,
        onSuccess: () => {
            alert(`Recall successfully initiated for ${recallForm.batchNumber}`);
            setRecallForm({ batchNumber: '', severityLevel: 'CLASS_II', reason: '' });
        }
    });

    const approveMutation = useMutation({
        mutationFn: async (productId) => (await apiClient.post(`/products/${productId}/approve`)).data,
        onSuccess: () => {
            alert('Drug Master successfully approved.');
            queryClient.invalidateQueries(['pendingDrugs']);
        }
    });

    // ── Handlers ──
    const handleLabSubmit = (e) => {
        e.preventDefault();
        labMutation.mutate({
            batchNumber: labForm.batchNumber,
            testResult: labForm.status,
            labNotes: labForm.notes,
            inspectorId: user?.participantId
        });
    };

    const handleRecallSubmit = (e) => {
        e.preventDefault();
        const confirmed = window.confirm(`WARNING: Triggering a ${recallForm.severityLevel} recall for ${recallForm.batchNumber}. Proceed?`);
        if (confirmed) {
            recallMutation.mutate({
                batchNumber: recallForm.batchNumber,
                severityLevel: recallForm.severityLevel,
                recallReason: recallForm.reason,
                initiatedBy: user?.participantId
            });
        }
    };

    return (
        <div className={styles.dashboardWrapper}>
            {/* ── Unified Mobile-Responsive Sidebar ── */}
            <aside className={styles.sidebar}>
                <div className={styles.sidebarBrand}>
                    <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>verified</span>
                    <span>Blockchain-Based Pharmaceutical Integrity System</span>
                </div>

                <div className={`${styles.navItem} ${activeTab === 'lab' ? styles.activeNav : ''}`} onClick={() => setActiveTab('lab')}>
                    <span className="material-symbols-outlined">biotech</span> Lab Scrutiny
                </div>
                <div className={`${styles.navItem} ${activeTab === 'recalls' ? styles.activeNav : ''}`} onClick={() => setActiveTab('recalls')}>
                    <span className="material-symbols-outlined">warning</span> National Recalls
                </div>
                <div className={`${styles.navItem} ${activeTab === 'approvals' ? styles.activeNav : ''}`} onClick={() => setActiveTab('approvals')}>
                    <span className="material-symbols-outlined">fact_check</span> Drug Approvals
                </div>
                <div className={`${styles.navItem} ${activeTab === 'threats' ? styles.activeNav : ''}`} onClick={() => setActiveTab('threats')}>
                    <span className="material-symbols-outlined">policy</span> Threat Intel
                </div>

                <button onClick={logout} className={`${styles.navItem} ${styles.logoutBtn}`} style={{ marginTop: 'auto' }}>
                    <span className="material-symbols-outlined">logout</span> Sign Out
                </button>
            </aside>

            {/* ── Main Dashboard Content ── */}
            <main className={styles.mainContent}>
                <div className={styles.contentGrid}>
                    <header className={styles.header}>
                        <div>
                            <h1 className={styles.title}>
                                {activeTab === 'lab' && 'Quality Assurance Lab'}
                                {activeTab === 'recalls' && 'Emergency Recalls'}
                                {activeTab === 'approvals' && 'Master Product Approvals'}
                                {activeTab === 'threats' && 'Counterfeit Intelligence'}
                            </h1>
                            <p className={styles.subtitle}>National Pharmaceutical Ledger • Regulatory View</p>
                        </div>
                        <div className={styles.userProfile}>
                            <span className="material-symbols-outlined">shield_person</span>
                            <span>{user?.username || 'ZAMRA Official'}</span>
                        </div>
                    </header>

                    {/* Left Column Content */}
                    <div>
                        {/* Always show top metrics */}
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
                                <div className={styles.metricLabel}>Pending Approvals</div>
                                <div className={styles.metricValue}>{pendingDrugs?.length || '0'}</div>
                            </div>
                        </div>

                        {/* TAB 1: LAB SCRUTINY */}
                        {activeTab === 'lab' && (
                            <div className={styles.labZone}>
                                <div className={styles.labHeader}>
                                    <span className="material-symbols-outlined">biotech</span>
                                    <h2>Enter Lab Test Results</h2>
                                </div>
                                <p className={styles.labDescription}>Record official laboratory test results. Batches marked "FAILED" will be blocked from dispensing.</p>

                                <form onSubmit={handleLabSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                                    <div className={styles.formRow}>
                                        <div className={styles.inputWrapper}>
                                            <input type="text" className={styles.input} placeholder="Batch Number" value={labForm.batchNumber} onChange={(e) => setLabForm({ ...labForm, batchNumber: e.target.value })} required />
                                        </div>
                                        <div className={styles.inputWrapper}>
                                            <select className={styles.input} value={labForm.status} onChange={(e) => setLabForm({ ...labForm, status: e.target.value })}>
                                                <option value="PENDING">PENDING</option>
                                                <option value="PASSED">PASSED (Clear)</option>
                                                <option value="FAILED">FAILED (Reject)</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div className={styles.inputWrapper} style={{ maxWidth: '100%' }}>
                                        <textarea className={styles.input} placeholder="Inspection notes, lab findings..." value={labForm.notes} onChange={(e) => setLabForm({ ...labForm, notes: e.target.value })} rows="2" style={{ resize: 'vertical' }}></textarea>
                                    </div>
                                    <button type="submit" className={styles.labBtn} disabled={labMutation.isPending}>
                                        {labMutation.isPending ? 'Recording...' : 'Submit Lab Result'}
                                    </button>
                                </form>
                            </div>
                        )}

                        {/* TAB 2: RECALLS */}
                        {activeTab === 'recalls' && (
                            <div className={styles.dangerZone}>
                                <div className={styles.dangerHeader}>
                                    <span className="material-symbols-outlined">warning</span>
                                    <h2>Initiate Market Recall</h2>
                                </div>
                                <p className={styles.dangerDescription}>Triggers a network-wide freeze on a specific batch based on the product_recalls schema.</p>

                                <form onSubmit={handleRecallSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                                    <div className={styles.formRow}>
                                        <div className={styles.inputWrapper}>
                                            <input type="text" className={styles.input} placeholder="Batch Number" value={recallForm.batchNumber} onChange={(e) => setRecallForm({ ...recallForm, batchNumber: e.target.value })} required />
                                        </div>
                                        <div className={styles.inputWrapper}>
                                            <select className={styles.input} value={recallForm.severityLevel} onChange={(e) => setRecallForm({ ...recallForm, severityLevel: e.target.value })}>
                                                <option value="CLASS_I">CLASS I (Severe Risk)</option>
                                                <option value="CLASS_II">CLASS II (Moderate Risk)</option>
                                                <option value="CLASS_III">CLASS III (Low Risk)</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div className={styles.inputWrapper} style={{ maxWidth: '100%' }}>
                                        <textarea className={styles.input} placeholder="Detailed public reason for recall..." value={recallForm.reason} onChange={(e) => setRecallForm({ ...recallForm, reason: e.target.value })} rows="3" required></textarea>
                                    </div>
                                    <button type="submit" className={styles.recallBtn} disabled={recallMutation.isPending}>
                                        {recallMutation.isPending ? 'Processing...' : 'Execute Recall'}
                                    </button>
                                </form>
                            </div>
                        )}

                        {/* TAB 3: APPROVALS */}
                        {activeTab === 'approvals' && (
                            <div className={styles.labZone}>
                                <h2>Pending Manufacturer Registrations</h2>
                                {loadingDrugs ? <p>Loading registry...</p> : (
                                    <table className={styles.inventoryTable} style={{ width: '100%', marginTop: '16px' }}>
                                        <thead>
                                        <tr>
                                            <th style={{textAlign: 'left'}}>Code</th>
                                            <th style={{textAlign: 'left'}}>Generic Name</th>
                                            <th style={{textAlign: 'left'}}>Brand</th>
                                            <th style={{textAlign: 'right'}}>Action</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {pendingDrugs?.map((drug) => (
                                            <tr key={drug.productId}>
                                                <td>{drug.productCode}</td>
                                                <td>{drug.genericName}</td>
                                                <td>{drug.brandName}</td>
                                                <td style={{textAlign: 'right'}}>
                                                    <button className={styles.labBtn} style={{padding: '6px 12px'}} onClick={() => approveMutation.mutate(drug.productId)}>
                                                        Approve
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        {!pendingDrugs?.length && <tr><td colSpan="4">No pending drugs requiring approval.</td></tr>}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}

                        {/* TAB 4: THREATS */}
                        {activeTab === 'threats' && (
                            <div className={styles.labZone}>
                                <h2>Suspicious Scanning Patterns</h2>
                                <p>Data aggregated from <code>vw_suspicious_patterns</code> detecting potential QR cloning.</p>
                                {loadingThreats ? <p>Analyzing network...</p> : (
                                    <table className={styles.inventoryTable} style={{ width: '100%', marginTop: '16px' }}>
                                        <thead>
                                        <tr>
                                            <th style={{textAlign: 'left'}}>Device ID</th>
                                            <th style={{textAlign: 'center'}}>Unique Products</th>
                                            <th style={{textAlign: 'center'}}>Total Scans</th>
                                            <th style={{textAlign: 'right'}}>Risk Level</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {threatData?.map((threat, i) => (
                                            <tr key={i}>
                                                <td style={{fontFamily: 'monospace'}}>{threat.deviceFingerprint?.substring(0,16)}...</td>
                                                <td style={{textAlign: 'center'}}>{threat.uniqueProducts}</td>
                                                <td style={{textAlign: 'center'}}>{threat.totalScans}</td>
                                                <td style={{textAlign: 'right'}}>
                                                        <span className={`${styles.statusBadge} ${threat.riskLevel === 'HIGH_RISK' ? styles.statusDanger : ''}`}>
                                                            {threat.riskLevel}
                                                        </span>
                                                </td>
                                            </tr>
                                        ))}
                                        {!threatData?.length && <tr><td colSpan="4">No suspicious activity detected.</td></tr>}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Right Column Alerts (Kept static) */}
                    <aside className={styles.alertsPanel}>
                        <div className={styles.alertsHeader}>
                            <h3>Live Network Alerts</h3>
                            <div className={styles.pulseDot}></div>
                        </div>
                        <div>
                            {alerts?.length > 0 ? (
                                alerts.map((alert, index) => (
                                    <div className={styles.timelineItem} key={index}>
                                        <div className={styles.timelineConnector}></div>
                                        {/* You can define severity dynamically based on scanCount if needed */}
                                        <div className={`${styles.timelineDot} ${alert.scanCount > 10 ? styles.timelineDotAlert : ''}`}></div>
                                        <div className={styles.timelineContent}>
                                            {/* FIXED: Using the actual DTO fields from Spring Boot */}
                                            <h4>{alert.riskReason}</h4>
                                            <p style={{ margin: '4px 0' }}>
                                                {new Date(alert.lastScannedAt).toLocaleTimeString()} • Scanned {alert.scanCount} times
                                            </p>
                                            <small style={{ fontSize: '10px', color: '#999', wordBreak: 'break-all' }}>
                                                Hash: {alert.qrHash.substring(0, 16)}...
                                            </small>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <p style={{ fontSize: '13px', color: 'var(--on-surface-variant)' }}>No recent alerts. Network is secure.</p>
                            )}
                        </div>
                    </aside>
                </div>
            </main>
        </div>
    );
}