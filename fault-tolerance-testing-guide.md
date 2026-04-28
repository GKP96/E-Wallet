# E-Wallet Fault Tolerance Testing Guide

To prove that your E-Wallet architecture is completely fault-tolerant and resilient, you can execute the following 5 edge-case tests. These tests simulate catastrophic infrastructure failures and verify that the system recovers gracefully without data loss or duplicate processing.

---

### 1. Test The Redis Cache Fallback (Microservice Down)
**The Goal:** Prove that a transaction succeeds even if the upstream `user-service` is completely offline.

**How to test:**
1. Start all services and your Redis server.
2. Create a User via `POST http://localhost:9000/user`. This saves the user in MySQL *and* caches their email in Redis.
3. **Kill the `user-service` terminal.** The service is now completely offline.
4. Send a `POST` to `http://localhost:8080/txn` to transfer money.
5. **What to watch for:** Watch the `transaction-service` terminal. It will hang for about 6 seconds as Spring Retry attempts the HTTP call 3 times (with 2-second delays). Then, the `@Recover` method triggers, printing: `HTTP to user-service failed. Falling back to Redis cache...`. The transaction will finish perfectly, and you will still receive the notification email!

---

### 2. Test the Outbox Pattern (Kafka Down)
**The Goal:** Prove that no data is lost if the message broker (Kafka) crashes.

**How to test:**
1. Stop your Kafka server (kill the terminal running the Kafka Broker).
2. Send a `POST http://localhost:8080/txn` request. 
3. **What to watch for:** You will get a `200 OK` response instantly. The API succeeds because it only writes to the local MySQL DB and the Outbox table. 
4. Now, wait 1 minute. Turn the Kafka Broker back on.
5. **What to watch for:** Watch the `transaction-service` terminal. The moment Kafka boots up, your `@Scheduled` outbox publisher will automatically connect, find the unsent event in the outbox table, and blast it out to Kafka to continue the Saga.

---

### 3. Test the Dead Letter Topic (Database Down)
**The Goal:** Prove that a broken database doesn't create a "Poison Pill" that freezes the Kafka queue.

**How to test:**
1. Keep all services running normally.
2. Go into your MySQL Workbench and temporarily rename the `wallet` table to `wallet_broken`.
3. Send a `POST http://localhost:8080/txn` request.
4. **What to watch for:** Watch the `wallet-service` terminal. It will pull the message, try to query the DB, and throw a SQL Exception. Because of the `FixedBackOff` configuration, it will pause for 1 second, retry, and crash again. After 3 consecutive crashes, it will route the message to the Dead Letter Topic (`txn_create.DLT`). The system survives and is ready for the next healthy transaction. *(Don't forget to rename the table back!).*

---

### 4. Test Idempotency (The "Double Click" Bug)
**The Goal:** Prove that a user is never charged twice if they click the "Pay" button multiple times.

**How to test:**
1. Copy your JSON payload in Postman. Make sure `idempotencyKey` is set to a unique string (e.g., `"duplicate-test-1"`).
2. Click Send. The transaction processes and deducts money normally.
3. Click Send again with the **exact same JSON payload**.
4. **What to watch for:** The `transaction-service` will instantly return the existing transaction ID. It will not create a new Outbox event, Kafka will not receive a second message, and the user's wallet balance will not be deducted a second time.

---

### 5. Test Saga Rollbacks (Bad Business Logic)
**The Goal:** Prove that the system handles invalid data gracefully without getting stuck.

**How to test:**
1. Send a `POST http://localhost:8080/txn` request, but set the `receiverUserId` to `999999` (an ID that does not exist in the database).
2. **What to watch for:** The transaction will initially save as `PENDING`. However, when `wallet-service` processes the Kafka message, it will realize the receiver doesn't exist. Instead of crashing, it will actively publish a `FAILED` event back to Kafka. `transaction-service` will consume it and update the MySQL transaction status to `FAILED`. You will then receive an email explicitly stating your transaction failed.
