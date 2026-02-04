package com.chiyumechunga.chaincode;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import com.owlike.genson.Genson;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Smart Contract enforcing ZAMMSA/ZAMRA compliance rules.
 * Handles lifecycle transitions and emits events for PostgreSQL synchronization.
 */
@Contract(
        name = "PharmaIntegrity",
        info = @Info(
                title = "Pharma Integrity Contract",
                description = "Smart Contract for ZAMMSA/ZAMRA ecosystem",
                version = "1.0.3",
                contact = @Contact(email = "chiyume@capstone.zm", name = "Chiyume Chunga")
        )
)
@Default
public class PharmaIntegrity implements ContractInterface {

    private final Genson genson = new Genson();

    // --- Event Models (Mapped 1:1 to Postgres Tables) ---

    /** Table: pharmaceutical_registry */
    class ManufactureEvent {
        public String batchId;
        public String qrHash;
        public String drugName;
        public String manufacturerId;
        public String expiryDate;
        public String txId;
        public String timestamp;
    }

    /** Table: chain_of_custody_events */
    class CustodyEvent {
        public String batchId;
        public String fromParticipant;
        public String toParticipant;
        public String eventType;
        public String txId;
        public String timestamp;
    }

    /** Table: regulatory_scrutiny */
    class ScrutinyEvent {
        public String batchId;
        public String inspectorId;
        public String result;
        public String notes;
        public String txId;
        public String timestamp;
    }

    /** Mapped to Dispensing Logic / Sales */
    class DispenseEvent {
        public String batchId;
        public String pharmacyId;
        public String txId;
        public String timestamp;
    }

    /** Emergency System */
    class RecallEvent {
        public String batchId;
        public String regulatorId;
        public String reason;
        public String txId;
        public String timestamp;
    }

    // --- Contract Transactions ---

    /**
     * Creates a new batch asset and triggers off-chain DB insertion.
     * @param manufacturerUuid Used to link Foreign Key in Postgres.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth manufacture(final Context ctx,
                                 final String batchId,
                                 final String qrHash,
                                 final String drugName,
                                 final String expiryDate,
                                 final String manufacturerUuid) {

        ChaincodeStub stub = ctx.getStub();

        if (batchExists(ctx, batchId)) {
            throw new ChaincodeException("Batch " + batchId + " already exists", "DUPLICATE_BATCH");
        }

        // Store Logic: Save MSP (Chain Security) and UUID (DB Sync)
        DrugTruth batch = new DrugTruth(batchId, qrHash, ctx.getClientIdentity().getMSPID(), manufacturerUuid, "MANUFACTURED", expiryDate);
        stub.putStringState(batchId, genson.serialize(batch));

        // Event Logic: Emit data required for 'pharmaceutical_registry'
        ManufactureEvent event = new ManufactureEvent();
        event.batchId = batchId;
        event.qrHash = qrHash;
        event.drugName = drugName;
        event.manufacturerId = manufacturerUuid;
        event.expiryDate = expiryDate;
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("DrugManufactured", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * Transfers ownership (e.g. Manufacturer -> Distributor).
     * @param newOwnerUuid UUID of the receiver for DB tracking.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth transfer(final Context ctx,
                              final String batchId,
                              final String newOwnerMsp,
                              final String newOwnerUuid) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth batch = getBatchState(ctx, batchId);
        String caller = ctx.getClientIdentity().getMSPID();

        // Check 1: Ownership
        if (!batch.getCurrentOwner().equalsIgnoreCase(caller)) {
            throw new ChaincodeException("Unauthorized: You do not own this batch", "ACCESS_DENIED");
        }

        // Check 2: Safety
        if (batch.getStatus().startsWith("RECALLED")) {
            throw new ChaincodeException("Cannot transfer RECALLED batch", "SAFETY_VIOLATION");
        }

        // State Update
        String oldOwnerUuid = batch.getCurrentOwnerUuid();
        batch.setCurrentOwner(newOwnerMsp);
        batch.setCurrentOwnerUuid(newOwnerUuid);
        batch.setStatus("IN_TRANSIT");
        stub.putStringState(batchId, genson.serialize(batch));

        // Event Logic: Emit data for 'chain_of_custody_events'
        CustodyEvent event = new CustodyEvent();
        event.batchId = batchId;
        event.fromParticipant = oldOwnerUuid;
        event.toParticipant = newOwnerUuid;
        event.eventType = "DISTRIBUTED";
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("CustodyTransferred", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * Records Lab Results from ZAMRA.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth inspect(final Context ctx,
                             final String batchId,
                             final String result,
                             final String notes,
                             final String inspectorUuid) {

        DrugTruth batch = getBatchState(ctx, batchId);

        batch.setStatus("INSPECTED_" + result);
        ctx.getStub().putStringState(batchId, genson.serialize(batch));

        ScrutinyEvent event = new ScrutinyEvent();
        event.batchId = batchId;
        event.inspectorId = inspectorUuid;
        event.result = result;
        event.notes = notes;
        event.txId = ctx.getStub().getTxId();
        event.timestamp = Instant.ofEpochMilli(ctx.getStub().getTxTimestamp().toEpochMilli()).toString();

        ctx.getStub().setEvent("LabInspectionCompleted", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * Pharmacy sells to patient. Includes strict Expiry Check.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth dispense(final Context ctx, final String batchId) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth batch = getBatchState(ctx, batchId);
        String caller = ctx.getClientIdentity().getMSPID();

        if (!batch.getCurrentOwner().equalsIgnoreCase(caller)) {
            throw new ChaincodeException("Unauthorized", "ACCESS_DENIED");
        }

        if (batch.getStatus().startsWith("RECALLED")) {
            throw new ChaincodeException("Cannot dispense RECALLED batch", "SAFETY_VIOLATION");
        }

        // Critical Check: Is it expired?
        try {
            Instant txTime = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli());
            LocalDate expiryDate = LocalDate.parse(batch.getExpiryDate());
            Instant expiryInstant = expiryDate.atStartOfDay(ZoneId.of("UTC")).toInstant();

            if (txTime.isAfter(expiryInstant)) {
                throw new ChaincodeException("EXPIRY ALERT: Batch expired on " + batch.getExpiryDate(), "EXPIRED_DRUG");
            }
        } catch (DateTimeParseException e) {
            throw new ChaincodeException("Invalid Expiry Date Format", "DATA_CORRUPTION");
        }

        batch.setStatus("DISPENSED");
        stub.putStringState(batchId, genson.serialize(batch));

        DispenseEvent event = new DispenseEvent();
        event.batchId = batchId;
        event.pharmacyId = batch.getCurrentOwnerUuid();
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("DrugDispensed", genson.serialize(event).getBytes());

        return batch;
    }

    /**
     * ZAMRA Emergency Recall. Overrides ownership to freeze asset.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth recall(final Context ctx, final String batchId, final String reason) {

        ChaincodeStub stub = ctx.getStub();
        String caller = ctx.getClientIdentity().getMSPID();

        // RBAC: Only ZAMRA allowed
        if (!caller.toLowerCase().contains("zamra")) {
            throw new ChaincodeException("Unauthorized: Only ZAMRA can recall", "ACCESS_DENIED");
        }

        DrugTruth batch = getBatchState(ctx, batchId);

        batch.setStatus("RECALLED: " + reason);
        stub.putStringState(batchId, genson.serialize(batch));

        RecallEvent event = new RecallEvent();
        event.batchId = batchId;
        event.regulatorId = caller;
        event.reason = reason;
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();

        stub.setEvent("EmergencyRecall", genson.serialize(event).getBytes());

        return batch;
    }

    /** Public Read-Only Verification */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public DrugTruth verifyBatch(final Context ctx, final String batchId) {
        return getBatchState(ctx, batchId);
    }

    // --- Helpers ---

    private DrugTruth getBatchState(Context ctx, String batchId) {
        String json = ctx.getStub().getStringState(batchId);
        if (json == null || json.isEmpty()) throw new ChaincodeException("Batch not found", "NOT_FOUND");
        return genson.deserialize(json, DrugTruth.class);
    }

    private boolean batchExists(Context ctx, String batchId) {
        String json = ctx.getStub().getStringState(batchId);
        return (json != null && !json.isEmpty());
    }
}