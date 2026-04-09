package com.chiyumechunga.chaincode;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

/**
 * Represents a Pharmaceutical Batch on the Ledger.
 * Perfectly matches the payload from Spring Boot's RegistryServiceImpl.
 */
@DataType()
public class DrugTruth {

    @Property()
    private String docType = "BATCH";

    @Property()
    private String qrHash;

    @Property()
    private String batchNumber;

    @Property()
    private String productId;

    @Property()
    private String productName;

    @Property()
    private String manufacturerId;

    @Property()
    private String expiryDate;

    @Property()
    private String currentStatus;

    @Property()
    private boolean requiresColdChain;

    @Property()
    private boolean approvedByZamra;

    // --- Constructors ---

    public DrugTruth() {
    }

    public DrugTruth(String qrHash, String batchNumber, String productId, String productName,
                     String manufacturerId, String expiryDate, String currentStatus,
                     boolean requiresColdChain, boolean approvedByZamra) {
        this.qrHash = qrHash;
        this.batchNumber = batchNumber;
        this.productId = productId;
        this.productName = productName;
        this.manufacturerId = manufacturerId;
        this.expiryDate = expiryDate;
        this.currentStatus = currentStatus;
        this.requiresColdChain = requiresColdChain;
        this.approvedByZamra = approvedByZamra;
    }

    // --- Getters & Setters ---

    public String getDocType() { return docType; }
    public String getQrHash() { return qrHash; }
    public void setQrHash(String qrHash) { this.qrHash = qrHash; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getManufacturerId() { return manufacturerId; }
    public void setManufacturerId(String manufacturerId) { this.manufacturerId = manufacturerId; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public boolean isRequiresColdChain() { return requiresColdChain; }
    public void setRequiresColdChain(boolean requiresColdChain) { this.requiresColdChain = requiresColdChain; }

    public boolean isApprovedByZamra() { return approvedByZamra; }
    public void setApprovedByZamra(boolean approvedByZamra) { this.approvedByZamra = approvedByZamra; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrugTruth drugTruth = (DrugTruth) o;
        return requiresColdChain == drugTruth.requiresColdChain &&
                approvedByZamra == drugTruth.approvedByZamra &&
                Objects.equals(qrHash, drugTruth.qrHash) &&
                Objects.equals(batchNumber, drugTruth.batchNumber) &&
                Objects.equals(productId, drugTruth.productId) &&
                Objects.equals(productName, drugTruth.productName) &&
                Objects.equals(manufacturerId, drugTruth.manufacturerId) &&
                Objects.equals(expiryDate, drugTruth.expiryDate) &&
                Objects.equals(currentStatus, drugTruth.currentStatus);
    }


}