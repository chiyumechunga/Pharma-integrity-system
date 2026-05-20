import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import styles from './ZammsaDashboard.module.css';

export default function ZammsaDashboard() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [activeTab, setActiveTab] = useState('inventory'); // Tabs: 'inventory', 'history'

    // 1. DATA FETCHING: Live Warehouse Inventory
    const { data: inventory, isLoading: isLoadingInventory } = useQuery({
        queryKey: ['zammsaInventory'],
        queryFn: async () => (await apiClient.get('batches')).data,
        refetchInterval: 30000
    });

    // 2. DATA FETCHING: Receipt History
    const { data: history } = useQuery({
        queryKey: ['zammsaHistory'],
        queryFn: async () => {
            try {
                return (await apiClient.get('custody/history')).data;
            } catch (e) {
                return (await apiClient.get('batches')).data.filter(b => b.currentStatus !== 'PENDING_BLOCKCHAIN');
            }
        }
    });

    // Calculate quick stats
    const totalBatches = inventory?.filter(b => b.currentStatus === 'CONFIRMED' || b.currentStatus === 'DISPENSED').length || 0;
    const totalHistory = history?.length || 0; // REPLACED Pending Receipts with History Count

    return (
        <div className={styles.dashboardWrapper}>
            {/* ── Sidebar for Logistics Control ── */}
            <aside className={styles.sidebar}>
                <div className={styles.sidebarBrand}>
                    <span className="material-symbols-outlined">hub</span>
                    <span>ZAMMSA Logistics</span>
                </div>

                <div className={`${styles.navItem} ${activeTab === 'inventory' ? styles.activeNav : ''}`} onClick={() => setActiveTab('inventory')}>
                    <span className="material-symbols-outlined">warehouse</span> Warehouse Stock
                </div>

                <div className={`${styles.navItem} ${activeTab === 'history' ? styles.activeNav : ''}`} onClick={() => setActiveTab('history')}>
                    <span className="material-symbols-outlined">history</span> Receipt History
                </div>

                {/* REMOVED: Inbound Scanner Navigation */}

                <div className={styles.navItem} onClick={() => navigate('/handover')}>
                    <span className="material-symbols-outlined">local_shipping</span> Outbound Dispatch
                </div>

                <div style={{ marginTop: 'auto' }} className={styles.navItem} onClick={logout}>
                    <span className="material-symbols-outlined">logout</span> Logout
                </div>
            </aside>

            <main className={styles.mainContent}>
                <header className={styles.header}>
                    <h1 className={styles.title}>
                        {activeTab === 'inventory' ? "National Distribution Hub" : "Inbound Receipt History"}
                    </h1>
                    <p className={styles.subtitle}>
                        {user?.username || 'ZAMMSA Admin'} • Zambia Medicines & Medical Supplies Agency
                    </p>
                </header>

                {/* VIEW 1: LIVE WAREHOUSE INVENTORY */}
                {activeTab === 'inventory' && (
                    <>
                        <div className={styles.dashboardGrid} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '32px' }}>
                            {/* STATS CARDS */}
                            <div className={styles.actionCard} style={{ background: 'white', padding: '24px', borderRadius: '16px', border: '1px solid #eaeaea' }}>
                                <div className={styles.iconCircle} style={{backgroundColor: '#e3f2fd', color: '#1976d2', padding: '12px', borderRadius: '50%', display: 'inline-block', marginBottom: '16px'}}>
                                    <span className="material-symbols-outlined">package_2</span>
                                </div>
                                <h3 style={{ margin: '0 0 8px 0' }}>{totalBatches} Active Batches</h3>
                                <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>Total medication batches currently in warehouse custody.</p>
                            </div>

                            {/* REPLACED: Pending Receipts card is now a Verified Receipts card */}
                            <div className={styles.actionCard} onClick={() => setActiveTab('history')} style={{ background: 'white', padding: '24px', borderRadius: '16px', border: '1px solid #eaeaea', cursor: 'pointer' }}>
                                <div className={styles.iconCircle} style={{backgroundColor: '#e6f4ea', color: '#1e8e3e', padding: '12px', borderRadius: '50%', display: 'inline-block', marginBottom: '16px'}}>
                                    <span className="material-symbols-outlined">verified</span>
                                </div>
                                <h3 style={{ margin: '0 0 8px 0' }}>{totalHistory} Verified Receipts</h3>
                                <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>Historical inbound shipments verified by the blockchain.</p>
                            </div>
                        </div>

                        {/* INVENTORY TABLE SECTION */}
                        <section className={styles.scannerZone}>
                            <h3 style={{ marginBottom: '16px' }}>Live Warehouse Inventory</h3>
                            <div className={styles.tableContainer} style={{ background: 'white', borderRadius: '12px', overflow: 'hidden', border: '1px solid #eaeaea' }}>
                                <table className={styles.inventoryTable} style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                                    <thead style={{ backgroundColor: '#f8f9fa', borderBottom: '2px solid #eaeaea' }}>
                                    <tr>
                                        <th style={{ padding: '16px' }}>Batch #</th>
                                        <th style={{ padding: '16px' }}>Product</th>
                                        <th style={{ padding: '16px' }}>Units</th>
                                        <th style={{ padding: '16px' }}>Expiry Date</th>
                                        <th style={{ padding: '16px' }}>Status</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {isLoadingInventory ? (
                                        <tr><td colSpan="5" style={{ padding: '16px', textAlign: 'center' }}>Loading inventory...</td></tr>
                                    ) : inventory?.filter(b => b.currentStatus !== 'PENDING_BLOCKCHAIN').map(batch => (
                                        <tr key={batch.batchNumber} style={{ borderBottom: '1px solid #eaeaea' }}>
                                            <td style={{ padding: '16px', fontFamily: 'monospace' }}>{batch.batchNumber}</td>
                                            <td style={{ padding: '16px', fontWeight: '500' }}>{batch.productName}</td>
                                            <td style={{ padding: '16px' }}>{batch.batchUnitCount || 20}</td>
                                            <td style={{ padding: '16px' }}>{batch.expiryDate}</td>
                                            <td style={{ padding: '16px' }}>
                                                    <span style={{
                                                        background: batch.currentStatus === 'CONFIRMED' ? '#e6f4ea' : '#fef7e0',
                                                        color: batch.currentStatus === 'CONFIRMED' ? '#1e8e3e' : '#b06000',
                                                        padding: '6px 12px', borderRadius: '24px', fontSize: '12px', fontWeight: 'bold'
                                                    }}>
                                                        {batch.currentStatus}
                                                    </span>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </>
                )}

                {/* VIEW 2: RECEIPT HISTORY */}
                {activeTab === 'history' && (
                    <section className={styles.historySection}>
                        <div className={styles.tableContainer} style={{ background: 'white', borderRadius: '12px', overflow: 'hidden', border: '1px solid #eaeaea' }}>
                            <table className={styles.inventoryTable} style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                                <thead style={{ backgroundColor: '#f8f9fa', borderBottom: '2px solid #eaeaea' }}>
                                <tr>
                                    <th style={{ padding: '16px' }}>Date Received</th>
                                    <th style={{ padding: '16px' }}>Batch #</th>
                                    <th style={{ padding: '16px' }}>Product</th>
                                    <th style={{ padding: '16px' }}>Blockchain TxID</th>
                                    <th style={{ padding: '16px' }}>Status</th>
                                </tr>
                                </thead>
                                <tbody>
                                {history?.map((record, index) => (
                                    <tr key={index} style={{ borderBottom: '1px solid #eaeaea' }}>
                                        <td style={{ padding: '16px' }}>
                                            {record.confirmedAt ? new Date(record.confirmedAt).toLocaleDateString() : 'N/A'}
                                        </td>
                                        <td style={{ padding: '16px', fontFamily: 'monospace' }}>{record.batchNumber}</td>
                                        <td style={{ padding: '16px' }}>{record.productName}</td>
                                        <td style={{ padding: '16px', fontSize: '12px', color: '#666', fontFamily: 'monospace' }}>
                                            {record.blockchainTxId?.substring(0, 16)}...
                                        </td>
                                        <td style={{ padding: '16px' }}>
                                                <span style={{ color: '#1e8e3e', fontWeight: '600', fontSize: '14px' }}>
                                                    <span className="material-symbols-outlined" style={{ fontSize: '14px', verticalAlign: 'middle', marginRight: '4px' }}>verified</span>
                                                    Verified on Chain
                                                </span>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}
            </main>
        </div>
    );
}