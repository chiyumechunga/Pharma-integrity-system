import apiClient from './apiClient';

const getGeoLocation = () =>
    new Promise((resolve) => {
        if (!navigator.geolocation) return resolve('Unknown');
        navigator.geolocation.getCurrentPosition(
            (pos) => resolve(`${pos.coords.latitude},${pos.coords.longitude}`),
            () => resolve('Denied'),
            { timeout: 5000 }
        );
    });

export const verificationService = {
    verifyProduct: async (qrHash) => {
        const geoLocation = await getGeoLocation();
        const response = await apiClient.post('verifications', {
            qrHash,
            deviceFingerprint: navigator.userAgent,
            geoLocation,
        });
        return response.data;
    },

    getProvenance: (qrHash) =>
        apiClient.get(`verifications/${qrHash}`).then(r => r.data),
};