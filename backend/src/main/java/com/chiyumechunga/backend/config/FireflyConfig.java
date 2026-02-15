package com.chiyumechunga.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FireflyConfig {

    @Value("${firefly.api.url}")
    private String fireflyUrl; //

    @Value("${firefly.namespace}")
    private String namespace; // e.g., default

    @Bean
    public WebClient fireflyWebClient(WebClient.Builder builder) {
        return builder
                // ERROR WAS HERE: Missing "/api/v1"
                // OLD: .baseUrl(fireflyUrl + "/namespaces/" + namespace)
                // NEW:
                .baseUrl(fireflyUrl + "/api/v1/namespaces/" + namespace)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}