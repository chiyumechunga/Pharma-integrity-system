import apiClient from './apiClient';

export const verificationService = {
    // Called after mobile camera decodes QR — public endpoint, no JWT needed
    verifyProduct: (qrHash) =>
        apiClient.post('verifications', { qrHash }).then(r => r.data),

    getProvenance: (qrHash) =>
        apiClient.get(`verifications/${qrHash}`).then(r => r.data),
};