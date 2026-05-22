import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import { useNavigate } from 'react-router-dom';
import styles from './PharmacyDashboard.module.css';

export default function PharmacyDashboard() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const [activeTab, setActiveTab] = useState('inventory');
    const [printBatch, setPrintBatch] = useState(null);

    // ── Data Fetching: Live Inventory & History ──
    const { data: batches, isLoading: isLoadingInventory } = useQuery({
        queryKey: ['pharmacyBatches'],
        queryFn: async () => (await apiClient.get('/batches')).data,
        refetchInterval: 15000
    });

    const { data: history } = useQuery({
        queryKey: ['pharmacyHistory'],
        queryFn: async () => {
            try {
                return (await apiClient.get('/custody/history')).data;
            } catch (e) {
                return [];
            }
        },
        refetchInterval: 15000
    });

    const { data: units } = useQuery({
        queryKey: ['pharmacyUnits'],
        queryFn: async () => {
            try {
                return (await apiClient.get('/units')).data;
            } catch (e) {
                return [];
            }
        },
        refetchInterval: 15000
    });

    const { data: alerts } = useQuery({
        queryKey: ['securityAlerts'],
        queryFn: async () => (await apiClient.get('/analytics/suspicious-scans')).data,
        enabled: activeTab === 'alerts'
    });

    // ── Analytics Calculations ──
    const activeInventory = batches?.filter(b => b.currentStatus === 'CONFIRMED' || b.currentStatus === 'DELIVERED') || [];

    const dispensedUnits = units?.filter(u => u.currentStatus === 'DISPENSED') || [];

    // Combine Batch History and Individual Dispensed Units for the Ledger View
    // Combine Batch History and Individual Dispensed Units for the Ledger View
    const combinedLedger = [
        ...(history || []),
        ...dispensedUnits.map(unit => {
            // Lookup parent batch to extract the exact product name from the registry
            const parentBatch = batches?.find(b =>
                (b.registryId && b.registryId === unit.registryId) ||
                (b.registry_id && b.registry_id === unit.registry_id) ||
                (unit.serialNumber || unit.serial_number)?.includes(b.batchNumber || b.batch_number)
            );

            return {
                eventType: unit.currentStatus || unit.current_status,
                identifier: unit.serialNumber || unit.serial_number,
                productName: parentBatch ? (parentBatch.productName || parentBatch.product_name) : 'Pending Sync',
                status: unit.currentStatus || unit.current_status
            };
        })
    ];

    const handlePrintLabels = (batch) => {
        setPrintBatch(batch);
        setActiveTab('print');
    };

    // ── Helpers ──
    const downloadQrCode = async (batchNumber) => {
        try {
            const response = await apiClient.get(`/batches/${batchNumber}/qrcode`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `PharmaTrust_BatchQR_${batchNumber}.png`);
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Failed to download QR Code", error);
            alert("Could not download QR code. Ensure the batch is CONFIRMED.");
        }
    };

    const downloadUnitQrCode = async (serialNumber) => {
        try {
            const response = await apiClient.get(`/units/${serialNumber}/qrcode`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `PharmaTrust_Unit_${serialNumber}.png`);
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Failed to download Unit QR Code", error);
            alert(`Could not download QR code for unit ${serialNumber}.`);
        }
    };

    return (
        <div className={styles.dashboardWrapper}>
            <aside className={styles.sidebar}>
                <div className={styles.sidebarBrand}>
                    <span className="material-symbols-outlined">local_pharmacy</span>
                    <span>Facility Node</span>
                </div>

                <div className={`${styles.navItem} ${activeTab === 'inventory' || activeTab === 'print' ? styles.activeNav : ''}`} onClick={() => setActiveTab('inventory')}>
                    <span className="material-symbols-outlined">inventory_2</span> Live Inventory
                </div>

                <div className={`${styles.navItem} ${activeTab === 'history' ? styles.activeNav : ''}`} onClick={() => setActiveTab('history')}>
                    <span className="material-symbols-outlined">receipt_long</span> Ledger History
                </div>

                <div className={`${styles.navItem} ${activeTab === 'operations' ? styles.activeNav : ''}`} onClick={() => setActiveTab('operations')}>
                    <span className="material-symbols-outlined">point_of_sale</span> POS Operations
                </div>

                <div className={`${styles.navItem} ${activeTab === 'alerts' ? styles.activeNav : ''}`} onClick={() => setActiveTab('alerts')}>
                    <span className="material-symbols-outlined">warning</span> Security Alerts
                </div>

                <button onClick={logout} className={`${styles.navItem} ${styles.logoutBtn}`} style={{marginTop: 'auto', width: '100%'}}>
                    <span className="material-symbols-outlined">logout</span> Sign Out
                </button>
            </aside>

            <main className={styles.mainContent}>
                <header className={styles.header}>
                    <h1 className={styles.title}>
                        {activeTab === 'inventory' && "Live Inventory"}
                        {activeTab === 'history' && "Ledger History"}
                        {activeTab === 'operations' && "POS Operations"}
                        {activeTab === 'alerts' && "Security Monitoring"}
                        {activeTab === 'print' && "Print Unit Labels"}
                    </h1>
                    <p className={styles.subtitle}>{user?.username || 'Pharmacy Admin'} • End-Point Dispensing</p>
                </header>

                {/* VIEW 1: LIVE INVENTORY */}
                {activeTab === 'inventory' && (
                    <>
                        <div className={styles.dashboardGrid} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '32px' }}>
                            <div className={styles.actionCard} style={{ background: 'white', padding: '24px', borderRadius: '16px', border: '1px solid var(--border)' }}>
                                <div className={styles.iconCircle} style={{backgroundColor: '#e3f2fd', color: '#1976d2', padding: '12px', borderRadius: '50%', display: 'inline-block', marginBottom: '16px'}}>
                                    <span className="material-symbols-outlined">medication</span>
                                </div>
                                <h3 style={{ margin: '0 0 8px 0' }}>{activeInventory.length} Active Batches</h3>
                                <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>Medications ready for patient dispensing.</p>
                            </div>

                            <div className={styles.actionCard} style={{ background: 'white', padding: '24px', borderRadius: '16px', border: '1px solid var(--border)' }}>
                                <div className={styles.iconCircle} style={{backgroundColor: '#e6f4ea', color: '#1e8e3e', padding: '12px', borderRadius: '50%', display: 'inline-block', marginBottom: '16px'}}>
                                    <span className="material-symbols-outlined">how_to_reg</span>
                                </div>
                                <h3 style={{ margin: '0 0 8px 0' }}>{dispensedUnits.length} Units Dispensed</h3>
                                <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>Individual units successfully logged to patients.</p>
                            </div>
                        </div>

                        <section className={styles.scannerZone}>
                            <h3 style={{ marginBottom: '16px' }}>Current Stock</h3>
                            <div style={{ background: 'white', borderRadius: '12px', overflow: 'hidden', border: '1px solid #eaeaea', overflowX: 'auto' }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontFamily: 'var(--font-ui)' }}>
                                    <thead style={{ backgroundColor: '#f8f9fa', borderBottom: '2px solid #eaeaea' }}>
                                    <tr>
                                        <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Batch #</th>
                                        <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Product</th>
                                        <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Expiry Date</th>
                                        <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Status</th>
                                        <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {isLoadingInventory ? (
                                        <tr><td colSpan="5" style={{ padding: '24px', textAlign: 'center' }}>Syncing with Ledger...</td></tr>
                                    ) : activeInventory.length === 0 ? (
                                        <tr><td colSpan="5" style={{ padding: '24px', textAlign: 'center' }}>No active stock available.</td></tr>
                                    ) : activeInventory.map(batch => (
                                        <tr key={batch.batchNumber} style={{ borderBottom: '1px solid #eaeaea' }}>
                                            <td style={{ padding: '16px', fontFamily: 'monospace', fontWeight: 'bold' }}>{batch.batchNumber}</td>
                                            <td style={{ padding: '16px' }}>{batch.productName}</td>
                                            <td style={{ padding: '16px' }}>{batch.expiryDate}</td>
                                            <td style={{ padding: '16px' }}>
                                                <span style={{ background: '#e6f4ea', color: '#1e8e3e', padding: '6px 12px', borderRadius: '24px', fontSize: '12px', fontWeight: 'bold' }}>
                                                    IN STOCK
                                                </span>
                                            </td>
                                            <td style={{ padding: '16px', display: 'flex', gap: '8px' }}>
                                                <button onClick={() => downloadQrCode(batch.batchNumber)} className={styles.btnAction} style={{ backgroundColor: '#2a9d8f', color: 'white', border: 'none' }}>
                                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', marginRight: '4px' }}>qr_code_2</span>
                                                    Batch QR
                                                </button>
                                                <button onClick={() => handlePrintLabels(batch)} className={styles.btnAction} style={{ backgroundColor: '#e76f51', color: 'white', border: 'none' }}>
                                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', marginRight: '4px' }}>print</span>
                                                    Unit Labels
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </>
                )}

                {/* VIEW 2: PRINT LABELS */}
                {activeTab === 'print' && printBatch && (
                    <section className={styles.scannerZone}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                <button
                                    onClick={() => setActiveTab('inventory')}
                                    style={{ background: '#e9ecef', color: '#333', border: 'none', padding: '8px 16px', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', fontWeight: 'bold' }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px', marginRight: '4px' }}>arrow_back</span>
                                    Back
                                </button>
                                <h2 style={{margin: 0}}>Batch: {printBatch.batchNumber}</h2>
                            </div>

                            <button onClick={() => window.print()} className={styles.btnPrimary} style={{ backgroundColor: '#e76f51', border: 'none', padding: '10px 20px', borderRadius: '4px', color: 'white', cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
                                <span className="material-symbols-outlined" style={{ marginRight: '8px' }}>print</span>
                                Print {printBatch.batchUnitCount || 0} Labels
                            </button>
                        </div>

                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '20px' }}>
                            {Array.from({ length: printBatch.batchUnitCount || 0 }, (_, i) => i + 1).map(num => {
                                const serialNumber = `${printBatch.batchNumber}-SN${String(num).padStart(3, '0')}`;
                                return (
                                    <div key={serialNumber} style={{ border: '1px solid #e0e0e0', padding: '16px', textAlign: 'center', borderRadius: '8px', background: '#fafafa', width: '220px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                                        <p style={{ margin: '0 0 8px 0', fontWeight: 'bold', fontSize: '14px', color: 'var(--primary)' }}>{printBatch.productName}</p>

                                        <img
                                            src={`http://localhost:8080/api/v1/units/${serialNumber}/qrcode?t=${new Date().getTime()}`}
                                            alt={`QR for ${serialNumber}`}
                                            style={{ width: '150px', height: '150px', marginBottom: '12px', background: 'white', padding: '4px', borderRadius: '4px' }}
                                        />

                                        <p style={{ margin: '0 0 12px 0', fontSize: '12px', fontFamily: 'monospace', background: '#e9ecef', padding: '4px 8px', borderRadius: '4px' }}>
                                            {serialNumber}
                                        </p>

                                        <button
                                            onClick={() => downloadUnitQrCode(serialNumber)}
                                            style={{ background: '#1976d2', color: 'white', border: 'none', padding: '8px', borderRadius: '4px', cursor: 'pointer', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '16px', marginRight: '4px' }}>download</span>
                                            Download QR
                                        </button>
                                    </div>
                                );
                            })}
                        </div>
                    </section>
                )}

                {/* VIEW 3: LEDGER HISTORY */}
                {activeTab === 'history' && (
                    <section className={styles.scannerZone}>
                        <h3 style={{ marginBottom: '16px' }}>Dispensed & Received Records</h3>
                        <div style={{ background: 'white', borderRadius: '12px', overflow: 'hidden', border: '1px solid #eaeaea', overflowX: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontFamily: 'var(--font-ui)' }}>
                                <thead style={{ backgroundColor: '#f8f9fa', borderBottom: '2px solid #eaeaea' }}>
                                <tr>
                                    <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Event / Action</th>
                                    <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Identifier</th>
                                    <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Product</th>
                                    <th style={{ padding: '16px', fontSize: '13px', color: '#555' }}>Status</th>
                                </tr>
                                </thead>
                                <tbody>
                                {combinedLedger?.length > 0 ? combinedLedger.map((record, index) => (
                                    <tr key={index} style={{ borderBottom: '1px solid #eaeaea' }}>
                                        <td style={{ padding: '16px', fontWeight: '500' }}>
                                            {record.eventType === 'DISPENSED' ? (
                                                <span style={{ color: '#1e8e3e', display: 'flex', alignItems: 'center', gap: '4px' }}>
                                                    <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>prescriptions</span>
                                                    {record.eventType}
                                                </span>
                                            ) : (
                                                record.eventType || 'INBOUND RECEIPT'
                                            )}
                                        </td>
                                        <td style={{ padding: '16px', fontFamily: 'monospace' }}>{record.batchNumber || record.identifier || 'N/A'}</td>
                                        <td style={{ padding: '16px' }}>{record.productName || 'N/A'}</td>
                                        <td style={{ padding: '16px' }}>
                                            <span style={{ color: record.eventType === 'DISPENSED' ? '#1e8e3e' : '#1976d2', fontWeight: '600', fontSize: '13px' }}>
                                                <span className="material-symbols-outlined" style={{ fontSize: '14px', verticalAlign: 'middle', marginRight: '4px' }}>
                                                    {record.status === 'DISPENSED' ? 'verified' : 'link'}
                                                </span>
                                                {record.status === 'DISPENSED' ? 'Ledger Verified' : (record.status || 'CONFIRMED')}
                                            </span>
                                        </td>
                                    </tr>
                                )) : (
                                    <tr><td colSpan="4" style={{ padding: '24px', textAlign: 'center' }}>No ledger history found.</td></tr>
                                )}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}

                {/* VIEW 4: POS OPERATIONS NAVIGATION */}
                {activeTab === 'operations' && (
                    <div className={styles.dashboardGrid}>
                        <div className={styles.actionCard} onClick={() => navigate('/pharmacy/dispense')}>
                            <div className={`${styles.iconCircle} ${styles.bgPrimaryLight}`}><span className="material-symbols-outlined">prescriptions</span></div>
                            <h3>Dispense Item</h3>
                        </div>
                        <div className={styles.actionCard} onClick={() => navigate('/handover')}>
                            <div className={styles.iconCircle} style={{backgroundColor: '#FFF8E1', color: '#F57F17'}}><span className="material-symbols-outlined">assignment_return</span></div>
                            <h3>Return Stock</h3>
                        </div>
                        <div className={styles.actionCard} onClick={() => navigate('/audit')}>
                            <div className={styles.iconCircle} style={{backgroundColor: '#E8F5E9', color: '#2E7D32'}}><span className="material-symbols-outlined">history</span></div>
                            <h3>Provenance Audit</h3>
                        </div>

                    </div>
                )}

                {/* VIEW 5: SECURITY ALERTS */}
                {activeTab === 'alerts' && (
                    <section className={styles.scannerZone}>
                        <h3 style={{marginBottom: '24px'}}>Regional Security Alerts</h3>
                        {alerts?.length > 0 ? (
                            alerts.map((alert, i) => (
                                <div key={i} className={styles.resultCard} style={{marginBottom: '16px', borderLeft: '5px solid #d32f2f', textAlign: 'left', marginTop: 0}}>
                                    <h4 style={{margin: '0 0 8px 0', color: '#d32f2f'}}>{alert.riskLevel} Risk Detected</h4>
                                    <strong>{alert.productName}</strong>
                                    <p style={{margin: '4px 0'}}>Multiple scans detected for Batch {alert.batchNumber}.</p>
                                </div>
                            ))
                        ) : (
                            <div style={{ textAlign: 'center', padding: '40px', color: '#666', background: '#f9f9f9', borderRadius: '12px' }}>
                                <span className="material-symbols-outlined" style={{ fontSize: '48px', color: '#ccc', marginBottom: '16px' }}>health_and_safety</span>
                                <p>No suspicious patterns detected. System is secure.</p>
                            </div>
                        )}
                    </section>
                )}


            </main>
        </div>
    );
}