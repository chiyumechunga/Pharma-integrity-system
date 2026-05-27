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
    const [activeTab, setActiveTab] = useState('inventory'); // inventory, products, mint, print
    const [batchUnitCount, setBatchUnitCount] = useState(20); // Default to 20 units per batch
    const [printBatch, setPrintBatch] = useState(null); // Tracks the batch currently selected for printing

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
            alert("Success: Batch successfully minted and registered on the ledger.");
        },
        onError: (error) => {
            // Extracts the error message from your ErrorResponseDto if available
            const errorMessage = error.response?.data?.message || "An error occurred while minting the batch.";
            alert(`Failed: ${errorMessage}`);
        }
    });

    // Helper: Register Asset to Blockchain
    const registerToBlockchain = useMutation({
        mutationFn: async (batchNumber) => (await apiClient.post(`batches/${batchNumber}/register`)).data,
        onSuccess: () => queryClient.invalidateQueries(['myInventory'])
    });

    // Helper: Download Batch QR Code
    const downloadQrCode = async (batchNumber) => {
        try {
            const response = await apiClient.get(`batches/${batchNumber}/qrcode`, {
                responseType: 'blob'
            });

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

    // Helper: Download Individual Unit QR Code
    const downloadUnitQrCode = async (serialNumber) => {
        try {
            const response = await apiClient.get(`units/${serialNumber}/qrcode`, {
                responseType: 'blob'
            });

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

    // Helper: Setup Print View
    const handlePrintLabels = (batch) => {
        setPrintBatch(batch);
        setActiveTab('print');
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
                        {activeTab === 'print' && "Print Unit Labels"}
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
                                        <td>
                                            <span className={`${styles.statusPill} ${batch.currentStatus === 'CONFIRMED' ? styles.statusPassed : styles.statusTransit}`}>
                                                {batch.currentStatus}
                                            </span>
                                        </td>
                                        <td style={{ display: 'flex', gap: '8px' }}>
                                            {batch.currentStatus === 'PENDING_BLOCKCHAIN' && (
                                                <button onClick={() => registerToBlockchain.mutate(batch.batchNumber)} className={styles.btnAction}>
                                                    Register on Chain
                                                </button>
                                            )}

                                            {(batch.currentStatus === 'CONFIRMED' || batch.currentStatus === 'PENDING_CONFIRMATION') && (
                                                <>
                                                    <button onClick={() => downloadQrCode(batch.batchNumber)} className={styles.btnAction} style={{ backgroundColor: '#2a9d8f', color: 'white', border: 'none' }}>
                                                        <span className="material-symbols-outlined" style={{ fontSize: '16px', marginRight: '4px' }}>qr_code_2</span>
                                                        Batch QR
                                                    </button>
                                                    <button onClick={() => handlePrintLabels(batch)} className={styles.btnAction} style={{ backgroundColor: '#e76f51', color: 'white', border: 'none' }}>
                                                        <span className="material-symbols-outlined" style={{ fontSize: '16px', marginRight: '4px' }}>print</span>
                                                        Unit Labels
                                                    </button>
                                                </>
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
                                manufacturerId: user.participantId,
                                batchUnitCount: parseInt(batchUnitCount, 10) // Included unit count in payload
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

                                {/* NEW INPUT: Batch Unit Count */}
                                <div className={styles.inputGroup}>
                                    <label>Units in Batch (Primary Packaging)</label>
                                    <input
                                        type="number"
                                        min="1"
                                        value={batchUnitCount}
                                        onChange={(e) => setBatchUnitCount(e.target.value)}
                                        required
                                        className={styles.input}
                                        placeholder="e.g., 20"
                                    />
                                </div>
                                <div className={styles.inputGroup}>
                                    <p style={{fontSize: '12px', color: 'var(--on-surface-variant)', marginTop: '24px'}}>
                                        * System will automatically generate deterministic serial numbers and individual QR hashes for each unit.
                                    </p>
                                </div>
                            </div>

                            <div className={styles.formActions}>
                                <button type="submit" className={styles.btnPrimary}>Register on Ledger</button>
                            </div>
                        </form>
                    </div>
                )}

                {/* VIEW 4: PRINT LABELS */}
                {activeTab === 'print' && printBatch && (
                    <div className={styles.formCard}>
                        {/* UPDATED HEADER: Now includes the Back Button */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>

                            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                {/* NEW: Back Button */}
                                <button
                                    onClick={() => setActiveTab('inventory')}
                                    className={styles.btnAction}
                                    style={{ background: '#e9ecef', color: '#333', border: 'none', padding: '8px 16px' }}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px', marginRight: '4px' }}>arrow_back</span>
                                    Back
                                </button>

                                <h2 style={{margin: 0}}>Batch: {printBatch.batchNumber}</h2>
                            </div>

                            <button onClick={() => window.print()} className={styles.btnPrimary} style={{ backgroundColor: '#e76f51' }}>
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
                                            src={`http://localhost:8080/api/v1/units/${serialNumber}/qrcode`}
                                            alt={`QR for ${serialNumber}`}
                                            style={{ width: '150px', height: '150px', marginBottom: '12px' }}
                                        />

                                        <p style={{ margin: '0 0 12px 0', fontSize: '12px', fontFamily: 'monospace', background: '#e9ecef', padding: '4px 8px', borderRadius: '4px' }}>
                                            {serialNumber}
                                        </p>

                                        {/* NEW: Download Button for individual unit */}
                                        <button
                                            onClick={() => downloadUnitQrCode(serialNumber)}
                                            className={styles.btnAction}
                                            style={{ width: '100%', justifyContent: 'center' }}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '16px', marginRight: '4px' }}>download</span>
                                            Download QR
                                        </button>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}