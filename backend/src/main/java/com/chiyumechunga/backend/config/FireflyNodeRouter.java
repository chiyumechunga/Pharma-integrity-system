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

    // INJECTED: The single Supernode URL (http://127.0.0.1:5000)
    // defined in application.properties as firefly.api.url
    @Value("${firefly.api.url}")
    private String fireflyApiUrl;

    @Value("${firefly.api.name}")       // ← add this
    private String apiName;

    // Cache clients to maintain performance
    private final Map<ParticipantType, WebClient> nodeClients = new ConcurrentHashMap<>();

    /**
     * Returns the connection to the Firefly Supernode.
     * * NOTE: We preserve the 'ParticipantType' argument to keep your Service layer
     * compatible, but inside here, we route EVERYONE to the same
     * single-node gateway (Port 5000).
     */
    public WebClient getClientForRole(ParticipantType role) {
        return nodeClients.computeIfAbsent(role, k -> buildClient());
    }

    private WebClient buildClient() {
        String fullUrl = fireflyApiUrl
                + "/api/v1/namespaces/" + namespace
                + "/apis/" + apiName;

        return WebClient.builder()
                .baseUrl(fullUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}