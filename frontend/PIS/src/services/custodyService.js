import apiClient from './apiClient';

export const custodyService = {
    transferCustody: (payload) =>
        apiClient.post('custody', payload).then(r => r.data),

    dispenseProduct: (qrHash) =>
        apiClient.post('custody/dispense', { qrHash }).then(r => r.data),

    getMyCustodyHistory: () =>
        apiClient.get('custody/history').then(r => r.data),
};