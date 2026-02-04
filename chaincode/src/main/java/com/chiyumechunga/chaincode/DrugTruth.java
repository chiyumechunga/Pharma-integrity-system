package com.chiyumechunga.chaincode;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

/**
 * Represents a Pharmaceutical Batch on the Ledger.
 * Acts as the immutable "Source of Truth" linking physical goods to off-chain data.
 */
@DataType()
public class DrugTruth {

    @Property()
    private final String batchId;

    @Property()
    private final String qrHash; // SHA-256 hash of physical label data. Integrity Check.

    @Property()
    private String currentOwner; // Fabric MSP ID (e.g., "ManufacturerMSP") for Chain Security.

    @Property()
    private String currentOwnerUuid; // Postgres UUID (e.g., "550e84...") for DB Sync.

    @Property()
    private String status; // Lifecycle state: MANUFACTURED, IN_TRANSIT, DISPENSED, RECALLED.

    @Property()
    private final String expiryDate; // YYYY-MM-DD. Required for safety checks.

    // --- Constructors ---

    public DrugTruth() {
        this.batchId = "";
        this.qrHash = "";
        this.currentOwner = "";
        this.currentOwnerUuid = "";
        this.status = "";
        this.expiryDate = "";
    }

    public DrugTruth(@JsonProperty("batchId") String batchId,
                     @JsonProperty("qrHash") String qrHash,
                     @JsonProperty("currentOwner") String currentOwner,
                     @JsonProperty("currentOwnerUuid") String currentOwnerUuid,
                     @JsonProperty("status") String status,
                     @JsonProperty("expiryDate") String expiryDate) {
        this.batchId = batchId;
        this.qrHash = qrHash;
        this.currentOwner = currentOwner;
        this.currentOwnerUuid = currentOwnerUuid;
        this.status = status;
        this.expiryDate = expiryDate;
    }

    // --- Getters & Setters ---

    public String getBatchId() { return batchId; }
    public String getQrHash() { return qrHash; }
    public String getCurrentOwner() { return currentOwner; }
    public String getCurrentOwnerUuid() { return currentOwnerUuid; }
    public String getStatus() { return status; }
    public String getExpiryDate() { return expiryDate; }

    public void setCurrentOwner(String currentOwner) { this.currentOwner = currentOwner; }
    public void setCurrentOwnerUuid(String currentOwnerUuid) { this.currentOwnerUuid = currentOwnerUuid; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrugTruth drugTruth = (DrugTruth) o;
        return Objects.equals(batchId, drugTruth.batchId) &&
                Objects.equals(qrHash, drugTruth.qrHash) &&
                Objects.equals(currentOwner, drugTruth.currentOwner) &&
                Objects.equals(currentOwnerUuid, drugTruth.currentOwnerUuid) &&
                Objects.equals(status, drugTruth.status) &&
                Objects.equals(expiryDate, drugTruth.expiryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, qrHash, currentOwner, currentOwnerUuid, status, expiryDate);
    }
}