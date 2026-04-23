import apiClient from './apiClient';

export const regulatoryService = {
    getAllBatches: () =>
        apiClient.get('regulatory/batches').then(r => r.data),

    getAllParticipants: () =>
        apiClient.get('regulatory/participants').then(r => r.data),

    getVerificationLogs: () =>
        apiClient.get('regulatory/verifications').then(r => r.data),
};