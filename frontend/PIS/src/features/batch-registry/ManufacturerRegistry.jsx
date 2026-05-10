import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import styles from './ManufacturerRegistry.module.css';
import { useNavigate } from 'react-router-dom';

export default function ManufacturerDashboard() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState('inventory'); // inventory, products, mint

    // 1. DATA FETCHING (Products and Batches)
    const { data: products } = useQuery({
        queryKey: ['products'],
        queryFn: async () => (await apiClient.get('products')).data
    });

    const { data: inventory, isLoading: isLoadingInventory } = useQuery({
        queryKey: ['myInventory'],
        queryFn: async () => (await apiClient.get('batches')).data,
        refetchInterval: 30000
    });

    // 2. MUTATIONS (Batch & Product Creation)
    const productMutation = useMutation({
        mutationFn: async (data) => (await apiClient.post('products', data)).data,
        onSuccess: () => {
            queryClient.invalidateQueries(['products']);
            setActiveTab('products');
            alert("Drug successfully added to catalog.");
        }
    });

    const batchMutation = useMutation({
        mutationFn: async (data) => (await apiClient.post('batches', data)).data,
        onSuccess: () => {
            queryClient.invalidateQueries(['myInventory']);
            setActiveTab('inventory');
        }
    });

    // Helper: Register Asset to Blockchain
    const registerToBlockchain = useMutation({
        mutationFn: async (batchNumber) => (await apiClient.post(`batches/${batchNumber}/register`)).data,
        onSuccess: () => queryClient.invalidateQueries(['myInventory'])
    });

    // Helper: Download QR Code from Backend
    const downloadQrCode = async (batchNumber) => {
        try {
            // Explicitly request a blob so Axios doesn't try to parse the PNG as JSON
            const response = await apiClient.get(`batches/${batchNumber}/qrcode`, {
                responseType: 'blob'
            });

            // Create a temporary local URL for the downloaded image blob
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a'); // 'a' stands for anchor tag
            link.href = url;
            link.setAttribute('download', `PharmaTrust_QR_${batchNumber}.png`); // File name
            document.body.appendChild(link);
            link.click();

            // Clean up
            link.parentNode.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Failed to download QR Code", error);
            alert("Could not download QR code. Ensure the batch is CONFIRMED.");
        }
    };

    return (
        <div className={styles.dashboardWrapper}>
            {/* SIDEBAR NAVIGATION */}
            <aside className={styles.sidebar}>
                <div className={styles.sidebarBrand}>
                    <span className="material-symbols-outlined">health_and_safety</span>
                    <span>Blockchain Based Pharmaceutical Integrity System</span>
                </div>
                <div className={`${styles.navItem} ${activeTab === 'inventory' ? styles.activeNav : ''}`} onClick={() => setActiveTab('inventory')}>
                    <span className="material-symbols-outlined">inventory_2</span> Drug Catalog
                </div>
                <div className={`${styles.navItem} ${activeTab === 'products' ? styles.activeNav : ''}`} onClick={() => setActiveTab('products')}>
                    <span className="material-symbols-outlined">medication</span> Register Drug
                </div>
                <div className={`${styles.navItem} ${activeTab === 'mint' ? styles.activeNav : ''}`} onClick={() => setActiveTab('mint')}>
                    <span className="material-symbols-outlined">add_box</span> Mint New Batch
                </div>
                <div className={styles.navItem} onClick={() => navigate('/handover')}>
                    <span className="material-symbols-outlined">local_shipping</span> Transfer Custody
                </div>
                <div style={{ marginTop: 'auto' }} className={styles.navItem} onClick={logout}>
                    <span className="material-symbols-outlined">logout</span> Logout
                </div>
            </aside>

            <main className={styles.mainContent}>
                <header className={styles.header}>
                    <h1 className={styles.title}>
                        {activeTab === 'inventory' && "Drug Catalog"}
                        {activeTab === 'products' && "Register Drug"}
                        {activeTab === 'mint' && "Mint New Batch"}
                    </h1>
                    <p className={styles.subtitle}>{user?.username} • Manufacturer Node</p>
                </header>

                {/* VIEW 1: DRUG CATALOG */}
                {activeTab === 'inventory' && (
                    <section className={styles.inventorySection}>
                        <div className={styles.tableContainer}>
                            <table className={styles.inventoryTable}>
                                <thead>
                                <tr>
                                    <th>Batch #</th>
                                    <th>Product</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                {inventory?.map(batch => (
                                    <tr key={batch.batchNumber}>
                                        <td>{batch.batchNumber}</td>
                                        <td>{batch.productName}</td>

                                        {/* FIXED: Using currentStatus for the pill */}
                                        <td>
                                            <span className={`${styles.statusPill} ${batch.currentStatus === 'CONFIRMED' ? styles.statusPassed : styles.statusTransit}`}>
                                                {batch.currentStatus}
                                            </span>
                                        </td>

                                        <td style={{ display: 'flex', gap: '8px' }}>

                                            {/* FIXED: Using currentStatus for the Register button */}
                                            {batch.currentStatus === 'PENDING_BLOCKCHAIN' && (
                                                <button onClick={() => registerToBlockchain.mutate(batch.batchNumber)} className={styles.btnAction}>
                                                    Register on Chain
                                                </button>
                                            )}

                                            {/* FIXED: Using currentStatus for the Download QR button */}
                                            {(batch.currentStatus === 'CONFIRMED' || batch.currentStatus === 'PENDING_CONFIRMATION') && (
                                                <button
                                                    onClick={() => downloadQrCode(batch.batchNumber)}
                                                    className={styles.btnAction}
                                                    style={{ backgroundColor: '#2a9d8f', color: 'white', border: 'none' }}
                                                >
                                                    <span className="material-symbols-outlined" style={{ fontSize: '16px', marginRight: '4px' }}>qr_code_2</span>
                                                    Download QR
                                                </button>
                                            )}

                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}

                {/* VIEW 2: REGISTER DRUG */}
                {activeTab === 'products' && (
                    <div className={styles.formCard}>
                        <form onSubmit={(e) => {
                            e.preventDefault();
                            const data = new FormData(e.target);
                            productMutation.mutate(Object.fromEntries(data.entries()));
                        }}>
                            <div className={styles.formGrid}>
                                <div className={styles.inputGroup}><label>Product Code</label><input name="productCode" required className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Generic Name</label><input name="genericName" required className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Brand Name</label><input name="brandName" className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Dosage Form</label><input name="dosageForm" placeholder="e.g. Syrup" className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Strength</label><input name="strength" placeholder="e.g. 500mg, 100 IU/mL" className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Therapeutic Class</label><input name="therapeuticClass" className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Approved by ZAMRA</label><select name="approvedByZamra" className={styles.select}><option value="true">Yes</option><option value="false">No</option></select></div>
                            </div>
                            {/* FIXED: Added formActions wrapper for padding and alignment */}
                            <div className={styles.formActions}>
                                <button type="submit" className={styles.btnPrimary}>Create Product Entry</button>
                            </div>
                        </form>
                    </div>
                )}

                {/* VIEW 3: MINT BATCH FORM */}
                {activeTab === 'mint' && (
                    <div className={styles.formCard}>
                        <form onSubmit={(e) => {
                            e.preventDefault();
                            const data = new FormData(e.target);
                            batchMutation.mutate({
                                ...Object.fromEntries(data.entries()),
                                manufacturerId: user.participantId
                            });
                        }}>
                            <div className={styles.formGrid}>
                                <div className={`${styles.inputGroup} ${styles.fullWidth}`}>
                                    <label>Select Product</label>
                                    <select name="productId" className={styles.select} required>
                                        <option value="">Choose drug...</option>
                                        {products?.map(p => (
                                            <option key={p.productId} value={p.productId}>{p.brandName} ({p.genericName})</option>
                                        ))}
                                    </select>
                                </div>
                                <div className={styles.inputGroup}><label>Batch Number</label><input name="batchNumber" required className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Mfg Date</label><input type="date" name="manufacturingDate" className={styles.input} /></div>
                                <div className={styles.inputGroup}><label>Expiry Date</label><input type="date" name="expiryDate" className={styles.input} /></div>
                            </div>
                            {/* FIXED: Added formActions wrapper for padding and alignment */}
                            <div className={styles.formActions}>
                                <button type="submit" className={styles.btnPrimary}>Register on Ledger</button>
                            </div>
                        </form>
                    </div>
                )}
            </main>
        </div>
    );
}