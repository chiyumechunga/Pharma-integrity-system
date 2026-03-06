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
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

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
    // EVENTS (Strict camelCase to match Spring Boot DTOs)
    // ==========================================================

    static class ManufactureEvent {
        @JsonProperty("batchNumber") public String batchNumber;
        @JsonProperty("productId") public String productId;
        @JsonProperty("productName") public String productName;
        @JsonProperty("manufacturerId") public String manufacturerId;
        @JsonProperty("manufacturingDate") public String manufacturingDate;
        @JsonProperty("expiryDate") public String expiryDate;
    }

    static class QRHashAttachedEvent {
        @JsonProperty("batchNumber") public String batchNumber;
        @JsonProperty("qrHash") public String qrHash;
        @JsonProperty("registryId") public String registryId;
    }

    static class CustodyEvent {
        @JsonProperty("batchNumber") public String batchNumber;
        @JsonProperty("fromParticipantId") public String fromParticipantId;
        @JsonProperty("toParticipantId") public String toParticipantId;
        @JsonProperty("eventType") public String eventType;
        @JsonProperty("quantity") public int quantity;
    }

    static class ScrutinyEvent {
        @JsonProperty("batchNumber") public String batchNumber;
        @JsonProperty("inspectorId") public String inspectorId;
        @JsonProperty("testResult") public String testResult;
        @JsonProperty("labNotes") public String labNotes;
    }

    static class RecallEvent {
        @JsonProperty("batchNumber") public String batchNumber;
        @JsonProperty("recallReason") public String recallReason;
        @JsonProperty("recalledByUuid") public String recalledByUuid;
    }

    static class DispenseEvent {
        @JsonProperty("batchNumber") public String batchNumber;
        @JsonProperty("pharmacyId") public String pharmacyId;
        @JsonProperty("patientId") public String patientId;
        @JsonProperty("quantity") public int quantity;
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

        // Status is REGISTERED on-chain. (PENDING_BLOCKCHAIN is strictly a backend Postgres concept).
        DrugTruth asset = new DrugTruth(
                batchNumber, productName, manufacturerId, expiryDate,
                "", manufacturerMspId, manufacturerId, "REGISTERED"
        );

        stub.putStringState(batchNumber, genson.serialize(asset));

        ManufactureEvent event = new ManufactureEvent();
        event.batchNumber = batchNumber;
        event.productId = productId;
        event.productName = productName;
        event.manufacturerId = manufacturerId;
        event.manufacturingDate = manufacturingDate;
        event.expiryDate = expiryDate;

        stub.setEvent("AssetCreated", genson.serialize(event).getBytes());

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

        stub.setEvent("QRHashAttached", genson.serialize(event).getBytes());

        return asset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth TransferCustody(final Context ctx,
                                     final String batchNumber,
                                     final String fromParticipantId,
                                     final String toParticipantId,
                                     final String toParticipantMspId,
                                     final String eventType,
                                     final int quantity) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        if (fromParticipantId == null || toParticipantId == null) {
            throw new ChaincodeException("Participant UUIDs cannot be null", "INVALID_INPUT");
        }
        if (quantity <= 0) {
            throw new ChaincodeException("Quantity must be positive", "INVALID_QUANTITY");
        }

        asset.setCurrentOwner(toParticipantMspId);
        asset.setCurrentOwnerUuid(toParticipantId);
        asset.setStatus("IN_TRANSIT_" + eventType);

        stub.putStringState(batchNumber, genson.serialize(asset));

        CustodyEvent event = new CustodyEvent();
        event.batchNumber = batchNumber;
        event.fromParticipantId = fromParticipantId;
        event.toParticipantId = toParticipantId;
        event.eventType = eventType;
        event.quantity = quantity;

        stub.setEvent("CustodyTransferred", genson.serialize(event).getBytes());

        return asset;
    }

    // Renamed from RecordScrutiny to match Spring Boot's invocation
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth SubmitTestResult(final Context ctx,
                                      final String batchNumber,
                                      final String inspectorId,
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
        event.inspectorId = inspectorId;

        stub.setEvent("TestResultSubmitted", genson.serialize(event).getBytes());

        return asset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth RecallBatch(final Context ctx,
                                 final String batchNumber,
                                 final String regulatorUuid,
                                 final String recallReason) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        if ("DISPENSED".equals(asset.getStatus())) {
            throw new ChaincodeException("Cannot recall a batch that has already been fully dispensed", "INVALID_STATE");
        }

        asset.setStatus("RECALLED");
        stub.putStringState(batchNumber, genson.serialize(asset));

        RecallEvent event = new RecallEvent();
        event.batchNumber = batchNumber;
        event.recalledByUuid = regulatorUuid;
        event.recallReason = recallReason;

        stub.setEvent("DrugRecalled", genson.serialize(event).getBytes());

        return asset;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DrugTruth DispenseBatch(final Context ctx,
                                   final String batchNumber,
                                   final String pharmacyId,
                                   final String patientId,
                                   final int quantity) {

        ChaincodeStub stub = ctx.getStub();
        DrugTruth asset = ReadAsset(ctx, batchNumber);

        if (pharmacyId == null || pharmacyId.isEmpty()) {
            throw new ChaincodeException("Pharmacy UUID required", "INVALID_INPUT");
        }
        if ("RECALLED".equals(asset.getStatus())) {
            throw new ChaincodeException("Cannot dispense a recalled batch", "ASSET_RECALLED");
        }

        asset.setStatus("DISPENSED");
        asset.setCurrentOwnerUuid(patientId);

        stub.putStringState(batchNumber, genson.serialize(asset));

        DispenseEvent event = new DispenseEvent();
        event.batchNumber = batchNumber;
        event.pharmacyId = pharmacyId;
        event.patientId = patientId;
        event.quantity = quantity;

        stub.setEvent("DrugDispensed", genson.serialize(event).getBytes());

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