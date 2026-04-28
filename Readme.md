# E-Wallet Distributed Microservices System

## 📖 Overview
The E-Wallet platform is a highly scalable, fault-tolerant backend system designed to handle financial transactions securely. Built using a distributed microservices architecture, it breaks down traditional monolithic processes into four independent domains communicating asynchronously. 

Rather than relying on basic CRUD operations, this project prioritizes **data consistency, high availability, and messaging guarantees** under network failure conditions, simulating enterprise-grade financial systems.

## 🏗️ Architecture & Core Patterns
This system is designed to survive database crashes, network partitions, and message broker downtime. It implements several advanced distributed system patterns:

* **Event-Driven Saga Pattern (Choreography):** Manages distributed transactions across multiple independent databases without relying on slow Two-Phase Commits (2PC).
* **Transactional Outbox Pattern:** Solves the "dual-write" problem. Ensures that saving data to MySQL and publishing events to Apache Kafka happen atomically, guaranteeing eventual consistency.
* **Idempotent Consumers:** Employs a unique MySQL mapping table to ensure that if Kafka accidentally redelivers a network-delayed message, users are mathematically guaranteed to never be charged twice.
* **Cache-Aside & Graceful Degradation:** Integrates Redis to instantly cache user profiles. If the User Service experiences a critical outage, the Transaction Service gracefully falls back to Redis to keep global transactions flowing.
* **Dead Letter Topics (DLT):** Prevents "Poison Pill" messages from freezing Kafka queues during temporary database outages by safely routing failing messages to a retry/analysis queue.

## 🛠️ Tech Stack
* **Language/Framework:** Java 8+, Spring Boot 2.6.x
* **Message Broker:** Apache Kafka (Spring Kafka)
* **Databases/Caching:** MySQL, Redis (Spring Data Redis)
* **Build Tool:** Maven (Multi-Module Monorepo Structure)
* **Other:** Spring Data JPA, Lombok

## 📂 Project Structure
The project is structured as a Maven Multi-Module workspace:
* `user-service/`: Manages user registration and profiles. Automatically caches to Redis.
* `wallet-service/`: Maintains user balances. Enforces idempotency and executes actual fund transfers.
* `transaction-service/`: Orchestrates the Saga. Tracks the global state of a transaction (PENDING, SUCCESSFUL, FAILED).
* `notification-service/`: Listens for completed transactions and dispatches confirmation emails via SMTP.

## 📚 Deep Dive Documentation
To truly understand the internal mechanics and fault-tolerance of this system, please review the included documentation:
1. [Solution Design & Saga Flow](solution-design.md) - Contains Mermaid architecture diagrams and step-by-step transaction flows.
2. [Kafka Internals Flow](kafka-internals-flow.md) - Deep dive into how bytes move through producers, partitions, and consumers.
3. [Fault Tolerance Testing Guide](fault-tolerance-testing-guide.md) - Step-by-step instructions on how to intentionally crash databases and observe the system's survival mechanisms.