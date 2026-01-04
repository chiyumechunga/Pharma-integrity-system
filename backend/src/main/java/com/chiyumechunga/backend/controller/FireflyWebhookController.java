package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.firefly.FireflyEventDto;
import com.chiyumechunga.backend.service.EventProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/firefly")
public class FireflyWebhookController {

    private final EventProcessingService eventProcessingService;

    // CONSTANT: The specific event type your Chaincode emits
    private static final String EVENT_TYPE_BLOCKCHAIN = "blockchain_event_received";

    public FireflyWebhookController(EventProcessingService eventProcessingService) {
        this.eventProcessingService = eventProcessingService;
    }

    /**
     * EVENT DRIVEN UPDATE:
     * Firefly → Webhook → Postgres
     * * Logic applied:
     * 1. Filtering: Ignore system events (pings, blocks) that aren't asset creations.
     * 2. Null Safety: Prevent NullPointerExceptions on empty payloads.
     * 3. Reliability: Return 500 on failure to trigger Firefly's "Reliable Delivery" (Retry).
     */
    @PostMapping("/assets")
    public ResponseEntity<Void> handleAssetEvent(@RequestBody FireflyEventDto event) {
        log.info("Received Event from Firefly: Type={}, ID={}", event.type(), event.id());

        // 1. FILTERING: Only process explicit blockchain events
        // If Firefly sends a "message_confirmed" or internal event, we ignore it safely.
        if (!EVENT_TYPE_BLOCKCHAIN.equals(event.type())) {
            log.debug("Skipping irrelevant event type: {}", event.type());
            return ResponseEntity.ok().build(); // Return 200 so Firefly considers it "delivered"
        }

        // 2. NULL SAFETY: Validation check
        if (event.output() == null || event.output().data() == null) {
            log.warn("Received blockchain event with empty data payload. ID: {}", event.id());
            return ResponseEntity.ok().build(); // Return 200 to discard bad data
        }

        // 3. PROCESSING WITH RETRY LOGIC
        try {
            // We run this SYNCHRONOUSLY.
            // Why? If we used @Async, we would return 200 OK immediately.
            // If the DB write failed 1ms later, the event would be lost forever.
            // By blocking here, if the DB fails, we catch the error and return 500.
            eventProcessingService.processBlockchainEvent(event);

            log.info("Successfully processed event ID: {}", event.id());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process Firefly event. Triggering RETRY mechanism.", e);

            // 4. RELIABILITY: Return 500 Internal Server Error
            // This tells Firefly: "I failed to save this. Please send it again later."
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}