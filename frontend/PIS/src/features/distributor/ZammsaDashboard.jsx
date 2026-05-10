// src/features/distributor/ZammsaDashboard.jsx
import React, { useState } from 'react';
import styles from '../pharmacy/PharmacyDashboard.module.css'; // Reuse your clean styles!
import { useNavigate } from 'react-router-dom';

export default function ZammsaDashboard() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('inventory');

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
                <div className={styles.navItem} onClick={() => navigate('/receive-inventory')}>
                    <span className="material-symbols-outlined">inventory</span> Inbound Verification
                </div>
                <div className={styles.navItem} onClick={() => navigate('/handover')}>
                    <span className="material-symbols-outlined">local_shipping</span> Outbound Distribution
                </div>
                <div style={{ marginTop: 'auto' }} className={styles.navItem} onClick={logout}>
                    <span className="material-symbols-outlined">logout</span> Logout
                </div>
            </aside>

            <main className={styles.mainContent}>
                <header className={styles.header}>
                    <h1 className={styles.title}>National Distribution Hub</h1>
                    <p className={styles.subtitle}>Zambia Medicines & Medical Supplies Agency</p>
                </header>

                <div className={styles.dashboardGrid}>
                    {/* STATS CARDS */}
                    <div className={styles.actionCard}>
                        <div className={styles.iconCircle} style={{backgroundColor: '#e3f2fd', color: '#1976d2'}}>
                            <span className="material-symbols-outlined">package_2</span>
                        </div>
                        <h3>Total Batches</h3>
                        <p>Total medication batches currently in warehouse custody.</p>
                    </div>

                    <div className={styles.actionCard} onClick={() => navigate('/receive-inventory')}>
                        <div className={styles.iconCircle} style={{backgroundColor: '#fff3e0', color: '#f57c00'}}>
                            <span className="material-symbols-outlined">pending_actions</span>
                        </div>
                        <h3>Pending Receipts</h3>
                        <p>Batches in transit from manufacturers awaiting verification[cite: 2].</p>
                    </div>
                </div>

                {/* INVENTORY TABLE SECTION */}
                <section className={styles.scannerZone} style={{marginTop: '32px'}}>
                    <h3>Live Warehouse Inventory</h3>
                    <table className={styles.inventoryTable}>
                        {/* Map your batches here, filtered by ZAMMSA's participantId */}
                    </table>
                </section>
            </main>
        </div>
    );
}