package com.chiyumechunga.backend.service;

import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.FailedEvent;
import com.chiyumechunga.backend.repository.FailedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final FailedEventRepository failedEventRepo;
    private final EventProcessingService eventService;
    private final ObjectMapper objectMapper;

    // Runs every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void retryFailedEvents() {
        // Fetch events that failed fewer than 3 times
        List<FailedEvent> retries = failedEventRepo.findByRetryCountLessThan(3);

        if (retries.isEmpty()) return;

        log.info("Found {} failed blockchain events. Attempting retry...", retries.size());

        for (FailedEvent failedEvent : retries) {
            try {
                // 1. Deserialize the payload back into the DTO
                FireflyEventDto eventDto = objectMapper.readValue(failedEvent.getRawPayload(), FireflyEventDto.class);

                // 2. Try processing it again
                eventService.processBlockchainEvent(eventDto);

                // 3. If successful, delete it from the DLQ
                failedEventRepo.delete(failedEvent);
                log.info("Successfully recovered failed event: {}", failedEvent.getTxId());

            } catch (Exception e) {
                // 4. If it fails again, increment the retry count
                log.error("Retry failed for event: {}. Reason: {}", failedEvent.getTxId(), e.getMessage());
                failedEvent.setRetryCount(failedEvent.getRetryCount() + 1);
                failedEvent.setErrorMessage("Retry failed: " + e.getMessage());
                failedEventRepo.save(failedEvent);
            }
        }
    }
}