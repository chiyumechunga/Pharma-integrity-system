package com.chiyumechunga.backend.integration;

import org.springframework.beans.factory.annotation.Autowired; // <--- This fixes the @Autowired error
import com.chiyumechunga.backend.config.FireflyNodeRouter;   // <--- This fixes the router error

import com.chiyumechunga.backend.model.ParticipantType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SpringBootTest
@Tag("integration") // Use this tag to separate from unit tests if needed
class FireflyConnectivityIT {

    @Autowired
     FireflyNodeRouter nodeRouter;

    /**
     * Test connectivity to the Manufacturer Node (Port 7000).
     * This relies on the 'pis' stack being running via 'ff start pis'.
     */
    @Test
    @DisplayName("Integration: Should connect to Manufacturer Firefly Node")
    void shouldConnectToManufacturerNode() {
        // 1. Get the client for Manufacturer (Configured for http://localhost:7000)
        WebClient client = nodeRouter.getClientForRole(ParticipantType.MANUFACTURER);

        // 2. Attempt to hit a basic Firefly endpoint (e.g., /status or /network/identities)
        // We use /status which is a standard Firefly endpoint to check health
        String response;
        try {
            response = client.get()
                    .uri("/status") // Maps to /api/v1/namespaces/default/status
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 3. Verify we got a JSON response implies the node is reachable
            Assertions.assertNotNull(response);
            Assertions.assertTrue(response.contains("org"), "Response should contain organization info");

            System.out.println("✅ Successfully connected to Manufacturer Node: " + response);

        } catch (Exception e) {
            Assertions.fail("Could not connect to Manufacturer Node at localhost:7000. Is the Firefly stack running? Error: " + e.getMessage());
        }
    }

    /**
     * Test connectivity to the ZAMMSA Node (Port 7001).
     */
    @Test
    @DisplayName("Integration: Should connect to ZAMMSA Firefly Node")
    void shouldConnectToZammsaNode() {
        WebClient client = nodeRouter.getClientForRole(ParticipantType.ZAMMSA);

        try {
            String response = client.get()
                    .uri("/status")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Assertions.assertNotNull(response);
            System.out.println("✅ Successfully connected to ZAMMSA Node");

        } catch (Exception e) {
            Assertions.fail("Could not connect to ZAMMSA Node at localhost:7001. Error: " + e.getMessage());
        }
    }

    /**
     * Test Contract Routing Logic (without needing the contract deployed).
     * Calling invoke on a missing contract should return a 404 or 500 from Firefly,
     * NOT a "Connection Refused".
     */
    @Test
    @DisplayName("Integration: Verify Router calls correct ports")
    void verifyRouterPortsReachable() {
        // This test attempts to hit the contract invoke endpoint.
        // Even if the contract isn't deployed, receiving a 404/500 proves we reached Firefly.
        // If we receive "Connection refused", the test fails.

        WebClient client = nodeRouter.getClientForRole(ParticipantType.PHARMACY); // Port 7002

        Assertions.assertThrows(WebClientResponseException.class, () -> client.post()
                .uri("/contracts/invoke")
                .bodyValue("{}") // Invalid body
                .retrieve()
                .bodyToMono(String.class)
                .block(), "Should receive an HTTP error from Firefly (proving connectivity), not a connection exception.");
    }
}