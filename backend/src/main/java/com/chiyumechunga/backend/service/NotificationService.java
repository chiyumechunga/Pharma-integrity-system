package com.chiyumechunga.backend.service;

public interface NotificationService {
    void sendAdminAlert(String message, String urgency);
}