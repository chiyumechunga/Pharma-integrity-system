package com.chiyumechunga.chaincode;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public final class PharmaIntegrityTest {

    @Nested
    class ManufactureTransaction {

        @Test
        public void whenBatchExists_thenThrowsError() {
            PharmaIntegrity contract = new PharmaIntegrity();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);
            // Simulate that "BATCH01" already exists on the ledger
            when(stub.getStringState("BATCH01")).thenReturn("{\"some\":\"data\"}");

            assertThrows(ChaincodeException.class, () -> {
                contract.manufacture(ctx, "BATCH01", "hash123", "Aspirin", "2025-01-01");
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

            DrugTruth result = contract.manufacture(ctx, "BATCH01", "hash123", "Aspirin", "2025-01-01");

            assertEquals("BATCH01", result.getBatchId());
            assertEquals("MANUFACTURED", result.getStatus());
            // Verify that an event was set
            verify(stub).setEvent(eq("DrugManufactured"), any(byte[].class));
        }
    }
}