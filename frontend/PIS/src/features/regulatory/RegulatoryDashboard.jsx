import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import styles from './RegulatoryDashboard.module.css';

// Recharts imports for the clean analytics
import {
    LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
    FunnelChart, Funnel, LabelList
} from 'recharts';

export default function RegulatoryDashboard() {
    const { user, logout } = useAuth();
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState('analytics');

    // Forms
    const [labForm, setLabForm] = useState({ batchNumber: '', status: 'PENDING', notes: '' });
    const [recallForm, setRecallForm] = useState({ batchNumber: '', severityLevel: 'CLASS_II', reason: '' });

    // ── Queries (With Error Catching) ──
    const { data: stats, isLoading: loadingStats } = useQuery({
        queryKey: ['dashboardStats'],
        queryFn: async () => {
            try { return (await apiClient.get('/analytics/dashboard')).data; }
            catch { return null; }
        },
        refetchInterval: 30000
    });

    const { data: alerts } = useQuery({
        queryKey: ['networkAlerts'],
        queryFn: async () => {
            try { return (await apiClient.get('/analytics/suspicious-patterns')).data; }
            catch { return []; }
        },
        refetchInterval: 15000
    });

    const { data: pendingDrugs, isLoading: loadingDrugs } = useQuery({
        queryKey: ['pendingDrugs'],
        queryFn: async () => {
            try { return (await apiClient.get('/products/pending')).data; }
            catch { return []; }
        },
        enabled: activeTab === 'approvals'
    });

    const { data: threatData, isLoading: loadingThreats } = useQuery({
        queryKey: ['threatData'],
        queryFn: async () => {
            try { return (await apiClient.get('/analytics/suspicious-patterns')).data; }
            catch { return []; }
        },
        enabled: activeTab === 'threats'
    });

    // Old Analytics: Verification Trends
    const { data: trends, isLoading: loadingTrends } = useQuery({
        queryKey: ['verificationTrends'],
        queryFn: async () => {
            try { return (await apiClient.get('/analytics/verification-trends')).data; }
            catch { return []; }
        },
        enabled: activeTab === 'analytics'
    });

    // Active Batches Query for the Data Table
    // Fallback to advancedAnalytics if your API serves batches from there,
    // or standard /products/registry endpoint.
    const { data: batchesData, isLoading: loadingBatches } = useQuery({
        queryKey: ['activeBatches'],
        queryFn: async () => {
            try {
                // Adjust this URL to match your Spring Boot controller for fetching batches
                return (await apiClient.get('/batches')).data;
            }
            catch { return []; }
        },
        enabled: activeTab === 'analytics'
    });

    // ── SAFE EXTRACTION & FORMATTING ──
    const safeArray = (arr) => Array.isArray(arr) ? arr : [];

    const safeAlerts = safeArray(alerts);
    const safeThreats = safeArray(threatData);
    const safePending = safeArray(pendingDrugs);
    const safeBatches = safeArray(batchesData);

    // Format trend data for the Line Chart (reversing so oldest dates are on the left)
    const formattedTrends = safeArray(trends).map(t => ({
        date: t.scanDate || t.scan_date || '',
        Scans: t.dailyScans || t.daily_scans || 0,
        Authentic: t.authenticScans || t.authentic_scans || 0
    })).reverse();

    // Derive Flow Data from the DB View Custody Transfers
    const rawTransfers = stats?.custodyTransfers || stats?.custody_transfers || 0;
    // Assuming a standard flow proportion for visualization if exact hop data isn't split in view
    const supplyChainFlow = [
        { name: 'Manufacturer', value: rawTransfers, fill: '#005f73' },
        { name: 'ZAMMSA', value: rawTransfers, fill: '#0a9396' },
        { name: 'Pharmacy POS', value: Math.ceil(rawTransfers / 2), fill: '#94d2bd' }
    ];

    // ── Mutations ──
    const labMutation = useMutation({
        mutationFn: async (data) => (await apiClient.post('/regulatory/scrutiny', data)).data,
        onSuccess: () => {
            alert('Lab inspection results securely recorded on the ledger!');
            setLabForm({ batchNumber: '', status: 'PENDING', notes: '' });
        }
    });

    const recallMutation = useMutation({

        mutationFn: async (data) => (await apiClient.post('/regulatory/recalls', data)).data,
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

    const handleLabSubmit = (e) => {
        e.preventDefault();

        // 1. Lookup the UUID for the entered Batch Number
        const targetBatch = safeBatches.find(b =>
            (b.batchNumber || b.batch_number) === labForm.batchNumber
        );

        // 2. Abort if the batch does not exist in the active registry
        if (!targetBatch) {
            alert("Validation Error: Batch Number not found in active registry. Please verify the batch ID.");
            return;
        }

        // 3. Transmit the mapped registryId instead of the raw batchNumber
        labMutation.mutate({
            registryId: targetBatch.registryId || targetBatch.registry_id,
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
            <aside className={styles.sidebar}>
                <div className={styles.sidebarBrand}>
                    <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>verified</span>
                    <span>Regulatory Framework</span>
                </div>

                <div className={`${styles.navItem} ${activeTab === 'analytics' ? styles.activeNav : ''}`} onClick={() => setActiveTab('analytics')}>
                    <span className="material-symbols-outlined">insights</span> Network Analytics
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

            <main className={styles.mainContent}>
                <div className={styles.contentGrid}>
                    <header className={styles.header}>
                        <div>
                            <h1 className={styles.title}>
                                {activeTab === 'analytics' && 'Supply Chain Analytics'}
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
                        {/* TAB: ANALYTICS (Clean Data-Driven View) */}
                        {activeTab === 'analytics' && (
                            <div className={styles.analyticsWrapper}>

                                {/* 1. TOP KPIs */}
                                <div className={styles.kpiGrid} style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
                                    <div className={styles.kpiCard}>
                                        <h4>Authentic Batches/Units</h4>
                                        <h2>{stats?.authenticBatchesTracked || stats?.authentic_batches_tracked || '0'}</h2>
                                    </div>
                                    <div className={styles.kpiCard}>
                                        <h4>Authentic Scans</h4>
                                        <h2>{stats?.authenticScans || stats?.authentic_scans || '0'}</h2>
                                    </div>
                                    <div className={styles.kpiCard}>
                                        <h4>Batch/Unit Transfers</h4>
                                        <h2>{stats?.custodyTransfers || stats?.custody_transfers || '0'}</h2>
                                    </div>
                                    <div className={styles.kpiCard}>
                                        <h4>Avg Sync Latency</h4>
                                        <h2>
                                            {stats?.avgSyncLatencySeconds || stats?.avg_sync_latency_seconds
                                                ? Number(stats?.avgSyncLatencySeconds || stats?.avg_sync_latency_seconds).toFixed(2)
                                                : '0.00'}s
                                        </h2>
                                    </div>
                                </div>

                                {/* 2. GRAPHS ROW */}
                                <div className={styles.chartsGrid2} style={{ marginTop: '24px' }}>

                                    {/* Line Chart: Verification Trends */}
                                    <div className={styles.chartCard}>
                                        <h3 style={{ margin: '0 0 24px', fontSize: '16px', color: 'var(--primary)' }}>30-Day Verification Trends</h3>
                                        {loadingTrends ? <p>Loading trends...</p> : (
                                            <ResponsiveContainer width="100%" height={280}>
                                                <LineChart data={formattedTrends} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#eee" />
                                                    <XAxis dataKey="date" tick={{fontSize: 12}} tickMargin={10} />
                                                    <YAxis tick={{fontSize: 12}} />
                                                    <Tooltip cursor={{ fill: 'transparent' }} contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                                    <Legend wrapperStyle={{ fontSize: '12px', marginTop: '10px' }} />
                                                    <Line type="monotone" dataKey="Scans" stroke="#8b5cf6" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                                                </LineChart>
                                            </ResponsiveContainer>
                                        )}
                                    </div>

                                    {/* Funnel Chart: Custody Flow */}
                                    <div className={styles.chartCard}>
                                        <h3 style={{ margin: '0 0 24px', fontSize: '16px', color: 'var(--primary)' }}>Batch Custody Flow</h3>
                                        <ResponsiveContainer width="100%" height={280}>
                                            <FunnelChart>
                                                <Tooltip contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                                <Funnel dataKey="value" data={supplyChainFlow} isAnimationActive>
                                                    <LabelList position="right" fill="#333" stroke="none" dataKey="name" fontSize={13} />
                                                </Funnel>
                                            </FunnelChart>
                                        </ResponsiveContainer>
                                    </div>
                                </div>

                                {/* 3. DATA TABLE ROW */}
                                <div className={styles.chartCard} style={{ marginTop: '24px' }}>
                                    <h3 style={{ margin: '0 0 16px', fontSize: '16px', color: 'var(--primary)' }}>Active Pharmaceutical Batches</h3>
                                    {loadingBatches ? <p>Loading batch registry...</p> : (
                                        <div style={{ overflowX: 'auto' }}>
                                            <table className={styles.analyticsTable} style={{ width: '100%', borderCollapse: 'collapse' }}>
                                                <thead>
                                                <tr style={{ borderBottom: '2px solid #eaeaea', backgroundColor: '#f8f9fa' }}>
                                                    <th style={{padding: '12px', textAlign: 'left'}}>Batch Number</th>
                                                    <th style={{padding: '12px', textAlign: 'left'}}>Product Name</th>
                                                    <th style={{padding: '12px', textAlign: 'left'}}>Mfg Date</th>
                                                    <th style={{padding: '12px', textAlign: 'left'}}>Expiry Date</th>
                                                    <th style={{padding: '12px', textAlign: 'right'}}>Status</th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                {safeBatches.slice(0, 10).map((batch, idx) => (
                                                    <tr key={idx} style={{ borderBottom: '1px solid #eaeaea' }}>
                                                        <td style={{padding: '12px', fontWeight: 'bold'}}>{batch?.batchNumber || batch?.batch_number || 'N/A'}</td>
                                                        <td style={{padding: '12px', color: '#555'}}>{batch?.productName || batch?.product_name || 'N/A'}</td>
                                                        <td style={{padding: '12px'}}>{batch?.manufacturingDate || batch?.manufacturing_date || 'N/A'}</td>
                                                        <td style={{padding: '12px'}}>{batch?.expiryDate || batch?.expiry_date || 'N/A'}</td>
                                                        <td style={{padding: '12px', textAlign: 'right'}}>
                                                                <span style={{
                                                                    background: '#e6f4ea',
                                                                    color: '#1e8e3e',
                                                                    padding: '4px 8px',
                                                                    borderRadius: '4px',
                                                                    fontSize: '11px',
                                                                    fontWeight: 'bold'
                                                                }}>
                                                                    {batch?.currentStatus || batch?.current_status || 'CONFIRMED'}
                                                                </span>
                                                        </td>
                                                    </tr>
                                                ))}
                                                {!safeBatches.length && (
                                                    <tr><td colSpan="5" style={{padding: '24px', textAlign: 'center', color: '#666'}}>No active batches found in registry.</td></tr>
                                                )}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* TAB: LAB SCRUTINY */}
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

                        {/* TAB: RECALLS */}
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

                        {/* TAB: APPROVALS */}
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
                                        {safePending.map((drug, idx) => (
                                            <tr key={idx}>
                                                <td>{drug?.productCode || 'N/A'}</td>
                                                <td>{drug?.genericName || 'N/A'}</td>
                                                <td>{drug?.brandName || 'N/A'}</td>
                                                <td style={{textAlign: 'right'}}>
                                                    <button className={styles.labBtn} style={{padding: '6px 12px'}} onClick={() => approveMutation.mutate(drug.productId)}>
                                                        Approve
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        {!safePending.length && <tr><td colSpan="4">No pending drugs requiring approval.</td></tr>}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}

                        {/* TAB: THREATS */}
                        {activeTab === 'threats' && (
                            <div className={styles.labZone}>
                                <h2>Suspicious Scanning Patterns</h2>
                                <p>Data aggregated from <code>vw_suspicious_patterns</code> detecting potential QR cloning.</p>
                                {loadingThreats ? <p>Analyzing network...</p> : (
                                    <table className={styles.inventoryTable} style={{ width: '100%', marginTop: '16px', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr style={{ borderBottom: '2px solid #eaeaea', backgroundColor: '#f8f9fa' }}>
                                            <th style={{padding: '12px', textAlign: 'left'}}>Device ID</th>
                                            <th style={{padding: '12px', textAlign: 'center'}}>Unique Products</th>
                                            <th style={{padding: '12px', textAlign: 'center'}}>Total Scans</th>
                                            <th style={{padding: '12px', textAlign: 'right'}}>Risk Level</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {safeThreats.map((threat, i) => (
                                            <tr key={i} style={{ borderBottom: '1px solid #eaeaea' }}>
                                                <td style={{padding: '12px', fontFamily: 'monospace'}}>{threat?.deviceFingerprint?.substring(0,16) || threat?.device_fingerprint?.substring(0,16)}...</td>
                                                <td style={{padding: '12px', textAlign: 'center'}}>{threat?.uniqueProducts || threat?.unique_products || 0}</td>
                                                <td style={{padding: '12px', textAlign: 'center'}}>{threat?.totalScans || threat?.total_scans || 0}</td>
                                                <td style={{padding: '12px', textAlign: 'right'}}>
                                                        <span className={`${styles.statusBadge} ${(threat?.riskLevel || threat?.risk_level) === 'HIGH_RISK' ? styles.statusDanger : ''}`}>
                                                            {threat?.riskLevel || threat?.risk_level || 'UNKNOWN'}
                                                        </span>
                                                </td>
                                            </tr>
                                        ))}
                                        {!safeThreats.length && <tr><td colSpan="4" style={{padding: '24px', textAlign: 'center'}}>No suspicious activity detected.</td></tr>}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Right Column Alerts (Kept static across all tabs) */}
                    <aside className={styles.alertsPanel}>
                        <div className={styles.alertsHeader}>
                            <h3>Live Network Alerts</h3>
                            <div className={styles.pulseDot}></div>
                        </div>
                        <div>
                            {safeAlerts.length > 0 ? (
                                safeAlerts.map((alert, index) => (
                                    <div className={styles.timelineItem} key={index}>
                                        <div className={styles.timelineConnector}></div>
                                        <div className={`${styles.timelineDot} ${(alert?.scanCount || alert?.total_scans) > 10 ? styles.timelineDotAlert : ''}`}></div>
                                        <div className={styles.timelineContent}>
                                            <h4>{(alert?.riskLevel || alert?.risk_level) === 'HIGH_RISK' ? 'Critical Clone Risk' : 'Suspicious Velocity'}</h4>
                                            <p style={{ margin: '4px 0' }}>
                                                Device scanned {alert?.uniqueProducts || alert?.unique_products || 0} unique batches rapidly.
                                            </p>
                                            <small style={{ fontSize: '10px', color: '#999', wordBreak: 'break-all' }}>
                                                Device: {alert?.deviceFingerprint?.substring(0, 16) || alert?.device_fingerprint?.substring(0, 16)}...
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