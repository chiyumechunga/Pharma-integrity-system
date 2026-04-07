package com.chiyumechunga.chaincode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Contract(name = "PharmaIntegrityContract")
@Default
public class PharmaIntegrity implements ContractInterface {

    private final Gson gson = new Gson();

    // CustodyEventPayload remains internal because it is purely an event payload, not a ledger asset
    private static class CustodyEventPayload {
        String docType = "CUSTODY";
        String qrHash;
        String fromParticipantId;
        String toParticipantId;
        String eventType;
        int quantity;
        String timestampNanos;
        String txId;
    }

    private String getDeterministicTimestamp(Context ctx) {
        java.time.Instant ts = ctx.getStub().getTxTimestamp();
        return String.format("%d%09d", ts.getEpochSecond(), ts.getNano());
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void CreateAsset(Context ctx, String payloadJSON) {
        JsonObject input = gson.fromJson(payloadJSON, JsonObject.class);
        String qrHash = input.get("qrHash").getAsString();

        if (qrHash == null || qrHash.length() != 64) {
            throw new RuntimeException("qrHash must be exactly 64 hex characters");
        }

        CompositeKey batchKey = ctx.getStub().createCompositeKey("BATCH", qrHash);
        byte[] existing = ctx.getStub().getState(batchKey.toString());
        if (existing != null && existing.length > 0) {
            throw new RuntimeException("Asset already exists for qrHash: " + qrHash);
        }

        // USING YOUR NEW DrugTruth CLASS
        DrugTruth asset = new DrugTruth();
        asset.setQrHash(qrHash);
        asset.setBatchNumber(input.get("batchNumber").getAsString());
        asset.setProductId(input.get("productId").getAsString());
        asset.setProductName(input.get("productName").getAsString());
        asset.setManufacturerId(input.get("manufacturerId").getAsString());
        asset.setExpiryDate(input.get("expiryDate").getAsString());
        asset.setCurrentStatus("ON_CHAIN");
        asset.setRequiresColdChain(input.has("requiresColdChain") && input.get("requiresColdChain").getAsBoolean());
        asset.setApprovedByZamra(input.has("approvedByZamra") && input.get("approvedByZamra").getAsBoolean());

        String jsonOut = gson.toJson(asset);
        ctx.getStub().putState(batchKey.toString(), jsonOut.getBytes(StandardCharsets.UTF_8));

        // Emits perfectly formatted JSON so `event.output().data()` won't hit NullPointers
        ctx.getStub().setEvent("AssetCreated", jsonOut.getBytes(StandardCharsets.UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void TransferCustody(Context ctx, String qrHash, String fromId, String toId, String eventType, int quantity) {
        String timestampNanos = getDeterministicTimestamp(ctx);
        String txId = ctx.getStub().getTxId();

        // Terminal Update
        if ("DISPENSED".equals(eventType) || "DESTROYED".equals(eventType)) {
            CompositeKey batchKey = ctx.getStub().createCompositeKey("BATCH", qrHash);
            byte[] batchBytes = ctx.getStub().getState(batchKey.toString());
            if (batchBytes != null && batchBytes.length > 0) {
                // DESERIALIZE BACK INTO YOUR DrugTruth CLASS
                DrugTruth asset = gson.fromJson(new String(batchBytes, StandardCharsets.UTF_8), DrugTruth.class);
                asset.setCurrentStatus(eventType);
                ctx.getStub().putState(batchKey.toString(), gson.toJson(asset).getBytes(StandardCharsets.UTF_8));
            }
        }

        CustodyEventPayload custody = new CustodyEventPayload();
        custody.qrHash = qrHash;
        custody.fromParticipantId = fromId;
        custody.toParticipantId = toId;
        custody.eventType = eventType;
        custody.quantity = quantity;
        custody.timestampNanos = timestampNanos;
        custody.txId = txId;

        CompositeKey custodyKey = ctx.getStub().createCompositeKey("CUSTODY", qrHash, timestampNanos);
        String jsonOut = gson.toJson(custody);

        ctx.getStub().putState(custodyKey.toString(), jsonOut.getBytes(StandardCharsets.UTF_8));
        ctx.getStub().setEvent("CustodyTransferred", jsonOut.getBytes(StandardCharsets.UTF_8));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAssetHistory(Context ctx, String qrHash) {
        List<String> history = new ArrayList<>();

        CompositeKey batchKey = ctx.getStub().createCompositeKey("BATCH", qrHash);
        byte[] batchBytes = ctx.getStub().getState(batchKey.toString());
        if (batchBytes != null && batchBytes.length > 0) {
            history.add(new String(batchBytes, StandardCharsets.UTF_8));
        }

        QueryResultsIterator<KeyValue> iterator = ctx.getStub().getStateByPartialCompositeKey("CUSTODY", qrHash);
        for (KeyValue kv : iterator) {
            history.add(kv.getStringValue());
        }

        return history.toString();
    }
}