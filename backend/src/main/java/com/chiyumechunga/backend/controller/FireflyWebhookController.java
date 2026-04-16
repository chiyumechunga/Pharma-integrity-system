package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.model.FailedEvent;
import com.chiyumechunga.backend.repository.FailedEventRepository;
import com.chiyumechunga.backend.service.EventProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/firefly")
@RequiredArgsConstructor
public class FireflyWebhookController {

    private final EventProcessingService eventService;
    private final FailedEventRepository failedEventRepo; // <--- Injected for the fix
    private final ObjectMapper objectMapper;             // <--- Injected to serialize payload

    /**
     * Entry point for all Blockchain Events (Webhooks from Firefly).
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveBlockchainEvent(@RequestBody FireflyEventDto event) {
        log.info(" Webhook Received: Event ID {}", event.id());

        try {
            // 1. Attempt to process the event immediately
            eventService.processBlockchainEvent(event);

            log.info(" Event processed successfully.");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error(" Error processing event {}. Saving to Dead Letter Queue.", event.id(), e);

            // 2. THE FIX: Persist failure to DB instead of throwing 500
            saveToDeadLetterQueue(event, e.getMessage());

            // 3. Return 200 OK to Firefly
            // This stops Firefly from retrying endlessly.
            // Our internal Scheduler will now handle the retries calmly every 5 mins.
            return ResponseEntity.ok().build();
        }
    }

    private void saveToDeadLetterQueue(FireflyEventDto event, String errorMessage) {
        try {
            FailedEvent failedEvent = new FailedEvent();

            // Extract Transaction ID safely (handle nulls if tx is missing)
            String txId = (event.transaction() != null) ? event.transaction().id() : "UNKNOWN_TX";
            failedEvent.setTxId(txId);

            // Serialize the entire event object back to JSON so we can retry it exactly later
            failedEvent.setRawPayload(objectMapper.writeValueAsString(event));

            failedEvent.setErrorMessage(errorMessage);
            failedEvent.setRetryCount(0);

            failedEventRepo.save(failedEvent);
            log.info(" Saved Event {} to FailedEventRepository.", event.id());

        } catch (JsonProcessingException jsonEx) {
            log.error(" Critical Failure: Could not serialize event to JSON for saving.", jsonEx);
            // In this rare case, we might want to return 500 to keep Firefly trying,
            // or just log it if we can't save it at all.
        }
    }
}