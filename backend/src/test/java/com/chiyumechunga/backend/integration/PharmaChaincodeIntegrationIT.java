package com.chiyumechunga.backend.integration;

import com.chiyumechunga.backend.dto.RegistryRequestDto;
import com.chiyumechunga.backend.dto.TransferRequestDto;
import com.chiyumechunga.backend.model.ParticipantType;
import com.chiyumechunga.backend.model.PharmaceuticalRegistry;
import com.chiyumechunga.backend.model.SupplyChainParticipant;
import com.chiyumechunga.backend.repository.ChainOfCustodyRepository;
import com.chiyumechunga.backend.repository.PharmaceuticalRegistryRepository;
import com.chiyumechunga.backend.repository.SupplyChainParticipantRepository;
import com.chiyumechunga.backend.service.CustodyService;
import com.chiyumechunga.backend.service.RegistryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * End-to-End Integration Test for FireFly Chaincode Invocation and Webhook Processing.
 * * REQUIRED INFRASTRUCTURE:
 * 1. Hyperledger FireFly stack must be running ('ff start').
 * 2. The Go chaincode must be deployed to the Fabric channel.
 * 3. FireFly Webhook must be configured to point to http://host.docker.internal:8080/api/v1/webhooks/firefly
 * * webEnvironment = DEFINED_PORT ensures the test server runs on 8080 so FireFly can push the webhook back.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PharmaChaincodeIntegrationIT {

    @Autowired
    private RegistryService registryService;

    @Autowired
    private CustodyService custodyService;

    @Autowired
    private PharmaceuticalRegistryRepository registryRepository;

    @Autowired
    private ChainOfCustodyRepository custodyRepository;

    @Autowired
    private SupplyChainParticipantRepository participantRepository;

    private static SupplyChainParticipant testManufacturer;
    private static SupplyChainParticipant testZammsa;
    private static String testBatchNumber;
    private static String testQrHash;

    @BeforeEach
    void setupParticipants() {
        // Ensure test participants exist in the database before running chaincode tests
        if (testManufacturer == null) {
            testManufacturer = new SupplyChainParticipant();
            testManufacturer.setParticipantCode("MFG-TEST-001");
            testManufacturer.setParticipantName("Test Pharma Corp");
            testManufacturer.setRole(ParticipantType.MANUFACTURER);
            testManufacturer.setEmail("testmfg@pharma.com");
            testManufacturer.setPasswordHash("hashed_password");
            testManufacturer = participantRepository.save(testManufacturer);

            testZammsa = new SupplyChainParticipant();
            testZammsa.setParticipantCode("ZAM-TEST-001");
            testZammsa.setParticipantName("ZAMMSA Main Warehouse");
            testZammsa.setRole(ParticipantType.ZAMMSA);
            testZammsa.setEmail("testzammsa@pharma.com");
            testZammsa.setPasswordHash("hashed_password");
            testZammsa = participantRepository.save(testZammsa);

            testBatchNumber = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @Test
    @Order(1)
    @DisplayName("E2E: Asset Creation & Webhook Consensus Loop")
    void testAssetRegistrationAndConsensus() throws InterruptedException {
        // 1. INITIATE TRANSACTION
        RegistryRequestDto request = new RegistryRequestDto(
                testBatchNumber,
                UUID.randomUUID().toString(), // Mock Product ID
                "Amoxicillin 500mg",
                testManufacturer.getParticipantId(),
                LocalDate.now().plusYears(2).toString(),
                true,
                true
        );

        // Execute synchronous backend service
        registryService.registerBatch(request);

        // 2. VERIFY INITIAL DB INTENT (PENDING)
        Optional<PharmaceuticalRegistry> pendingRecordOpt = registryRepository.findByBatchNumber(testBatchNumber);
        Assertions.assertTrue(pendingRecordOpt.isPresent(), "Record should exist in DB immediately.");
        PharmaceuticalRegistry record = pendingRecordOpt.get();
        Assertions.assertTrue(
                record.getCurrentStatus().contains("PENDING"),
                "Initial status must be PENDING_CONFIRMATION or PENDING_BLOCKCHAIN"
        );

        // Store the securely generated QR hash for the next tests
        testQrHash = record.getQrHash();

        // 3. ASYNCHRONOUS POLLING (Waiting for FireFly Webhook)
        // FireFly takes time to order the transaction in Fabric, commit it, and fire the webhook.
        boolean isConfirmed = false;
        int maxRetries = 15; // 30 seconds total wait maximum

        System.out.println("⏳ Waiting for blockchain consensus and FireFly Webhook...");

        for (int i = 0; i < maxRetries; i++) {
            TimeUnit.SECONDS.sleep(2);

            PharmaceuticalRegistry updatedRecord = registryRepository.findByQrHash(testQrHash).orElseThrow();
            if ("ON_CHAIN".equals(updatedRecord.getCurrentStatus())) {
                isConfirmed = true;
                Assertions.assertNotNull(updatedRecord.getBlockchainTxId(), "Transaction ID must be populated by Webhook");
                System.out.println("✅ Webhook received! Asset confirmed on ledger. TxID: " + updatedRecord.getBlockchainTxId());
                break;
            }
        }

        Assertions.assertTrue(isConfirmed, "Webhook failed to confirm the asset within the timeout period. Check FireFly logs.");
    }

    @Test
    @Order(2)
    @DisplayName("E2E: Custody Transfer & Webhook Verification")
    void testCustodyTransfer() throws InterruptedException {
        // Ensure Order(1) succeeded
        Assertions.assertNotNull(testQrHash, "QR Hash must be available from previous test");

        // 1. INITIATE TRANSFER
        TransferRequestDto request = new TransferRequestDto(
                testQrHash,
                testManufacturer.getParticipantId(),
                testZammsa.getParticipantId(),
                "SHIPPED",
                1000
        );

        long initialCustodyCount = custodyRepository.count();

        // Execute synchronous backend service
        custodyService.transferCustody(request);

        // 2. ASYNCHRONOUS POLLING
        boolean eventLogged = false;
        int maxRetries = 15;

        System.out.println("⏳ Waiting for Custody Transfer consensus...");

        for (int i = 0; i < maxRetries; i++) {
            TimeUnit.SECONDS.sleep(2);

            if (custodyRepository.count() > initialCustodyCount) {
                eventLogged = true;
                System.out.println("✅ Custody Transfer confirmed on ledger and synced to DB.");
                break;
            }
        }

        Assertions.assertTrue(eventLogged, "Webhook failed to sync the custody event within the timeout period.");
    }
}