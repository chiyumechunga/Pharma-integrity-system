package com.chiyumechunga.backend.mytests;

import com.chiyumechunga.backend.controller.RegistryController;
import com.chiyumechunga.backend.dto.FireflyAckDto;
import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.service.RegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// The new import for Spring Boot 3.4+
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

// Static imports for cleaner test code
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistryController.class)
public class RegistryControllerSecurityTest { // <--- Added 'public' to help IDE detection


    private MockMvc mockMvc;

    // REPLACES @MockBean
    @MockitoBean
    private RegistryService registryService;

    private ObjectMapper objectMapper;

    @Test
    void shouldSanitizeXssAttackBeforeCallingService() throws Exception {
        // 1. ATTACK: Define malicious input
        String dangerousName = "<script>alert('Hacked')</script>";
        UUID mfgId = UUID.randomUUID();

        RegistryRequestDto attackRequest = new RegistryRequestDto(
                dangerousName,
                "BATCH-XSS",
                mfgId,
                LocalDate.now().plusYears(1),
                "validhash123"
        );

        // Mock the service response
        Mockito.when(registryService.registerBatch(Mockito.any()))
                .thenReturn(new FireflyAckDto("op-1", "SUBMITTED", "msg"));

        // 2. EXECUTE: Perform the POST request
        mockMvc.perform(post("/api/v1/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attackRequest)))
                .andExpect(status().isAccepted());

        // 3. CAPTURE & VERIFY
        ArgumentCaptor<RegistryRequestDto> captor = ArgumentCaptor.forClass(RegistryRequestDto.class);
        Mockito.verify(registryService).registerBatch(captor.capture());

        RegistryRequestDto capturedRequest = captor.getValue();

        // Assert that the script tags were escaped
        assertEquals("&lt;script&gt;alert('Hacked')&lt;/script&gt;", capturedRequest.productName());
    }
}