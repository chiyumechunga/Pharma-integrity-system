package com.chiyumechunga.backend.scheduler;

import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.FailedEvent;
import com.chiyumechunga.backend.repository.FailedEventRepository;
import com.chiyumechunga.backend.service.EventProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainRetryScheduler {

    private final FailedEventRepository failedEventRepo;
    private final EventProcessingService eventProcessingService;
    private final ObjectMapper objectMapper;

    // Run every 5 minutes
    @Scheduled(fixedDelay = 300000)
    public void retryFailedEvents() {
        // 1. Find events that haven't been retried too many times (Max 5)
        List<FailedEvent> failures = failedEventRepo.findAll().stream()
                .filter(f -> f.getRetryCount() < 5)
                .toList();

        if (failures.isEmpty()) return;

        log.info("Found {} failed blockchain events. Attempting retry...", failures.size());

        for (FailedEvent failure : failures) {
            try {
                // 2. Deserialize the original JSON payload
                FireflyEventDto originalEvent = objectMapper.readValue(failure.getRawPayload(), FireflyEventDto.class);

                // 3. Retry Processing
                eventProcessingService.processBlockchainEvent(originalEvent);

                // 4. If successful, delete the failure record
                failedEventRepo.delete(failure);
                log.info("✅ Successfully recovered event {}", failure.getTxId());

            } catch (Exception e) {
                // 5. If it fails again, increment counter
                failure.setRetryCount(failure.getRetryCount() + 1);
                failedEventRepo.save(failure);
                log.error("Retry failed for event {}. Attempt {}/5", failure.getTxId(), failure.getRetryCount());
            }
        }
    }
}