package com.chiyumechunga.chaincode;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public final class PharmaIntegrityTest {

    private final Genson genson = new Genson();

    @Nested
    class ManufactureTransaction {

        @Test
        public void whenBatchExists_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(mock(ClientIdentity.class));

            // Simulate that "BATCH01" already exists
            when(stub.getStringState("BATCH01")).thenReturn("{\"some\":\"data\"}");

            // Call with NEW arguments (UUID included)
            assertThrows(ChaincodeException.class, () -> {
                contract.manufacture(ctx, "BATCH01", "hash123", "Aspirin", "2025-01-01", "550e8400-e29b-41d4-a716-446655440000");
            });
        }

        @Test
        public void whenBatchIsNew_thenSuccess() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(stub.getStringState("BATCH01")).thenReturn(""); // Empty = New
            when(identity.getMSPID()).thenReturn("ManufacturerMSP");
            when(stub.getTxTimestamp()).thenReturn(Instant.now());

            // Call with NEW arguments
            DrugTruth result = contract.manufacture(ctx,
                    "BATCH01",
                    "hash123",
                    "Aspirin",
                    "2025-01-01",
                    "550e8400-e29b-41d4-a716-446655440000" // manufacturerUuid
            );

            assertEquals("BATCH01", result.getBatchId());
            assertEquals("MANUFACTURED", result.getStatus());
            assertEquals("550e8400-e29b-41d4-a716-446655440000", result.getCurrentOwnerUuid());

            verify(stub).setEvent(eq("DrugManufactured"), any(byte[].class));
        }
    }

    @Nested
    class DispenseTransaction {

        @Test
        public void whenDrugIsExpired_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("PharmacyMSP");

            // 1. Create a batch that expired in the past (2020)
            DrugTruth expiredBatch = new DrugTruth("BATCH02", "hash", "PharmacyMSP", "uuid-123", "IN_TRANSIT", "2020-01-01");
            when(stub.getStringState("BATCH02")).thenReturn(genson.serialize(expiredBatch));

            // 2. Set "Current Time" to NOW (2025/2026)
            when(stub.getTxTimestamp()).thenReturn(Instant.now());

            assertThrows(ChaincodeException.class, () -> {
                contract.dispense(ctx, "BATCH02");
            });
        }

        @Test
        public void whenDrugIsValid_thenSuccess() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("PharmacyMSP");

            // 1. Create a batch that expires in the future (2030)
            DrugTruth validBatch = new DrugTruth("BATCH03", "hash", "PharmacyMSP", "uuid-123", "AT_PHARMACY", "2030-01-01");
            when(stub.getStringState("BATCH03")).thenReturn(genson.serialize(validBatch));

            // 2. Set "Current Time" to NOW
            when(stub.getTxTimestamp()).thenReturn(Instant.now());

            DrugTruth result = contract.dispense(ctx, "BATCH03");

            assertEquals("DISPENSED", result.getStatus());
            verify(stub).setEvent(eq("DrugDispensed"), any(byte[].class));
        }
    }

    @Nested
    class RecallTransaction {
        @Test
        public void whenCallerIsNotZamra_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getClientIdentity()).thenReturn(identity);
            when(identity.getMSPID()).thenReturn("ManufacturerMSP"); // Wrong Role

            assertThrows(ChaincodeException.class, () -> {
                contract.recall(ctx, "BATCH01", "Bad Quality");
            });
        }

        @Test
        public void whenCallerIsZamra_thenSuccess() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity identity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(identity);

            // Mock ZAMRA Identity (Case insensitive check in code)
            when(identity.getMSPID()).thenReturn("ZAMRAMSP");
            when(stub.getTxTimestamp()).thenReturn(Instant.now());

            DrugTruth existingBatch = new DrugTruth("BATCH04", "hash", "PharmacyMSP", "uuid-pharma", "AT_PHARMACY", "2030-01-01");
            when(stub.getStringState("BATCH04")).thenReturn(genson.serialize(existingBatch));

            DrugTruth result = contract.recall(ctx, "BATCH04", "Safety Issue");

            // Ensure status was updated regardless of ownership
            assertEquals("RECALLED: Safety Issue", result.getStatus());
            verify(stub).setEvent(eq("EmergencyRecall"), any(byte[].class));
        }
    }
}