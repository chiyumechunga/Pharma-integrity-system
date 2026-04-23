import apiClient from './apiClient';

export const pharmacyService = {
    getIncomingTransfers: () =>
        apiClient.get('pharmacy/transfers').then(r => r.data),

    confirmReceipt: (transferId) =>
        apiClient.put(`pharmacy/transfers/${transferId}/confirm`).then(r => r.data),
};