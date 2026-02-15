package com.chiyumechunga.backend.controller;

import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.service.RegistryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/api/v1/registry")
public class RegistryController {

    private final RegistryService registryService;

    public RegistryController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping
    public ResponseEntity<FireflyAckDto> registerBatch(@Valid @RequestBody RegistryRequestDto request) {
        // 1. INPUT SANITIZATION (Keep this for Defense in Depth)
        // UPDATED: No qrHash field (backend generates it), added manufacturingDate
        RegistryRequestDto safeRequest = new RegistryRequestDto(
                HtmlUtils.htmlEscape(request.productName()),
                sanitizeStrict(request.batchNumber()),
                request.manufacturerId(),
                request.manufacturingDate(), // ADDED: Pass through manufacturing date
                request.expiryDate()
                // REMOVED: qrHash - backend generates this
        );

        // 2. EXECUTE LOGIC
        FireflyAckDto serviceResponse = registryService.registerBatch(safeRequest);

        // 3. BREAK THE TAINT CHAIN (Security Fix)
        // Return only system-generated data, not user input
        FireflyAckDto cleanResponse = new FireflyAckDto(
                serviceResponse.operationId(), // SAFE: System-generated UUID from Firefly
                serviceResponse.status(),      // SAFE: System-controlled status
                "Batch registration initiated. QR hash will be generated after blockchain confirmation." // SAFE: Hardcoded
        );

        // 4. SECURITY HEADERS
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(cleanResponse, headers, HttpStatus.ACCEPTED);
    }

    private String sanitizeStrict(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9-_]", "");
    }
}