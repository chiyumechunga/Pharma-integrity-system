package com.chiyumechunga.backend.config;

import com.chiyumechunga.backend.model.ParticipantType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class FireflyNodeRouter {

    @Value("${firefly.namespace}")
    private String namespace;

    // Cache clients so we don't rebuild them every request
    private final Map<ParticipantType, WebClient> nodeClients = new ConcurrentHashMap<>();

    /**
     * Returns the connection to the SPECIFIC Firefly Node for this user role.
     */
    public WebClient getClientForRole(ParticipantType role) {
        return nodeClients.computeIfAbsent(role, this::buildClient);
    }

    private WebClient buildClient(ParticipantType role) {
        String baseUrl;

        // MAP ROLES TO DOCKER PORTS
        switch (role) {
            case MANUFACTURER:
                baseUrl = "http://localhost:7000"; // Node 0
                break;
            case ZAMMSA: // Distributor
                baseUrl = "http://localhost:7001"; // Node 1
                break;
            case PHARMACY:
                baseUrl = "http://localhost:7002"; // Node 2
                break;
            case ZAMRA: // Regulator
                baseUrl = "http://localhost:7003"; // Node 3
                break;
            default:
                throw new IllegalArgumentException("No Firefly Node configured for role: " + role);
        }

        return WebClient.builder()
                .baseUrl(baseUrl + "/api/v1/namespaces/" + namespace)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}