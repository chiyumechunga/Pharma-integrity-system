// src/features/pharmacy/ScannerResult.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './PharmacyDashboard.module.css';

export default function ScannerResult({ result, scannedHash }) {
    const navigate = useNavigate();

    // 1. Evaluate specific business logic states
    const isDispensed = result.status === 'DISPENSED';
    const isRecalled = result.status === 'RECALLED' || result.message?.includes('Recalled');
    const isExpired = result.message?.includes('Expired');
    const isAuthenticAndAvailable = result.isValid && !isDispensed && !isRecalled && !isExpired;

    // 2. Determine UI theme based on safety, not just authenticity
    let themeClass = styles.themeSuccess;
    let alertBanner = null;

    if (isRecalled || isExpired || !result.isValid) {
        themeClass = styles.themeDanger;
        alertBanner = "CRITICAL: Do not dispense. Product is unsafe or invalid.";
    } else if (isDispensed) {
        themeClass = styles.themeWarning;
        alertBanner = "WARNING: This item is already recorded as dispensed. Do not hand to patient. Potential duplicate scan.";
    }

    return (
        <div className={`${styles.resultCard} ${themeClass}`}>

            {/* Header Status */}
            <div className={styles.statusHeader}>
                <span className="material-symbols-outlined icon">
                    {isAuthenticAndAvailable ? 'check_circle' : 'warning'}
                </span>
                <h2>{result.productName || 'Unknown Product'}</h2>
            </div>

            <p className={styles.systemMessage}>{result.message}</p>

            {/* Contextual Alert Banner */}
            {alertBanner && (
                <div className={styles.alertBanner}>
                    <span className="material-symbols-outlined">error</span>
                    {alertBanner}
                </div>
            )}

            {/* Action Routing Panel */}
            <div className={styles.actionGrid}>

                {/* Dispense Action - ONLY if safe */}
                {isAuthenticAndAvailable ? (
                    <button
                        className={`${styles.actionBtn} ${styles.btnPrimary}`}
                        onClick={() => navigate(`/pharmacy/dispense/${scannedHash}`, { state: { product: result } })}
                    >
                        <span className="material-symbols-outlined">prescriptions</span>
                        Proceed to Dispense
                    </button>
                ) : (
                    <button className={`${styles.actionBtn} ${styles.btnDisabled}`} disabled>
                        <span className="material-symbols-outlined">block</span>
                        Dispense Locked
                    </button>
                )}

                {/* Audit Action - ALWAYS available */}
                <button
                    className={`${styles.actionBtn} ${styles.btnSecondary}`}
                    onClick={() => navigate(`/audit/${scannedHash}`)}
                >
                    <span className="material-symbols-outlined">account_tree</span>
                    Provenance Audit
                </button>


            </div>
        </div>
    );
}