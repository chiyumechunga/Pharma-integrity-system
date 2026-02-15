package com.chiyumechunga.chaincode;

import com.owlike.genson.Genson;
import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification; // ADDED
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator; // ADDED

import java.time.Instant;

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
    // EVENTS
    // ==========================================================

    static class ManufactureEvent {
        @JsonProperty("batch_number") public String batchNumber;
        @JsonProperty("product_id") public String productId;
        @JsonProperty("product_name") public String productName;
        @JsonProperty("manufacturer_id") public String manufacturerId;
        @JsonProperty("manufacturing_date") public String manufacturingDate;
        @JsonProperty("expiry_date") public String expiryDate;
        @JsonProperty("tx_id") public String txId;
        @JsonProperty("invoked_by_msp") public String invokedByMsp;
    }

    static class QRHashAttachedEvent {
        @JsonProperty("batch_number") public String batchNumber;
        @JsonProperty("qr_hash") public String qrHash;
        @JsonProperty("registry_id") public String registryId;
        @JsonProperty("tx_id") public String txId;
    }

    static class CustodyEvent {
        @JsonProperty("batch_number") public String batchNumber;
        @JsonProperty("from_participant_uuid") public String fromParticipantUuid;
        @JsonProperty("to_participant_uuid") public String toParticipantUuid;
        @JsonProperty("event_type") public String eventType;
        @JsonProperty("quantity") public int quantity;
        @JsonProperty("tx_id") public String txId;
        @JsonProperty("timestamp") public String timestamp;
        @JsonProperty("invoked_by_msp") public String invokedByMsp;
    }

    static class ScrutinyEvent {
        @JsonProperty("batch_number") public String batchNumber;
        @JsonProperty("test_result") public String testResult;
        @JsonProperty("lab_notes") public String labNotes;
        @JsonProperty("inspector_uuid") public String inspectorUuid;
        @JsonProperty("tx_id") public String txId;
        @JsonProperty("timestamp") public String timestamp;
        @JsonProperty("invoked_by_msp") public String invokedByMsp;
    }

    static class RecallEvent {
        @JsonProperty("batch_number") public String batchNumber;
        @JsonProperty("product_id") public String productId;
        @JsonProperty("recall_reason") public String recallReason;
        @JsonProperty("severity_level") public String severityLevel;
        @JsonProperty("recalled_by_uuid") public String recalledByUuid;
        @JsonProperty("tx_id") public String txId;
        @JsonProperty("timestamp") public String timestamp;
        @JsonProperty("invoked_by_msp") public String invokedByMsp;
    }

    static class DispenseEvent {
        @JsonProperty("batch_number") public String batchNumber;
        @JsonProperty("pharmacy_uuid") public String pharmacyUuid;
        @JsonProperty("patient_id") public String patientId;
        @JsonProperty("quantity") public int quantity;
        @JsonProperty("tx_id") public String txId;
        @JsonProperty("timestamp") public String timestamp;
        @JsonProperty("invoked_by_msp") public String invokedByMsp;
    }

    // ==========================================================
    // TRANSACTIONS
    // ==========================================================

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

        if (AssetExists(ctx, batchNumber)) {
            throw new ChaincodeException("Asset " + batchNumber + " already exists", "ASSET_ALREADY_EXISTS");
        }
        if (batchNumber == null || batchNumber.isEmpty()) {
            throw new ChaincodeException("Batch number cannot be empty", "INVALID_INPUT");
        }

        DrugTruth asset = new DrugTruth(
                batchNumber, productName, manufacturerId, expiryDate,
                "", manufacturerMspId, manufacturerId, "PENDING_BLOCKCHAIN"
        );

        stub.putStringState(batchNumber, genson.serialize(asset));

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

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth AttachQRHash(final Context ctx,
                                  final String batchNumber,
                                  final String qrHash,
                                  final String registryId) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        if (qrHash == null || qrHash.isEmpty()) {
            throw new ChaincodeException("QR hash cannot be empty", "INVALID_INPUT");
        }
        if (!qrHash.matches("^[0-9a-fA-F]{64}$")) {
            throw new ChaincodeException("QR hash must be 64-char hex (SHA256)", "INVALID_HASH_FORMAT");
        }
        if (asset.getQrHash() != null && !asset.getQrHash().isEmpty()) {
            throw new ChaincodeException("Batch already has QR hash attached", "QR_ALREADY_EXISTS");
        }

        asset.setQrHash(qrHash);
        asset.setStatus("CONFIRMED");

        stub.putStringState(batchNumber, genson.serialize(asset));

        QRHashAttachedEvent event = new QRHashAttachedEvent();
        event.batchNumber = batchNumber;
        event.qrHash = qrHash;
        event.registryId = registryId;
        event.txId = stub.getTxId();

        stub.setEvent("QRHashAttached", genson.serialize(event).getBytes());

        return asset;
    }

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

        if (fromParticipantUuid == null || toParticipantUuid == null) {
            throw new ChaincodeException("Participant UUIDs cannot be null", "INVALID_INPUT");
        }
        if (quantity <= 0) {
            throw new ChaincodeException("Quantity must be positive", "INVALID_QUANTITY");
        }

        asset.setCurrentOwner(toParticipantMspId);
        asset.setCurrentOwnerUuid(toParticipantUuid);
        asset.setStatus("IN_TRANSIT_" + eventType);

        stub.putStringState(batchNumber, genson.serialize(asset));

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

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth RecordScrutiny(final Context ctx,
                                    final String batchNumber,
                                    final String inspectorUuid,
                                    final String testResult,
                                    final String labNotes) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        String statusPrefix = testResult.equals("PASSED") ? "QA_PASSED" :
                testResult.equals("FAILED") ? "QA_FAILED" : "QA_PENDING";

        asset.setStatus(statusPrefix + ": " + labNotes);

        stub.putStringState(batchNumber, genson.serialize(asset));

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

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public DrugTruth ReadAssetByQRHash(final Context ctx, final String qrHash) {
        throw new ChaincodeException("Query by QR hash should be done via backend/Postgres", "USE_BACKEND_QUERY");
    }

    /**
     * FIXED:
     * 1. Uses try-with-resources for Iterator
     * 2. Uses for-each loop for KeyModification
     * 3. Uses correct Timestamp handling
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAssetHistory(final Context ctx, final String batchNumber) {
        ChaincodeStub stub = ctx.getStub();

        try (QueryResultsIterator<KeyModification> historyIterator = stub.getHistoryForKey(batchNumber)) {

            StringBuilder history = new StringBuilder("[");
            boolean first = true;

            for (KeyModification modification : historyIterator) {
                if (!first) {
                    history.append(",");
                }
                first = false;

                history.append("{");
                history.append("\"txId\":\"").append(modification.getTxId()).append("\",");
                history.append("\"value\":").append(modification.getStringValue()).append(",");

                // FIX: modification.getTimestamp() returns Instant, just use toString() for ISO format
                history.append("\"timestamp\":\"").append(modification.getTimestamp().toString()).append("\",");

                history.append("\"isDelete\":").append(modification.isDeleted());
                history.append("}");
            }

            history.append("]");
            return history.toString();
        } catch (Exception e) {
            throw new ChaincodeException("Failed to get asset history: " + e.getMessage());
        }
    }
}