import apiClient from './apiClient';

export const registryService = {
    registerBatch: (payload) =>
        apiClient.post('registry', payload).then(r => r.data),

    getMyBatches: () =>
        apiClient.get('registry').then(r => r.data),

    // Returns an object URL string ready for <img src={...} />
    getQrCode: (registryId) =>
        apiClient.get(`registry/qr/${registryId}`) // blob responseType set by interceptor
            .then(r => URL.createObjectURL(r.data)),
};