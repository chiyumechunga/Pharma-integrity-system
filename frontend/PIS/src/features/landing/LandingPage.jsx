import React from 'react';
import { Link } from 'react-router-dom';
import styles from './LandingPage.module.css';

export default function LandingPage() {
    return (
        <div className={styles.container}>

            {/* Top Header */}
            <header className={styles.header}>
                <div className={styles.brand}>
          <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>
            verified
          </span>
                    Pharmaceutical Integrity System
                </div>
                {/* Subtle link for supply chain staff to log in */}
                <Link to="/login" className={styles.loginBtn}>
                    Staff Portal
                </Link>
            </header>

            {/* Hero Section */}
            <main className={styles.hero}>
        <span className={`material-symbols-outlined ${styles.heroIcon}`} style={{ fontVariationSettings: "'FILL' 1" }}>
          health_and_safety
        </span>

                <h1 className={styles.title}>National Pharmaceutical Ledger System</h1>

                <p className={styles.description}>
                    Empowering patients and regulators with end-to-end cryptographic traceability.
                    Scan any registered medication to verify its authenticity and supply chain journey instantly.
                </p>

                <div className={styles.ctaWrapper}>
                    {/* The main button directing users to the scanner */}
                    <Link to="/verify" className={styles.scanBtn}>
                        <span className="material-symbols-outlined">qr_code_scanner</span>
                        Scan Medication QR
                    </Link>
                </div>
            </main>

            {/* Informational Cards */}
            <section className={styles.features}>
                <div className={styles.featureCard}>
                    <span className={`material-symbols-outlined ${styles.featureIcon}`}>link</span>
                    <h3>Immutable Ledger</h3>
                    <p>Every batch is recorded on a secure Hyperledger Fabric blockchain, ensuring data cannot be tampered with or counterfeited.</p>
                </div>
                <div className={styles.featureCard}>
                    <span className={`material-symbols-outlined ${styles.featureIcon}`}>policy</span>
                    <h3>ZAMRA Oversight</h3>
                    <p>Integrated directly with national regulatory authorities for immediate emergency recalls and lab quality assurance.</p>
                </div>
                <div className={styles.featureCard}>
                    <span className={`material-symbols-outlined ${styles.featureIcon}`}>verified_user</span>
                    <h3>Patient Safety</h3>
                    <p>Instantly confirm that the medication you are about to take is genuine, unexpired, and safe for consumption.</p>
                </div>
            </section>

        </div>
    );
}