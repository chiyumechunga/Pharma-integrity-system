package com.chiyumechunga.chaincode;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Backend-Driven Authorization with Two-Phase QR Hash Workflow
 */
public final class PharmaIntegrityTest {

    private final Genson genson = new Genson();

    @Nested
    class CreateAssetTransaction {

        @Test
        public void whenBatchIsNew_thenCreatesWithoutQRHash() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(stub.getStringState("BATCH001")).thenReturn("");
            when(identity.getMSPID()).thenReturn("ManufacturerMSP");
            when(stub.getTxId()).thenReturn("tx123456");

            DrugTruth result = contract.CreateAsset(
                    ctx,
                    "BATCH001",
                    "prod-uuid-123",
                    "Aspirin 100mg",
                    "mfg-uuid-456",
                    "ManufacturerMSP",
                    "2024-01-15",
                    "2026-01-15"
            );

            // Assertions
            assertEquals("BATCH001", result.getBatchNumber());
            assertEquals("Aspirin 100mg", result.getProductName());
            assertEquals("mfg-uuid-456", result.getManufacturerId());
            assertEquals("2026-01-15", result.getExpiryDate());
            assertEquals("", result.getQrHash()); // QR hash is empty initially
            assertEquals("PENDING_BLOCKCHAIN", result.getStatus());
            assertEquals("ManufacturerMSP", result.getCurrentOwner());

            // Verify blockchain write
            verify(stub).putStringState(eq("BATCH001"), anyString());

            // Verify event emission
            ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(stub).setEvent(eq("DrugManufactured"), eventCaptor.capture());

            String eventJson = new String(eventCaptor.getValue());
            assertTrue(eventJson.contains("BATCH001"));
            assertTrue(eventJson.contains("tx123456"));
        }

        @Test
        public void whenBatchExists_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(stub.getStringState("BATCH001")).thenReturn("{\"batch_number\":\"BATCH001\"}");

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.CreateAsset(ctx, "BATCH001", "prod-uuid", "Aspirin",
                    "mfg-uuid", "ManufacturerMSP", "2024-01-15", "2026-01-15"));

            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        public void whenBatchNumberEmpty_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.CreateAsset(ctx, "", "prod-uuid", "Aspirin",
                    "mfg-uuid", "ManufacturerMSP", "2024-01-15", "2026-01-15"));

            assertTrue(exception.getMessage().contains("cannot be empty"));
        }
    }

    @Nested
    class AttachQRHashTransaction {

        @Test
        public void whenValidQRHash_thenAttachesSuccessfully() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(stub.getTxId()).thenReturn("tx-attach-123");

            // Existing batch without QR hash
            DrugTruth existingBatch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "", "ManufacturerMSP", "mfg-uuid", "PENDING_BLOCKCHAIN"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(existingBatch));

            String qrHash = "a3f5e7b9c1d2f4a6e8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4";
            String registryId = "reg-uuid-789";

            DrugTruth result = contract.AttachQRHash(ctx, "BATCH001", qrHash, registryId);

            // Assertions
            assertEquals(qrHash, result.getQrHash());
            assertEquals("CONFIRMED", result.getStatus());

            // Verify blockchain update
            verify(stub).putStringState(eq("BATCH001"), anyString());

            // Verify event emission
            ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(stub).setEvent(eq("QRHashAttached"), eventCaptor.capture());

            String eventJson = new String(eventCaptor.getValue());
            assertTrue(eventJson.contains(qrHash));
            assertTrue(eventJson.contains(registryId));
        }

        @Test
        public void whenQRHashInvalidFormat_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth existingBatch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "", "ManufacturerMSP", "mfg-uuid", "PENDING_BLOCKCHAIN"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(existingBatch));

            // Invalid QR hash (not 64 hex chars)
            String invalidQRHash = "not-a-valid-hash";

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.AttachQRHash(ctx, "BATCH001", invalidQRHash, "reg-uuid"));

            assertTrue(exception.getMessage().contains("64-char hex"));
        }

        @Test
        public void whenQRHashEmpty_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth existingBatch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "", "ManufacturerMSP", "mfg-uuid", "PENDING_BLOCKCHAIN"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(existingBatch));

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.AttachQRHash(ctx, "BATCH001", "", "reg-uuid"));

            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        public void whenQRHashAlreadyExists_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            // Batch already has QR hash
            DrugTruth existingBatch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "a3f5e7b9c1d2f4a6e8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4",
                    "ManufacturerMSP", "mfg-uuid", "CONFIRMED"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(existingBatch));

            String newQRHash = "b4f6e8c0d3e5f7a9e1c3d5f7a9b1c3d5f7a9b1c3d5f7a9b1c3d5f7a9b1c3d5f7";

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.AttachQRHash(ctx, "BATCH001", newQRHash, "reg-uuid"));

            assertTrue(exception.getMessage().contains("already has QR hash"));
        }
    }

    @Nested
    class TransferCustodyTransaction {

        @Test
        public void whenValidTransfer_thenSuccessfullyRecorded() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("ManufacturerMSP");
            when(stub.getTxId()).thenReturn("tx-transfer-456");

            DrugTruth existingBatch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "a3f5e7b9c1d2f4a6e8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4",
                    "ManufacturerMSP", "mfg-uuid", "CONFIRMED"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(existingBatch));

            DrugTruth result = contract.TransferCustody(
                    ctx,
                    "BATCH001",
                    "mfg-uuid",          // from
                    "dist-uuid-123",     // to
                    "DistributorMSP",    // to MSP
                    "DISTRIBUTED",       // event type
                    10000                // quantity
            );

            assertEquals("DistributorMSP", result.getCurrentOwner());
            assertEquals("dist-uuid-123", result.getCurrentOwnerUuid());
            assertEquals("IN_TRANSIT_DISTRIBUTED", result.getStatus());

            // Verify event
            ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(stub).setEvent(eq("CustodyTransferred"), eventCaptor.capture());

            String eventJson = new String(eventCaptor.getValue());
            assertTrue(eventJson.contains("DISTRIBUTED"));
            assertTrue(eventJson.contains("10000"));
        }

        @Test
        public void whenNullParticipantUUIDs_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth existingBatch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "ManufacturerMSP", "mfg-uuid", "CONFIRMED"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(existingBatch));

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.TransferCustody(ctx, "BATCH001", null, null,
                    "DistributorMSP", "DISTRIBUTED", 10000));

            assertTrue(exception.getMessage().contains("cannot be null"));
        }

        @Test
        public void whenInvalidQuantity_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth existingBatch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "ManufacturerMSP", "mfg-uuid", "CONFIRMED"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(existingBatch));

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.TransferCustody(ctx, "BATCH001", "mfg-uuid", "dist-uuid",
                    "DistributorMSP", "DISTRIBUTED", -100));

            assertTrue(exception.getMessage().contains("must be positive"));
        }
    }

    @Nested
    class RecordScrutinyTransaction {

        @Test
        public void whenTestPasses_thenStatusUpdated() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("LabMSP");
            when(stub.getTxId()).thenReturn("tx-scrutiny-789");

            DrugTruth batch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "LabMSP", "lab-uuid", "RECEIVED_AT_LAB"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(batch));

            DrugTruth result = contract.RecordScrutiny(
                    ctx,
                    "BATCH001",
                    "inspector-uuid-123",
                    "PASSED",
                    "All quality tests passed"
            );

            assertEquals("QA_PASSED: All quality tests passed", result.getStatus());

            // Verify event
            ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(stub).setEvent(eq("LabScrutinyCompleted"), eventCaptor.capture());

            String eventJson = new String(eventCaptor.getValue());
            assertTrue(eventJson.contains("PASSED"));
            assertTrue(eventJson.contains("inspector-uuid-123"));
        }

        @Test
        public void whenTestFails_thenStatusReflectsFailure() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("LabMSP");
            when(stub.getTxId()).thenReturn("tx-scrutiny-790");

            DrugTruth batch = new DrugTruth(
                    "BATCH002", "Paracetamol", "mfg-uuid", "2026-01-15",
                    "qrhash2", "LabMSP", "lab-uuid", "RECEIVED_AT_LAB"
            );
            when(stub.getStringState("BATCH002")).thenReturn(genson.serialize(batch));

            DrugTruth result = contract.RecordScrutiny(
                    ctx,
                    "BATCH002",
                    "inspector-uuid-456",
                    "FAILED",
                    "Contamination detected in sample"
            );

            assertEquals("QA_FAILED: Contamination detected in sample", result.getStatus());
        }

        @Test
        public void whenTestPending_thenStatusPending() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(stub.getTxId()).thenReturn("tx-scrutiny-791");

            DrugTruth batch = new DrugTruth(
                    "BATCH003", "Ibuprofen", "mfg-uuid", "2026-01-15",
                    "qrhash3", "LabMSP", "lab-uuid", "RECEIVED_AT_LAB"
            );
            when(stub.getStringState("BATCH003")).thenReturn(genson.serialize(batch));

            DrugTruth result = contract.RecordScrutiny(
                    ctx,
                    "BATCH003",
                    "inspector-uuid-789",
                    "PENDING",
                    "Awaiting final test results"
            );

            assertEquals("QA_PENDING: Awaiting final test results", result.getStatus());
        }
    }

    @Nested
    class DispenseBatchTransaction {

        @Test
        public void whenValidDispense_thenSuccessfullyRecorded() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("PharmacyMSP");
            when(stub.getTxId()).thenReturn("tx-dispense-999");

            DrugTruth batch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "PharmacyMSP", "pharmacy-uuid", "AT_PHARMACY"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(batch));

            DrugTruth result = contract.DispenseBatch(
                    ctx,
                    "BATCH001",
                    "pharmacy-uuid",
                    "PATIENT-001",
                    50
            );

            assertEquals("DISPENSED", result.getStatus());

            // Verify event
            ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(stub).setEvent(eq("DrugDispensed"), eventCaptor.capture());

            String eventJson = new String(eventCaptor.getValue());
            assertTrue(eventJson.contains("PATIENT-001"));
            assertTrue(eventJson.contains("50"));
        }

        @Test
        public void whenPharmacyUuidNull_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth batch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "PharmacyMSP", "pharmacy-uuid", "AT_PHARMACY"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(batch));

            Exception exception = assertThrows(ChaincodeException.class, () -> {
                contract.DispenseBatch(ctx, "BATCH001", null, "PATIENT-001", 50);
            });

            assertTrue(exception.getMessage().contains("Pharmacy UUID required"));
        }

        @Test
        public void whenQuantityInvalid_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth batch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "PharmacyMSP", "pharmacy-uuid", "AT_PHARMACY"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(batch));

            Exception exception = assertThrows(ChaincodeException.class, () -> contract.DispenseBatch(ctx, "BATCH001", "pharmacy-uuid", "PATIENT-001", 0));

            assertTrue(exception.getMessage().contains("must be positive"));
        }
    }

    @Nested
    class RecallBatchTransaction {

        @Test
        public void whenRecalled_thenStatusUpdated() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("ZAMRAMSP");
            when(stub.getTxId()).thenReturn("tx-recall-111");

            DrugTruth batch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "PharmacyMSP", "pharmacy-uuid", "AT_PHARMACY"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(batch));

            DrugTruth result = contract.RecallBatch(
                    ctx,
                    "BATCH001",
                    "prod-uuid-123",
                    "zamra-regulator-uuid",
                    "Contamination found in manufacturing facility",
                    "CLASS_I"
            );

            assertEquals("RECALLED", result.getStatus());

            // Verify event
            ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(stub).setEvent(eq("DrugRecalled"), eventCaptor.capture());

            String eventJson = new String(eventCaptor.getValue());
            assertTrue(eventJson.contains("CLASS_I"));
            assertTrue(eventJson.contains("Contamination"));
        }
    }

    @Nested
    class UpdateStatusTransaction {

        @Test
        public void whenStatusUpdated_thenReflectedInAsset() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth batch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "DistributorMSP", "dist-uuid", "IN_TRANSIT"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(batch));

            DrugTruth result = contract.UpdateStatus(
                    ctx,
                    "BATCH001",
                    "ARRIVED_AT_WAREHOUSE",
                    "warehouse-manager-uuid"
            );

            assertEquals("ARRIVED_AT_WAREHOUSE", result.getStatus());
        }
    }

    @Nested
    class QueryTransactions {

        @Test
        public void whenAssetExists_thenReadSucceeds() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);

            DrugTruth batch = new DrugTruth(
                    "BATCH001", "Aspirin", "mfg-uuid", "2026-01-15",
                    "qrhash", "ManufacturerMSP", "mfg-uuid", "CONFIRMED"
            );
            when(stub.getStringState("BATCH001")).thenReturn(genson.serialize(batch));

            DrugTruth result = contract.ReadAsset(ctx, "BATCH001");

            assertEquals("BATCH001", result.getBatchNumber());
            assertEquals("qrhash", result.getQrHash());
            assertEquals("CONFIRMED", result.getStatus());
        }

        @Test
        public void whenAssetDoesNotExist_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("NONEXISTENT")).thenReturn("");

            Exception exception = assertThrows(ChaincodeException.class, () -> {
                contract.ReadAsset(ctx, "NONEXISTENT");
            });

            assertTrue(exception.getMessage().contains("does not exist"));
        }

        @Test
        public void whenCheckingExistence_thenReturnsCorrectBoolean() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("EXISTS")).thenReturn("{\"batch_number\":\"EXISTS\"}");
            when(stub.getStringState("NOTEXISTS")).thenReturn("");

            assertTrue(contract.AssetExists(ctx, "EXISTS"));
            assertFalse(contract.AssetExists(ctx, "NOTEXISTS"));
        }
    }

    @Nested
    class EndToEndWorkflow {

        @Test
        public void testCompleteLifecycle() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("ManufacturerMSP");
            when(stub.getTxId()).thenReturn("tx-lifecycle-001");

            // Phase 1: Create asset without QR hash
            when(stub.getStringState("BATCH-LIFECYCLE")).thenReturn("");

            DrugTruth created = contract.CreateAsset(
                    ctx, "BATCH-LIFECYCLE", "prod-uuid", "Aspirin",
                    "mfg-uuid", "ManufacturerMSP", "2024-01-15", "2026-01-15"
            );

            assertEquals("", created.getQrHash());
            assertEquals("PENDING_BLOCKCHAIN", created.getStatus());

            // Phase 2: Attach QR hash
            when(stub.getStringState("BATCH-LIFECYCLE")).thenReturn(genson.serialize(created));

            String qrHash = "a3f5e7b9c1d2f4a6e8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4f6a8b0c2d4";
            DrugTruth confirmed = contract.AttachQRHash(
                    ctx, "BATCH-LIFECYCLE", qrHash, "reg-uuid"
            );

            assertEquals(qrHash, confirmed.getQrHash());
            assertEquals("CONFIRMED", confirmed.getStatus());

            // Phase 3: Transfer custody
            when(stub.getStringState("BATCH-LIFECYCLE")).thenReturn(genson.serialize(confirmed));

            DrugTruth transferred = contract.TransferCustody(
                    ctx, "BATCH-LIFECYCLE", "mfg-uuid", "dist-uuid",
                    "DistributorMSP", "DISTRIBUTED", 10000
            );

            assertEquals("DistributorMSP", transferred.getCurrentOwner());
            assertEquals("IN_TRANSIT_DISTRIBUTED", transferred.getStatus());

            // Phase 4: Quality check
            when(stub.getStringState("BATCH-LIFECYCLE")).thenReturn(genson.serialize(transferred));
            when(identity.getMSPID()).thenReturn("LabMSP");

            DrugTruth inspected = contract.RecordScrutiny(
                    ctx, "BATCH-LIFECYCLE", "inspector-uuid", "PASSED", "All tests passed"
            );

            assertTrue(inspected.getStatus().contains("QA_PASSED"));

            // Verify all blockchain operations occurred
            verify(stub, atLeast(4)).putStringState(eq("BATCH-LIFECYCLE"), anyString());
            verify(stub, atLeast(4)).setEvent(anyString(), any(byte[].class));
        }
    }
}