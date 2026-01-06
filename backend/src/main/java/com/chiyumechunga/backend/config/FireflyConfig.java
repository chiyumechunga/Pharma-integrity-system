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
    private String fireflyUrl;

    @Value("${firefly.namespace}")
    private String namespace;

    @Bean
    public WebClient fireflyWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(fireflyUrl + "/namespaces/" + namespace)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}