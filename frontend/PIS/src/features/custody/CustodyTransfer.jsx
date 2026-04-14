import React from 'react';
import styles from './CustodyTransfer.module.css';
import { apiClient } from "../../services/apiClient.js";
import { useMutation } from "@tanstack/react-query";
import { useAuth } from '../auth/AuthContext'; // 1. Import Auth Context

export default function CustodyTransfer() {
    // 2. Extract user and logout function
    const { user, logout } = useAuth();

    const offlineScans = 3;

    const transferMutation = useMutation({
        mutationFn: async (transferData) => {
            // Adjusted to hit the root POST endpoint of CustodyController
            const response = await apiClient.post('/custody', transferData);
            return response.data;
        },
        onSuccess: () => {
            alert("Transfer recorded successfully!");
        },
        onError: (error) => {
            alert("Transfer failed: " + (error.response?.data?.message || error.message));
        }
    });

    // Dummy function to test the mutation without the camera scanner
    const handleDummySend = () => {
        transferMutation.mutate({
            batchNumber: "BAT-2026-XYZ",
            // Dynamically use the logged-in user's participant ID if available
            fromParticipantId: user?.participantId || "ZAMMSA-HQ",
            toParticipantId: "PHARMACY-001"
        });
    };

    return (
        <div className={styles.container}>
            {/* Header & Sticky Top */}
            <header className={styles.header}>
                <div>
                    <h1 className={styles.title}>Handover</h1>
                    {/* Dynamically show the logged-in user/facility name */}
                    <p className={styles.subtitle}>{user?.username || 'Lusaka Central Depot'}</p>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    {offlineScans > 0 && (
                        <div className={styles.syncBadge}>
                            <span className={`material-symbols-outlined ${styles.syncIcon}`}>
                              cloud_off
                            </span>
                            <span className={styles.syncText}>{offlineScans} Queued</span>
                        </div>
                    )}
                    {/* 3. The Secure Logout Button */}
                    <button onClick={logout} className={styles.logoutBtn} title="Secure Logout">
                        <span className="material-symbols-outlined">logout</span>
                    </button>
                </div>
            </header>

            {/* Main Actions */}
            <section className={styles.actionSection}>
                {/* Added onClick={handleDummySend} to test the API call */}
                <div className={`${styles.actionCard} ${styles.actionCardPrimary}`} onClick={handleDummySend}>
                    <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                        <div className={`${styles.actionIconWrapper} ${styles.bgPrimaryLight}`}>
                          <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>
                            qr_code_scanner
                          </span>
                        </div>
                        <div className={styles.actionText}>
                            <h3>Scan to Send</h3>
                            <p>Transfer custody to driver</p>
                        </div>
                    </div>
                    <span className={`material-symbols-outlined ${styles.chevron}`}>chevron_right</span>
                </div>

                <div className={styles.actionCard}>
                    <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                        <div className={`${styles.actionIconWrapper} ${styles.bgSurfaceVariant}`}>
                            <span className="material-symbols-outlined">call_received</span>
                        </div>
                        <div className={styles.actionText}>
                            <h3>Scan to Receive</h3>
                            <p>Accept delivery at facility</p>
                        </div>
                    </div>
                    <span className={`material-symbols-outlined ${styles.chevron}`}>chevron_right</span>
                </div>
            </section>

            {/* Loading Indicator */}
            {transferMutation.isPending && (
                <p style={{ textAlign: 'center', fontFamily: 'var(--font-ui)', color: 'var(--primary)', marginTop: '16px' }}>
                    Recording on Ledger...
                </p>
            )}

            {/* Recent Activity List */}
            <section className={styles.listSection}>
                <h2 className={styles.listHeader}>Recent Transfers</h2>

                <div className={styles.list}>
                    {/* Active List Item */}
                    <div className={styles.listItem}>
                        <div className={`${styles.statusIndicator} ${styles.statusActive}`}></div>
                        <div className={styles.itemContent}>
                            <div className={styles.itemHeader}>
                                <span className={styles.batchNumber}>BAT-8924-XYZ</span>
                                <span className={styles.time}>Just now</span>
                            </div>
                            <p className={styles.itemDetails}>
                                <span className={styles.statusText}>In Transit</span> • Driver ID: 8842
                            </p>
                        </div>
                    </div>

                    {/* Past List Item */}
                    <div className={styles.listItem}>
                        <div className={styles.statusIndicator}></div>
                        <div className={styles.itemContent}>
                            <div className={styles.itemHeader}>
                                <span className={styles.batchNumber}>BAT-7102-ABC</span>
                                <span className={styles.time}>14:30</span>
                            </div>
                            <p className={styles.itemDetails}>
                                Received • Pharmacy 44
                            </p>
                        </div>
                    </div>
                </div>
            </section>
        </div>
    );
}