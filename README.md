# Blockchain-Based Pharmaceutical Integrity System for Zambia.

## Abstract

This repository presents a blockchain-based pharmaceutical integrity system developed as an undergraduate capstone project at Mulungushi University. The system addresses critical vulnerabilities in pharmaceutical supply chains within developing nations, with specific focus on Zambia's healthcare infrastructure. By leveraging **Hyperledger FireFly** with **Hyperledger Fabric chaincode**, this implementation demonstrates a practical application of blockchain solutions to combat pharmaceutical counterfeiting and enhance supply chain transparency.

## Problem Statement

Pharmaceutical supply chains in developing nations face significant structural challenges that compromise public health outcomes and patient safety. The absence of robust tracking mechanisms enables counterfeit medications to infiltrate legitimate distribution channels, while limited transparency obscures the provenance and handling history of pharmaceutical products. Healthcare providers and end-users lack reliable mechanisms to verify product authenticity, creating substantial risks to treatment efficacy and public health.

This project addresses these challenges through **Hyperledger FireFly** and **Fabric chaincode** to establish an immutable, transparent pharmaceutical tracking infrastructure with event-driven synchronization.

## System Features

The platform provides comprehensive pharmaceutical supply chain management capabilities through blockchain technology:

- **Immutable Ledger**: Fabric securely records all transactions and movements of pharmaceutical products, creating a permanent audit trail
- **Track-and-Trace**: Stakeholders monitor product journeys from manufacturing through end-user delivery
- **End-User Verification**: Patients authenticate products through unique identifiers
- **Chaincode Enforcement**: Business rules and compliance automated at protocol level
- **Anti-Counterfeiting**: Real-time monitoring identifies suspicious distribution patterns

FireFly events drive dual writes to PostgreSQL for read-optimized queries.

## Technical Architecture

### Technology Stack
- **Blockchain:** Hyperledger Fabric Supernode (ff init fabric)
- **Middleware:** Hyperledger FireFly (REST APIs, Event Bus, FFI)
- **Backend:** Java Spring Boot (REST API)
- **Database:** PostgreSQL (off-chain)
- **Runtime:** Docker + Docker Compose
- **Frontend:** React.js

**Hyperledger Fabric Supernode** provides enterprise-grade permissions and confidential transactions. **FireFly** adds REST APIs, typed events, and dual-write orchestration.

### Core Components

**Chaincode Implementation** 
Chaincode governs pharmaceutical lifecycle: `RegisterBatch`, `TransferCustody`, `QualityCheckpoint`, `DispenseBatch`. Emits Fabric events consumed by FireFly.

**FireFly Middleware**
- REST APIs for chaincode invoke/query
- WebSocket event streaming
- FFI contract interfaces
- Event-driven dual writes 

**REST API Layer**
Spring Boot endpoints abstract FireFly operations for stakeholders.

**Database Infrastructure**
PostgreSQL stores user data, metadata, and event projections.

## Event-Driven Dual Write Architecture
API Request → FireFly REST → Fabric Chaincode → Event Emission → FireFly Event Bus → PostgreSQL Offchain Database.

**Flow:**
1. API submits transaction via FireFly REST endpoint
2. Fabric chaincode executes, emits domain event
3. firefly-fabconnect streams event to FireFly
4. Event processor updates PostgreSQL idempotently 

**Benefits:**
- No direct dual writes (avoids race conditions)
- Fabric = source of truth
- PostgreSQL = read-optimized projection
- Eventual consistency via sequenced events 

## Installation and Deployment

### Prerequisites
```bash
Docker Engine 20+
Docker Compose 2.0+
Hyperledger FireFly CLI
```
Quick Start with Fabric Supernode
```bash
ff init fabric capstone
ff start capstone
```

## Academic Context

Developed as partial fulfillment of undergraduate capstone requirements at **Mulungushi University, Department of Computer Science**. Demonstrates **FireFly Fabric Supernode** for real-world pharmaceutical supply chain challenges in resource-constrained environments.

## Project Status

**Proof-of-concept** for academic evaluation. Production requires:
- Security auditing
- Scalability optimization
- Regulatory compliance
- Healthcare system integration 

## Author Information

**Chiyume Chunga**
**Bachelor of Science in Information Technology**
**Mulungushi University, Zambia**

**Contact:** 
📧 chiyumechunga@gmail.com 
💼 [linkedin.com/in/chiyume-chunga](https://linkedin.com/in/chiyume-chunga) 

## Acknowledgments

- **Supervisor**: Dr. Sinyinda Muwanei, Ph.D.
- **Hyperledger FireFly & Fabric** communities
- **Mulungushi University**


## Future Research

- IoT integration
- Nationwide scalability analysis
- Healthcare system interoperability
- Economic impact assessment
- Stakeholder adoption barriers
- Regulatory frameworks
