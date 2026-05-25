	package main

	import (
		"encoding/json"
		"fmt"
		"log"

		"github.com/hyperledger/fabric-contract-api-go/contractapi"
	)

	// PharmaIntegrityContract implements the smart contract
	type PharmaIntegrityContract struct {
		contractapi.Contract
	}

	// --- Data Models (Strictly aligned with Spring Boot EventProcessor) ---

	// AssetData perfectly replaces the obsolete DrugTruth model
	type AssetData struct {
		DocType           string `json:"docType"`
		QRHash            string `json:"qrHash"`
		BatchNumber       string `json:"batchNumber"`
		ProductId         string `json:"productId"`
		ProductName       string `json:"productName"`
		ManufacturerId    string `json:"manufacturerId"`
		ExpiryDate        string `json:"expiryDate"`
		CurrentStatus     string `json:"currentStatus"`
		RequiresColdChain bool   `json:"requiresColdChain"`
		ApprovedByZamra   bool   `json:"approvedByZamra"`
	}

	type CustodyEventPayload struct {
		DocType           string `json:"docType"`
		QRHash            string `json:"qrHash"`
		FromParticipantId string `json:"fromParticipantId"`
		ToParticipantId   string `json:"toParticipantId"`
		EventType         string `json:"eventType"`
		Quantity          int    `json:"quantity"`
		TimestampNanos    string `json:"timestampNanos"`
		TxId              string `json:"txId"`
	}

	// --- Transactions ---

	// CreateAsset takes a single JSON string payload to bypass FireFly Map/Object mapping complexities
	func (c *PharmaIntegrityContract) CreateAsset(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
		var input map[string]interface{}
		if err := json.Unmarshal([]byte(payloadJSON), &input); err != nil {
			return fmt.Errorf("failed to parse input JSON: %v", err)
		}

		qrHash, ok := input["qrHash"].(string)
		if !ok || len(qrHash) != 64 {
			return fmt.Errorf("qrHash must be exactly 64 characters")
		}

		// Use LevelDB/SQLite compatible Composite Keys
		batchKey, _ := ctx.GetStub().CreateCompositeKey("BATCH", []string{qrHash})
		exists, _ := ctx.GetStub().GetState(batchKey)
		if exists != nil {
			return fmt.Errorf("asset already exists for qrHash: %s", qrHash)
		}

		// Safely map booleans which might be missing in older payloads
		requiresColdChain := false
		if val, ok := input["requiresColdChain"].(bool); ok {
			requiresColdChain = val
		}
		approvedByZamra := false
		if val, ok := input["approvedByZamra"].(bool); ok {
			approvedByZamra = val
		}

		asset := AssetData{
			DocType:           "BATCH",
			QRHash:            qrHash,
			BatchNumber:       input["batchNumber"].(string),
			ProductId:         input["productId"].(string),
			ProductName:       input["productName"].(string),
			ManufacturerId:    input["manufacturerId"].(string),
			ExpiryDate:        input["expiryDate"].(string),
			CurrentStatus:     "ON_CHAIN",
			RequiresColdChain: requiresColdChain,
			ApprovedByZamra:   approvedByZamra,
		}

		assetBytes, _ := json.Marshal(asset)
		ctx.GetStub().PutState(batchKey, assetBytes)

		// Emits exact JSON structure required by FireflyWebhookController
		ctx.GetStub().SetEvent("AssetCreated", assetBytes)
		return nil
	}

	// TransferCustody utilizes deterministic timestamps and updates the terminal lifecycle status
	func (c *PharmaIntegrityContract) TransferCustody(ctx contractapi.TransactionContextInterface, qrHash, fromId, toId, eventType string, quantity int) error {
		if len(qrHash) != 64 {
			return fmt.Errorf("invalid qrHash")
		}

		ts, _ := ctx.GetStub().GetTxTimestamp()
		timestampNanos := fmt.Sprintf("%d%09d", ts.Seconds, ts.Nanos)
		txId := ctx.GetStub().GetTxID()

		// Update main batch status if terminal event (DISPENSED or DESTROYED)
		if eventType == "DISPENSED" || eventType == "DESTROYED" {
			batchKey, _ := ctx.GetStub().CreateCompositeKey("BATCH", []string{qrHash})
			batchBytes, _ := ctx.GetStub().GetState(batchKey)
			if batchBytes != nil {
				var asset AssetData
				json.Unmarshal(batchBytes, &asset)
				asset.CurrentStatus = eventType
				updatedAsset, _ := json.Marshal(asset)
				ctx.GetStub().PutState(batchKey, updatedAsset)
			}
		}

		custody := CustodyEventPayload{
			DocType:           "CUSTODY",
			QRHash:            qrHash,
			FromParticipantId: fromId,
			ToParticipantId:   toId,
			EventType:         eventType,
			Quantity:          quantity,
			TimestampNanos:    timestampNanos,
			TxId:              txId,
		}

		custodyKey, _ := ctx.GetStub().CreateCompositeKey("CUSTODY", []string{qrHash, timestampNanos})
		custodyBytes, _ := json.Marshal(custody)

		ctx.GetStub().PutState(custodyKey, custodyBytes)
		ctx.GetStub().SetEvent("CustodyTransferred", custodyBytes)
		return nil
	}

	// GetAssetHistory natively queries LevelDB partial composite keys without requiring CouchDB
	func (c *PharmaIntegrityContract) GetAssetHistory(ctx contractapi.TransactionContextInterface, qrHash string) ([]string, error) {
		var history []string

		// 1. Retrieve current batch state
		batchKey, _ := ctx.GetStub().CreateCompositeKey("BATCH", []string{qrHash})
		batchBytes, _ := ctx.GetStub().GetState(batchKey)
		if batchBytes != nil {
			history = append(history, string(batchBytes))
		}

		// 2. Retrieve custody trail
		iterator, err := ctx.GetStub().GetStateByPartialCompositeKey("CUSTODY", []string{qrHash})
		if err == nil {
			defer iterator.Close()
			for iterator.HasNext() {
				response, _ := iterator.Next()
				history = append(history, string(response.Value))
			}
		}

		return history, nil
	}

	// --- Main Execution ---

	func main() {
		pharmaChaincode, err := contractapi.NewChaincode(&PharmaIntegrityContract{})
		if err != nil {
			log.Panicf("Error creating Pharma Integrity chaincode: %v", err)
		}

		if err := pharmaChaincode.Start(); err != nil {
			log.Panicf("Error starting Pharma Integrity chaincode: %v", err)
		}
	}
