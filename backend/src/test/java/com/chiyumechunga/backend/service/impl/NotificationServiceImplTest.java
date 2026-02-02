package com.chiyumechunga.backend.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void shouldSendAdminAlertWithoutException() {
        // EXECUTE & VERIFY
        // Since the current implementation only logs, we verify that it runs successfully
        // without throwing exceptions when fed valid strings.
        Assertions.assertDoesNotThrow(() -> {
            notificationService.sendAdminAlert("Counterfeit Detected", "HIGH");
        });
    }
}