package com.chiyumechunga.chaincode;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PharmaIntegrityTest {

    private PharmaIntegrity contract;
    private Context ctx;
    private ChaincodeStub stub;
    private ClientIdentity clientIdentity;
    private final Genson genson = new Genson();

    @BeforeEach
    public void setup() {
        contract = new PharmaIntegrity();
        ctx = mock(Context.class);
        stub = mock(ChaincodeStub.class);
        clientIdentity = mock(ClientIdentity.class);

        when(ctx.getStub()).thenReturn(stub);
        when(ctx.getClientIdentity()).thenReturn(clientIdentity);
        when(clientIdentity.getMSPID()).thenReturn("Org1MSP");

        // Mock standard transaction details
        when(stub.getTxId()).thenReturn("tx-12345");
        when(stub.getTxTimestamp()).thenReturn(Instant.now());
    }

    @Test
    public void testCreateAsset_Success() {
        String batchNumber = "BATCH-001";

        // Mock that the asset does not exist yet
        when(stub.getStringState(batchNumber)).thenReturn("");

        DrugTruth result = contract.CreateAsset(ctx, batchNumber, "PROD-A", "Aspirin", "MFG-1", "Org1MSP", "2026-01-01", "2028-01-01");

        // Verify state is populated correctly
        assertEquals("REGISTERED", result.getStatus());
        assertEquals(batchNumber, result.getBatchNumber());
        assertEquals("Org1MSP", result.getCurrentOwner());
        assertEquals("MFG-1", result.getCurrentOwnerUuid());

        // Verify stub interactions
        verify(stub).putStringState(eq(batchNumber), anyString());
        verify(stub).setEvent(eq("AssetCreated"), any());
    }

    @Test
    public void testCreateAsset_AlreadyExists_ThrowsException() {
        String batchNumber = "BATCH-001";

        // Mock that the asset already exists
        when(stub.getStringState(batchNumber)).thenReturn("{\"batch_number\":\"BATCH-001\"}");

        ChaincodeException exception = assertThrows(ChaincodeException.class, () -> {
            contract.CreateAsset(ctx, batchNumber, "PROD-A", "Aspirin", "MFG-1", "Org1MSP", "2026-01-01", "2028-01-01");
        });

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    public void testAttachQRHash_Success() {
        String batchNumber = "BATCH-001";
        String validHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // 64 chars

        // Create an existing asset in REGISTERED state without a hash
        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "", "Org1MSP", "MFG-1", "REGISTERED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        DrugTruth result = contract.AttachQRHash(ctx, batchNumber, validHash, "REG-123");

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(validHash, result.getQrHash());
        verify(stub).putStringState(eq(batchNumber), anyString());
        verify(stub).setEvent(eq("QRHashAttached"), any());
    }

    @Test
    public void testAttachQRHash_InvalidFormat_ThrowsException() {
        String batchNumber = "BATCH-001";
        String invalidHash = "short-hash";

        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "", "Org1MSP", "MFG-1", "REGISTERED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        assertThrows(ChaincodeException.class, () -> {
            contract.AttachQRHash(ctx, batchNumber, invalidHash, "REG-123");
        });
    }

    @Test
    public void testTransferCustody_Success() {
        String batchNumber = "BATCH-001";

        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "hash", "Org1MSP", "MFG-1", "CONFIRMED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        DrugTruth result = contract.TransferCustody(ctx, batchNumber, "MFG-1", "DIST-1", "DistributorMSP", "SOLD", 100);

        assertEquals("IN_TRANSIT_SOLD", result.getStatus());
        assertEquals("DIST-1", result.getCurrentOwnerUuid());
        assertEquals("DistributorMSP", result.getCurrentOwner());
        verify(stub).putStringState(eq(batchNumber), anyString());
        verify(stub).setEvent(eq("CustodyTransferred"), any());
    }

    @Test
    public void testSubmitTestResult_Passed() {
        String batchNumber = "BATCH-001";

        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "hash", "Org1MSP", "MFG-1", "CONFIRMED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        DrugTruth result = contract.SubmitTestResult(ctx, batchNumber, "INSP-1", "PASSED", "All markers clear");

        assertTrue(result.getStatus().startsWith("QA_PASSED"));
        verify(stub).setEvent(eq("TestResultSubmitted"), any());
    }

    @Test
    public void testRecallBatch_Success() {
        String batchNumber = "BATCH-001";

        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "hash", "Org1MSP", "MFG-1", "CONFIRMED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        DrugTruth result = contract.RecallBatch(ctx, batchNumber, "ZAMRA-1", "Contamination detected");

        assertEquals("RECALLED", result.getStatus());
        verify(stub).setEvent(eq("DrugRecalled"), any());
    }

    @Test
    public void testRecallBatch_AlreadyDispensed_ThrowsException() {
        String batchNumber = "BATCH-001";

        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "hash", "Org1MSP", "MFG-1", "DISPENSED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        ChaincodeException exception = assertThrows(ChaincodeException.class, () -> {
            contract.RecallBatch(ctx, batchNumber, "ZAMRA-1", "Contamination detected");
        });

        assertTrue(exception.getMessage().contains("fully dispensed"));
    }

    @Test
    public void testDispenseBatch_Success() {
        String batchNumber = "BATCH-001";

        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "hash", "PharmMSP", "PHARM-1", "CONFIRMED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        DrugTruth result = contract.DispenseBatch(ctx, batchNumber, "PHARM-1", "PATIENT-123", 1);

        assertEquals("DISPENSED", result.getStatus());
        assertEquals("PATIENT-123", result.getCurrentOwnerUuid());
        verify(stub).setEvent(eq("DrugDispensed"), any());
    }

    @Test
    public void testDispenseBatch_Recalled_ThrowsException() {
        String batchNumber = "BATCH-001";

        DrugTruth existingAsset = new DrugTruth(batchNumber, "Aspirin", "MFG-1", "2028-01-01", "hash", "PharmMSP", "PHARM-1", "RECALLED");
        when(stub.getStringState(batchNumber)).thenReturn(genson.serialize(existingAsset));

        ChaincodeException exception = assertThrows(ChaincodeException.class, () -> {
            contract.DispenseBatch(ctx, batchNumber, "PHARM-1", "PATIENT-123", 1);
        });

        assertTrue(exception.getMessage().contains("recalled"));
    }
}