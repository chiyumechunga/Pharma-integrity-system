import React, { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';
import { useAuth } from '../auth/AuthContext';
import styles from './ManufacturerRegistry.module.css';
import { useNavigate } from 'react-router-dom';

export default function ManufacturerRegistry() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        productId: '',
        batchNumber: '',
        manufacturerId: user?.participantId || 'MFG-ZAM-001',
        manufacturingDate: '',
        expiryDate: ''
    });

    // 1. Fetch Manufacturer's Inventory History
    const { data: inventory, isLoading: isLoadingInventory } = useQuery({
        queryKey: ['myInventory'],
        queryFn: async () => {
            try {
                const response = await apiClient.get('/batches');
                return response.data;
            } catch (error) {
                console.warn("Failed to fetch inventory:", error);
                return [];
            }
        },
        refetchInterval: 30000
    });

    // 2. Mint New Batch
    const registerMutation = useMutation({
        mutationFn: async (data) => {
            const response = await apiClient.post('/batches', data);
            return response.data;
        }
    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        registerMutation.mutate(formData);
    };

    const resetForm = () => {
        setFormData({ ...formData, productId: '', batchNumber: '', manufacturingDate: '', expiryDate: '' });
        registerMutation.reset();
    };

    const qrCodeUrl = registerMutation.isSuccess
        ? `${apiClient.defaults.baseURL}/batches/${formData.batchNumber}/qrcode`
        : null;

    // Helper to color-code supply chain statuses
    const getStatusClass = (status) => {
        switch (status) {
            case 'PASSED': return styles.statusPassed;
            case 'IN_TRANSIT': return styles.statusTransit;
            case 'DISPENSED': return styles.statusDispensed;
            case 'RECALLED':
            case 'FAILED':
            case 'EXPIRED': return styles.statusDanger;
            default: return styles.statusTransit;
        }
    };

    return (
        <div className={styles.container}>
            {/* SINGLE UNIFIED HEADER INSIDE THE CONTAINER */}
            <header className={styles.header} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <h1 className={styles.title}>Manufacturer Portal</h1>
                    <p className={styles.subtitle}>{user?.username || 'PharmaCorp Zambia'} • Digital Asset Minting</p>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <button
                        onClick={() => navigate('/handover')}
                        className={styles.btnPrimary}
                        style={{ padding: '10px 20px', borderRadius: '8px', fontSize: '13px' }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>local_shipping</span>
                        Transfer Inventory
                    </button>

                    <button onClick={logout} className={styles.logoutBtn} title="Secure Logout">
                        <span className="material-symbols-outlined">logout</span>
                    </button>
                </div>
            </header>

            {/* MINTING FORM */}
            <div className={styles.formCard}>
                {!registerMutation.isSuccess ? (
                    <form onSubmit={handleSubmit}>
                        <div className={styles.formGrid}>
                            <div className={`${styles.inputGroup} ${styles.fullWidth}`}>
                                <label className={styles.label}>Registered Product</label>
                                <select className={styles.select} name="productId" value={formData.productId} onChange={handleChange} required>
                                    <option value="" disabled>Select medication...</option>
                                    <option value="fb992857-e6f9-467a-9a99-b1d64391e6b8">Amoxicillin 500mg Capsules</option>
                                    <option value="a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d">Paracetamol 500mg Tablets</option>
                                </select>
                            </div>

                            <div className={`${styles.inputGroup} ${styles.fullWidth}`}>
                                <label className={styles.label}>Manufacturer Batch Number</label>
                                <input type="text" className={styles.input} name="batchNumber" value={formData.batchNumber} onChange={handleChange} placeholder="e.g. BAT-2026-XYZ" required />
                            </div>

                            <div className={styles.inputGroup}>
                                <label className={styles.label}>Mfg. Date</label>
                                <input type="date" className={styles.input} name="manufacturingDate" value={formData.manufacturingDate} onChange={handleChange} required />
                            </div>

                            <div className={styles.inputGroup}>
                                <label className={styles.label}>Expiry Date</label>
                                <input type="date" className={styles.input} name="expiryDate" value={formData.expiryDate} onChange={handleChange} required />
                            </div>
                        </div>

                        {registerMutation.isError && (
                            <p style={{ color: 'red', marginTop: '16px', fontSize: '14px', fontFamily: 'var(--font-ui)' }}>
                                Error: {registerMutation.error.response?.data?.message || 'Failed to register batch'}
                            </p>
                        )}

                        <div className={styles.formActions}>
                            <button type="submit" className={styles.btnPrimary} disabled={registerMutation.isPending}>
                                {registerMutation.isPending ? 'Minting Asset...' : 'Register on Ledger'}
                                {!registerMutation.isPending && <span className="material-symbols-outlined">add_link</span>}
                            </button>
                        </div>
                    </form>
                ) : (
                    <div className={styles.successState}>
                        <div className={styles.badgeContainer}>
                            <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>verified</span>
                            Confirmed on Chain
                        </div>

                        <h2 className={styles.title} style={{ fontSize: '22px' }}>Batch {formData.batchNumber}</h2>

                        <div className={styles.qrBox}>
                            <div className={styles.qrPlaceholder} style={{ padding: '8px' }}>
                                <img src={qrCodeUrl} alt="Batch QR Code" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                            </div>
                            <p className={styles.qrInstructions}>
                                Apply this cryptographic QR to the physical packaging before handover to logistics.
                            </p>
                        </div>

                        <button onClick={resetForm} className={styles.btnPrimary} style={{ margin: '0 auto' }}>
                            Register Another Batch
                        </button>
                    </div>
                )}
            </div>

            {/* INVENTORY MONITORING TABLE */}
            <section className={styles.inventorySection}>
                <h2 style={{ fontSize: '20px', color: 'var(--primary)', marginBottom: '8px' }}>My Production Ledger</h2>
                <p style={{ fontFamily: 'var(--font-ui)', color: 'var(--on-surface-variant)', fontSize: '14px' }}>
                    Live supply chain status of all batches minted by your facility.
                </p>

                <div className={styles.tableContainer}>
                    <table className={styles.inventoryTable}>
                        <thead>
                        <tr>
                            <th>Batch Number</th>
                            <th>Product ID</th>
                            <th>Mfg Date</th>
                            <th>Expiry Date</th>
                            <th>Current Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        {isLoadingInventory ? (
                            <tr>
                                <td colSpan="5" style={{ textAlign: 'center', padding: '32px' }}>Loading ledger data...</td>
                            </tr>
                        ) : inventory && inventory.length > 0 ? (
                            inventory.map((batch) => (
                                <tr key={batch.batchNumber}>
                                    <td style={{ fontWeight: '600' }}>{batch.batchNumber}</td>
                                    <td>{batch.productId || 'Unknown'}</td>
                                    <td>{batch.manufacturingDate}</td>
                                    <td>{batch.expiryDate}</td>
                                    <td>
                                            <span className={`${styles.statusPill} ${getStatusClass(batch.status)}`}>
                                                {batch.status || 'PASSED'}
                                            </span>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="5" style={{ textAlign: 'center', padding: '32px', color: 'var(--on-surface-variant)' }}>
                                    No batches registered yet.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}