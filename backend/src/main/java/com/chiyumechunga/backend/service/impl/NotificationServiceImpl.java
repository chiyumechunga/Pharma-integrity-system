package com.chiyumechunga.backend.service.impl;

import com.chiyumechunga.backend.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendAdminAlert(String message, String urgency) {
        // In a real app, this sends an Email (JavaMailSender) or SMS (Twilio)
        // For now, we simulate a critical log
        log.error("🚨 ALERT [{}]: {}", urgency, message);
    }
}