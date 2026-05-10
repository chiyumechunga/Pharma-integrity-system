// src/components/Footer.jsx
import React from 'react';

export default function Footer() {
    return (
        <footer style={{
            textAlign: 'center',
            padding: '24px 16px',
            backgroundColor: 'var(--surface)',
            color: 'var(--on-surface-variant)',
            fontFamily: 'var(--font-ui)',
            fontSize: '13px',
            borderTop: '1px solid var(--border)',
            marginTop: 'auto',
            width: '100%',
            boxSizing: 'border-box'
        }}>
            <p style={{ margin: 0, fontWeight: '500' }}>
                &copy; {new Date().getFullYear()} Blockchain-Based Pharmaceutical Integrity System. All rights reserved.
            </p>
            <p style={{ margin: '6px 0 0 0', fontSize: '12px', opacity: 0.8 }}>
                Securing the Zambian Pharmaceutical Supply Chain
            </p>
        </footer>
    );
}