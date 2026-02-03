package com.chiyumechunga.chaincode;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import com.owlike.genson.Genson;
import java.time.Instant;

@Contract(
        name = "PharmaIntegrity",
        info = @Info(
                title = "Pharma Integrity Contract",
                description = "Smart Contract for ZAMMSA/ZAMRA ecosystem used in the Blockchain Application",
                version = "1.0.0",
                contact = @Contact(email = "chiyumechunga@gmail.com", name = "Chiyume Chunga")
        )
)
@Default
public class PharmaIntegrity implements ContractInterface {

    private final Genson genson = new Genson();

    // --- EVENT MODELS (Must match Backend DTOs) ---
    class ManufactureEvent {
        public String batchId;
        public String qrHash;
        public String drugName;
        public String manufacturerId;
        public String expiryDate;
        public String txId;
        public String timestamp;
    }

    class CustodyEvent {
        public String batchId;
        public String fromParticipant;
        public String toParticipant;
        public String eventType;
        public String txId;
        public String timestamp;
    }

    class ScrutinyEvent {
        public String batchId;
        public String inspectorId;
        public String result;
        public String notes;
        public String txId;
        public String timestamp;
    }

    // Triggers a 'sale_event' in Postgres
    class DispenseEvent {
        public String batchId;
        public String pharmacyId; // The MSP ID of the pharmacy
        public String txId;
        public String timestamp;
    }

    // Triggers an 'emergency_alert' in Postgres
    class RecallEvent {
        public String batchId;
        public String regulatorId;
        public String reason;
        public String txId;
        public String timestamp;
    }

    // --- CONTRACT METHODS ---

    /**
     * 1. MANUFACTURE BATCH
     * Creates the asset and triggers Postgres 'pharmaceutical_registry' insertion.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth manufacture(final Context ctx,
                                 final String batchId,
                                 final String qrHash,
                                 final String drugName,
                                 final String expiryDate) {

        ChaincodeStub stub = ctx.getStub();
        String txId = stub.getTxId();
        String mspId = ctx.getClientIdentity().getMSPID();

        if (batchExists(ctx, batchId)) {
            throw new ChaincodeException("Batch " + batchId + " already exists", "DUPLICATE_BATCH");
        }

        // 1. Write Truth to Ledger
        // NEW / FIXED LINE
        DrugTruth batch = new DrugTruth(batchId, qrHash, mspId, "MANUFACTURED", expiryDate);
        stub.putStringState(batchId, genson.serialize(batch));

        // 2. Emit Event for Spring Boot
        ManufactureEvent event = new ManufactureEvent();
        event.batchId = batchId;
        event.qrHash = qrHash;
        event.drugName = drugName;
        event.manufacturerId = mspId;
        event.expiryDate = expiryDate;
        event.txId = txId;
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("DrugManufactured", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * 2. TRANSFER CUSTODY
     * Updates owner and triggers Postgres 'chain_of_custody_events' insertion.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth transfer(final Context ctx,
                              final String batchId,
                              final String newOwnerMsp) {

        ChaincodeStub stub = ctx.getStub();
        String txId = stub.getTxId();
        String currentOwner = ctx.getClientIdentity().getMSPID();

        DrugTruth batch = getBatchState(ctx, batchId);

        // Verification: Only the current owner can transfer
        if (!batch.getCurrentOwner().equalsIgnoreCase(currentOwner)) {
            // In production, uncomment strictly:
            throw new ChaincodeException("Unauthorized: You do not own this batch", "ACCESS_DENIED");
        }

        String oldOwner = batch.getCurrentOwner();

        // 1. Update Truth
        batch.setCurrentOwner(newOwnerMsp);
        batch.setStatus("IN_TRANSIT");
        stub.putStringState(batchId, genson.serialize(batch));

        // 2. Emit Event
        CustodyEvent event = new CustodyEvent();
        event.batchId = batchId;
        event.fromParticipant = oldOwner;
        event.toParticipant = newOwnerMsp;
        event.eventType = "DISTRIBUTED";
        event.txId = txId;
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("CustodyTransferred", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * 3. INSPECT / RECALL
     * Updates status and triggers Postgres 'regulatory_scrutiny' insertion.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth inspect(final Context ctx,
                             final String batchId,
                             final String result,
                             final String notes) {

        DrugTruth batch = getBatchState(ctx, batchId);
        String txId = ctx.getStub().getTxId();

        // 1. Update Truth
        batch.setStatus("INSPECTED_" + result);
        ctx.getStub().putStringState(batchId, genson.serialize(batch));

        // 2. Emit Event
        ScrutinyEvent event = new ScrutinyEvent();
        event.batchId = batchId;
        event.inspectorId = ctx.getClientIdentity().getMSPID();
        event.result = result;
        event.notes = notes;
        event.txId = txId;
        event.timestamp = Instant.ofEpochMilli(ctx.getStub().getTxTimestamp().toEpochMilli()).toString();

        ctx.getStub().setEvent("LabInspectionCompleted", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * 4. VERIFY BATCH
     * Read-only method for the public verification portal.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public DrugTruth verifyBatch(final Context ctx, final String batchId) {
        return getBatchState(ctx, batchId);
    }

    // --- HELPERS ---
    private DrugTruth getBatchState(Context ctx, String batchId) {
        String json = ctx.getStub().getStringState(batchId);
        if (json == null || json.isEmpty()) throw new ChaincodeException("Batch not found", "NOT_FOUND");
        return genson.deserialize(json, DrugTruth.class);
    }

    private boolean batchExists(Context ctx, String batchId) {
        String json = ctx.getStub().getStringState(batchId);
        return (json != null && !json.isEmpty());
    }

    /**
     * 5. DISPENSE (Pharmacy sells to Patient)
     * Marks the end of the supply chain lifecycle.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth dispense(final Context ctx, final String batchId) {

        ChaincodeStub stub = ctx.getStub();
        String txId = stub.getTxId();
        String caller = ctx.getClientIdentity().getMSPID();

        DrugTruth batch = getBatchState(ctx, batchId);

        // Verification: Only current owner can dispense
        if (!batch.getCurrentOwner().equalsIgnoreCase(caller)) {
            throw new ChaincodeException("Unauthorized: You do not own this batch", "ACCESS_DENIED");
        }

        // Verification: Cannot dispense recalled or expired drugs
        if (batch.getStatus().contains("RECALLED")) {
            throw new ChaincodeException("Cannot dispense RECALLED batch", "SAFETY_VIOLATION");
        }

        // 1. Update Truth
        batch.setStatus("DISPENSED");
        stub.putStringState(batchId, genson.serialize(batch));

        // 2. Emit Event
        DispenseEvent event = new DispenseEvent();
        event.batchId = batchId;
        event.pharmacyId = caller;
        event.txId = txId;
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("DrugDispensed", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * 6. RECALL (ZAMRA Emergency Action)
     * Overrides ownership to lock a batch immediately.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth recall(final Context ctx, final String batchId, final String reason) {

        ChaincodeStub stub = ctx.getStub();
        String txId = stub.getTxId();
        String caller = ctx.getClientIdentity().getMSPID();

        // Verification: Strict Role Check for ZAMRA
        // You must replace "ZAMRAMSP" with your actual MSP ID from stack.json
        if (!caller.contains("zamra") && !caller.contains("Regulator")) {
            throw new ChaincodeException("Unauthorized: Only ZAMRA can recall batches", "ACCESS_DENIED");
        }

        DrugTruth batch = getBatchState(ctx, batchId);

        // 1. Update Truth (Force Status)
        batch.setStatus("RECALLED: " + reason);
        stub.putStringState(batchId, genson.serialize(batch));

        // 2. Emit Event
        RecallEvent event = new RecallEvent();
        event.batchId = batchId;
        event.regulatorId = caller;
        event.reason = reason;
        event.txId = txId;
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("EmergencyRecall", genson.serialize(event).getBytes());

        return batch;
    }

}