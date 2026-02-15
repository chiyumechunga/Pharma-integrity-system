package com.chiyumechunga.chaincode;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import com.owlike.genson.Genson;
import java.time.Instant;

/**
 * Pharma Integrity Chaincode - Backend-Driven Authorization Model
 *
 * WORKFLOW:
 * 1. Backend creates batch on blockchain (WITHOUT qr_hash)
 * 2. Backend receives tx_id from blockchain
 * 3. Backend generates QR hash: SHA256(batch_number + tx_id + product_name + expiry_date)
 * 4. Backend calls AttachQRHash() to update the batch
 * 5. Backend stores everything in Postgres
 *
 * This separates concerns and reduces chaincode overhead.
 */
@Contract(
        name = "PharmaIntegrity",
        info = @Info(
                title = "Pharma Integrity Contract",
                description = "Backend-driven pharmaceutical provenance ledger",
                version = "2.1.0"))
@Default
public final class PharmaIntegrity implements ContractInterface {

    private final Genson genson = new Genson();

    // ==========================================================
    // EVENTS (Aligned to Backend DTOs & Postgres Schema)
    // ==========================================================

    static class ManufactureEvent {
        @JsonProperty("batch_number")
        public String batchNumber;

        @JsonProperty("product_id")
        public String productId; // UUID from product_master table

        @JsonProperty("product_name")
        public String productName;

        @JsonProperty("manufacturer_id")
        public String manufacturerId;

        @JsonProperty("manufacturing_date")
        public String manufacturingDate;

        @JsonProperty("expiry_date")
        public String expiryDate;

        @JsonProperty("tx_id")
        public String txId;

        @JsonProperty("invoked_by_msp")
        public String invokedByMsp;
    }

    static class QRHashAttachedEvent {
        @JsonProperty("batch_number")
        public String batchNumber;

        @JsonProperty("qr_hash")
        public String qrHash;

        @JsonProperty("registry_id")
        public String registryId; // From Postgres pharmaceutical_registry

        @JsonProperty("tx_id")
        public String txId;
    }

    static class CustodyEvent {
        @JsonProperty("batch_number")
        public String batchNumber;

        @JsonProperty("from_participant_uuid")
        public String fromParticipantUuid;

        @JsonProperty("to_participant_uuid")
        public String toParticipantUuid;

        @JsonProperty("event_type")
        public String eventType; // MANUFACTURED, RECEIVED, DISTRIBUTED, DISPENSED

        @JsonProperty("quantity")
        public int quantity;

        @JsonProperty("tx_id")
        public String txId;

        @JsonProperty("timestamp")
        public String timestamp;

        @JsonProperty("invoked_by_msp")
        public String invokedByMsp;
    }

    static class ScrutinyEvent {
        @JsonProperty("batch_number")
        public String batchNumber;

        @JsonProperty("test_result")
        public String testResult; // PASSED, FAILED, PENDING

        @JsonProperty("lab_notes")
        public String labNotes;

        @JsonProperty("inspector_uuid")
        public String inspectorUuid;

        @JsonProperty("tx_id")
        public String txId;

        @JsonProperty("timestamp")
        public String timestamp;

        @JsonProperty("invoked_by_msp")
        public String invokedByMsp;
    }

    static class RecallEvent {
        @JsonProperty("batch_number")
        public String batchNumber;

        @JsonProperty("product_id")
        public String productId; // May recall entire product line

        @JsonProperty("recall_reason")
        public String recallReason;

        @JsonProperty("severity_level")
        public String severityLevel; // CLASS_I, CLASS_II, CLASS_III

        @JsonProperty("recalled_by_uuid")
        public String recalledByUuid;

        @JsonProperty("tx_id")
        public String txId;

        @JsonProperty("timestamp")
        public String timestamp;

        @JsonProperty("invoked_by_msp")
        public String invokedByMsp;
    }

    static class DispenseEvent {
        @JsonProperty("batch_number")
        public String batchNumber;

        @JsonProperty("pharmacy_uuid")
        public String pharmacyUuid;

        @JsonProperty("patient_id")
        public String patientId;

        @JsonProperty("quantity")
        public int quantity;

        @JsonProperty("tx_id")
        public String txId;

        @JsonProperty("timestamp")
        public String timestamp;

        @JsonProperty("invoked_by_msp")
        public String invokedByMsp;
    }

    // ==========================================================
    // TRANSACTIONS - Backend Authorization Model
    // ==========================================================

    /**
     * Step 1: Create batch WITHOUT QR hash.
     * Backend will generate QR hash after getting tx_id.
     *
     * @param productId UUID from product_master table
     * @param manufacturingDate ISO date string
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth CreateAsset(final Context ctx,
                                 final String batchNumber,
                                 final String productId,
                                 final String productName,
                                 final String manufacturerId,
                                 final String manufacturerMspId,
                                 final String manufacturingDate,
                                 final String expiryDate) {

        ChaincodeStub stub = ctx.getStub();

        // Basic validation
        if (AssetExists(ctx, batchNumber)) {
            throw new ChaincodeException("Asset " + batchNumber + " already exists", "ASSET_ALREADY_EXISTS");
        }

        if (batchNumber == null || batchNumber.isEmpty()) {
            throw new ChaincodeException("Batch number cannot be empty", "INVALID_INPUT");
        }

        // Create asset WITHOUT qr_hash (backend will add it)
        DrugTruth asset = new DrugTruth(
                batchNumber,
                productName,
                manufacturerId,
                expiryDate,
                "", // qr_hash is empty initially
                manufacturerMspId,
                manufacturerId,
                "PENDING_BLOCKCHAIN"
        );

        stub.putStringState(batchNumber, genson.serialize(asset));

        // Emit event
        ManufactureEvent event = new ManufactureEvent();
        event.batchNumber = batchNumber;
        event.productId = productId;
        event.productName = productName;
        event.manufacturerId = manufacturerId;
        event.manufacturingDate = manufacturingDate;
        event.expiryDate = expiryDate;
        event.txId = stub.getTxId();
        event.invokedByMsp = ctx.getClientIdentity().getMSPID();

        stub.setEvent("DrugManufactured", genson.serialize(event).getBytes());

        return asset;
    }

    /**
     * Step 2: Backend calls this after generating QR hash.
     *
     * QR Hash Generation Logic (Backend):
     * qr_hash = SHA256(batch_number + blockchain_tx_id + product_name + expiry_date)
     *
     * @param registryId UUID from pharmaceutical_registry table
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth AttachQRHash(final Context ctx,
                                  final String batchNumber,
                                  final String qrHash,
                                  final String registryId) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        // Validation
        if (qrHash == null || qrHash.isEmpty()) {
            throw new ChaincodeException("QR hash cannot be empty", "INVALID_INPUT");
        }

        if (!qrHash.matches("^[0-9a-fA-F]{64}$")) {
            throw new ChaincodeException("QR hash must be 64-char hex (SHA256)", "INVALID_HASH_FORMAT");
        }

        // Check if already has QR hash
        if (asset.getQrHash() != null && !asset.getQrHash().isEmpty()) {
            throw new ChaincodeException("Batch already has QR hash attached", "QR_ALREADY_EXISTS");
        }

        // Attach QR hash and confirm
        asset.setQrHash(qrHash);
        asset.setStatus("CONFIRMED");

        stub.putStringState(batchNumber, genson.serialize(asset));

        // Emit event
        QRHashAttachedEvent event = new QRHashAttachedEvent();
        event.batchNumber = batchNumber;
        event.qrHash = qrHash;
        event.registryId = registryId;
        event.txId = stub.getTxId();

        stub.setEvent("QRHashAttached", genson.serialize(event).getBytes());

        return asset;
    }

    /**
     * Transfer custody with quantity tracking.
     * Aligned with chain_of_custody_events table.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth TransferCustody(final Context ctx,
                                     final String batchNumber,
                                     final String fromParticipantUuid,
                                     final String toParticipantUuid,
                                     final String toParticipantMspId,
                                     final String eventType,
                                     final int quantity) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        // Validation
        if (fromParticipantUuid == null || toParticipantUuid == null) {
            throw new ChaincodeException("Participant UUIDs cannot be null", "INVALID_INPUT");
        }

        if (quantity <= 0) {
            throw new ChaincodeException("Quantity must be positive", "INVALID_QUANTITY");
        }

        // Update custody
        asset.setCurrentOwner(toParticipantMspId);
        asset.setCurrentOwnerUuid(toParticipantUuid);
        asset.setStatus("IN_TRANSIT_" + eventType);

        stub.putStringState(batchNumber, genson.serialize(asset));

        // Emit event
        CustodyEvent event = new CustodyEvent();
        event.batchNumber = batchNumber;
        event.fromParticipantUuid = fromParticipantUuid;
        event.toParticipantUuid = toParticipantUuid;
        event.eventType = eventType;
        event.quantity = quantity;
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();
        event.invokedByMsp = ctx.getClientIdentity().getMSPID();

        stub.setEvent("CustodyTransferred", genson.serialize(event).getBytes());

        return asset;
    }

    /**
     * Record lab inspection.
     * Aligned with regulatory_scrutiny table.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth RecordScrutiny(final Context ctx,
                                    final String batchNumber,
                                    final String inspectorUuid,
                                    final String testResult,
                                    final String labNotes) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        // Update status based on test result
        String statusPrefix = testResult.equals("PASSED") ? "QA_PASSED" :
                testResult.equals("FAILED") ? "QA_FAILED" : "QA_PENDING";

        asset.setStatus(statusPrefix + ": " + labNotes);

        stub.putStringState(batchNumber, genson.serialize(asset));

        // Emit event
        ScrutinyEvent event = new ScrutinyEvent();
        event.batchNumber = batchNumber;
        event.testResult = testResult;
        event.labNotes = labNotes;
        event.inspectorUuid = inspectorUuid;
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();
        event.invokedByMsp = ctx.getClientIdentity().getMSPID();

        stub.setEvent("LabScrutinyCompleted", genson.serialize(event).getBytes());

        return asset;
    }

    /**
     * Record dispensing to patient.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth DispenseBatch(final Context ctx,
                                   final String batchNumber,
                                   final String pharmacyUuid,
                                   final String patientId,
                                   final int quantity) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        if (pharmacyUuid == null || pharmacyUuid.isEmpty()) {
            throw new ChaincodeException("Pharmacy UUID required", "INVALID_INPUT");
        }

        if (quantity <= 0) {
            throw new ChaincodeException("Quantity must be positive", "INVALID_QUANTITY");
        }

        asset.setStatus("DISPENSED");
        stub.putStringState(batchNumber, genson.serialize(asset));

        // Emit event
        DispenseEvent event = new DispenseEvent();
        event.batchNumber = batchNumber;
        event.pharmacyUuid = pharmacyUuid;
        event.patientId = patientId;
        event.quantity = quantity;
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();
        event.invokedByMsp = ctx.getClientIdentity().getMSPID();

        stub.setEvent("DrugDispensed", genson.serialize(event).getBytes());

        return asset;
    }

    /**
     * Record a recall.
     * Aligned with product_recalls table.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth RecallBatch(final Context ctx,
                                 final String batchNumber,
                                 final String productId,
                                 final String regulatorUuid,
                                 final String recallReason,
                                 final String severityLevel) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        asset.setStatus("RECALLED");
        stub.putStringState(batchNumber, genson.serialize(asset));

        // Emit event
        RecallEvent event = new RecallEvent();
        event.batchNumber = batchNumber;
        event.productId = productId;
        event.recallReason = recallReason;
        event.severityLevel = severityLevel;
        event.recalledByUuid = regulatorUuid;
        event.txId = stub.getTxId();
        event.timestamp = Instant.ofEpochMilli(stub.getTxTimestamp().toEpochMilli()).toString();
        event.invokedByMsp = ctx.getClientIdentity().getMSPID();

        stub.setEvent("DrugRecalled", genson.serialize(event).getBytes());

        return asset;
    }

    /**
     * Generic status update.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth UpdateStatus(final Context ctx,
                                  final String batchNumber,
                                  final String newStatus,
                                  final String updatedByUuid) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        asset.setStatus(newStatus);
        stub.putStringState(batchNumber, genson.serialize(asset));

        return asset;
    }

    // ==========================================================
    // QUERY FUNCTIONS
    // ==========================================================

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public DrugTruth ReadAsset(final Context ctx, final String batchNumber) {
        String assetJSON = ctx.getStub().getStringState(batchNumber);
        if (assetJSON == null || assetJSON.isEmpty()) {
            throw new ChaincodeException("Asset " + batchNumber + " does not exist", "ASSET_NOT_FOUND");
        }
        return genson.deserialize(assetJSON, DrugTruth.class);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean AssetExists(final Context ctx, final String batchNumber) {
        String assetJSON = ctx.getStub().getStringState(batchNumber);
        return (assetJSON != null && !assetJSON.isEmpty());
    }

    /**
     * Query by QR hash (for patient verification).
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public DrugTruth ReadAssetByQRHash(final Context ctx, final String qrHash) {
        // This requires a CouchDB index in production
        // For now, this is a placeholder - backend should query Postgres directly
        throw new ChaincodeException("Query by QR hash should be done via backend/Postgres", "USE_BACKEND_QUERY");
    }

    /**
     * Get complete transaction history.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAssetHistory(final Context ctx, final String batchNumber) {
        ChaincodeStub stub = ctx.getStub();

        var historyIterator = stub.getHistoryForKey(batchNumber);

        StringBuilder history = new StringBuilder("[");
        boolean first = true;

        while (historyIterator.hasNext()) {
            var modification = historyIterator.next();

            if (!first) {
                history.append(",");
            }
            first = false;

            history.append("{");
            history.append("\"txId\":\"").append(modification.getTxId()).append("\",");
            history.append("\"value\":").append(modification.getStringValue()).append(",");
            history.append("\"timestamp\":\"").append(
                    Instant.ofEpochSecond(
                            modification.getTimestamp().getSeconds(),
                            modification.getTimestamp().getNanos()
                    ).toString()
            ).append("\",");
            history.append("\"isDelete\":").append(modification.isDeleted());
            history.append("}");
        }

        history.append("]");
        historyIterator.close();

        return history.toString();
    }
}