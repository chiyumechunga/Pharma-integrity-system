package com.chiyumechunga.chaincode;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

@DataType()
public class DrugTruth {

    @Property()
    private final String batchId;

    @Property()
    private final String qrHash;

    @Property()
    private String currentOwner;

    @Property()
    private String status;

    @Property()
    private final String expiryDate; // NEW: Required for on-chain validation

    public DrugTruth() {
        this.batchId = "";
        this.qrHash = "";
        this.currentOwner = "";
        this.status = "";
        this.expiryDate = "";
    }

    public DrugTruth(@JsonProperty("batchId") String batchId,
                     @JsonProperty("qrHash") String qrHash,
                     @JsonProperty("currentOwner") String currentOwner,
                     @JsonProperty("status") String status,
                     @JsonProperty("expiryDate") String expiryDate) {
        this.batchId = batchId;
        this.qrHash = qrHash;
        this.currentOwner = currentOwner;
        this.status = status;
        this.expiryDate = expiryDate;
    }

    // Getters
    public String getBatchId() { return batchId; }
    public String getQrHash() { return qrHash; }
    public String getCurrentOwner() { return currentOwner; }
    public String getStatus() { return status; }
    public String getExpiryDate() { return expiryDate; }

    // Setters
    public void setCurrentOwner(String currentOwner) { this.currentOwner = currentOwner; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrugTruth drugTruth = (DrugTruth) o;
        return Objects.equals(batchId, drugTruth.batchId) &&
                Objects.equals(qrHash, drugTruth.qrHash) &&
                Objects.equals(currentOwner, drugTruth.currentOwner) &&
                Objects.equals(status, drugTruth.status) &&
                Objects.equals(expiryDate, drugTruth.expiryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, qrHash, currentOwner, status, expiryDate);
    }
}