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
     * UNIVERSAL EVENT RECEIVER:
     * Handles ALL Blockchain events:
     * 1. CreateAsset (Registry)
     * 2. TransferCustody (Supply Chain)
     * 3. SubmitTestResult (Regulatory)
     *
     * Firefly Subscription should point to: POST /api/v1/webhooks/firefly
     */
    @PostMapping // <--- CHANGED: Removed "/assets" to make it the default handler for this path
    public ResponseEntity<Void> handleBlockchainEvent(@RequestBody FireflyEventDto event) {
        log.info("🔔 Webhook Triggered | Type: {} | ID: {}", event.type(), event.id());

        // 1. FILTERING: Only process explicit blockchain events
        if (!EVENT_TYPE_BLOCKCHAIN.equals(event.type())) {
            log.debug("Skipping non-blockchain event (Ping/System): {}", event.type());
            return ResponseEntity.ok().build();
        }

        // 2. NULL SAFETY: Validation check
        if (event.output() == null || event.output().data() == null) {
            log.warn("⚠️ Ignored Empty Payload Event ID: {}", event.id());
            return ResponseEntity.ok().build();
        }

        // 3. IDENTIFY SUB-TYPE (For Debugging)
        // Firefly sends the "data" map. We can peek at it to see what kind of event it is.
        // (The Service handles the actual logic, but logging it here helps you debug)
        String qrHash = event.output().data().qrHash();
        log.info("Processing Event for Product QR: {}", qrHash);

        // 4. PROCESSING WITH RETRY LOGIC (Synchronous)
        try {
            // This service method is the "Switchboard" that decides:
            // "Is this a new asset? Save to Registry."
            // "Is this a transfer? Update ChainOfCustody."
            eventProcessingService.processBlockchainEvent(event);

            log.info("✅ Event Processed Successfully: {}", event.id());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ CRITICAL: Failed to process Firefly event {}. Triggering RETRY.", event.id(), e);

            // 5. RELIABILITY: Return 500 Internal Server Error
            // Firefly will detect this 500 and automatically retry sending the event
            // according to its "Reliable Delivery" policy (usually exponential backoff).
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}