# Distributed Notification System

A high-performance, load-balanced, real-time distributed notification system built with **Spring Boot 4**, **Java 25**, **Valkey (Redis-compatible Pub/Sub)**, **WebSockets**, and **Nginx**.

---

## 🏗 Architecture & Overview

```
                        +----------------------+
                        |   Nginx Load Balancer| (Port 8080)
                        +----------+-----------+
                                   |
           +-----------------------+-----------------------+
           |                       |                       |
           v                       v                       v
    +--------------+        +--------------+        +--------------+
    | App Server 1 |        | App Server 2 |        | App Server 3 |
    +-------+------+        +-------+------+        +-------+------+
            |                       |                       |
            +-----------------------+-----------------------+
                                    |
                                    v
                         +--------------------+
                         |  Valkey (Pub/Sub)  | (Port 6379)
                         +--------------------+
```

### Key Features
* **WebSocket Endpoint (`/ws`):**
  * When a client connects via WebSocket, the server generates a unique `clientId` (UUID) and sends an initial JSON greeting: `{"type": "CONNECTED", "clientId": "<UUID>"}`.
  * Each server instance subscribes to Valkey Pub/Sub events for active clients.
* **HTTP POST Endpoint (`POST /notification/{clientId}`):**
  * Publishes notification messages to Valkey Pub/Sub channel `notification:<clientId>`.
  * The server instance holding the active WebSocket connection receives the Valkey event and immediately pushes the payload to the client over WebSocket.
* **Horizontal Scalability:**
  * Multiple Spring Boot application nodes run behind an Nginx load balancer.
  * Valkey Pub/Sub ensures seamless cross-node communication regardless of which server instance receives the HTTP POST request.

---

## 🛠 Tech Stack
* **Java 25**
* **Spring Boot 4.0.0** (Spring Web, Spring WebSocket, Spring Data Redis)
* **Valkey 8.0** (In-Memory Data Store / PubSub)
* **Nginx** (Reverse Proxy & Load Balancer with WebSocket upgrade handling)
* **Docker & Docker Compose**

---

## 🚀 Getting Started

### Prerequisites
* Docker / Podman with Compose plugin

### Start the Cluster
To build and start all containers (1 Valkey instance, 3 Spring Boot app servers, 1 Nginx load balancer):

```bash
docker compose up --build -d
```

Check status of all running containers:
```bash
docker compose ps
```

Stop the cluster:
```bash
docker compose down
```

---

## 🧪 Integration Testing

Integration test scripts using native Node.js (`WebSocket` + `fetch`) are included in the repository:

### 1. Single Client Test
Tests establishing a WebSocket connection, extracting `clientId`, posting a notification payload to `POST /notification/{clientId}`, and receiving the real-time message back:

```bash
node test_single_client.js
```

### 2. Multi-Client Concurrent Test
Simulates 10 concurrent WebSocket clients connecting to the load balancer across multiple app nodes and receiving targeted notifications:

```bash
node test_multi_client.js
```

---

## 📜 Endpoints Reference

| Protocol | Endpoint | Description |
| :--- | :--- | :--- |
| **WebSocket** | `ws://localhost:8080/ws` | Establishes WebSocket connection. Returns initial `{"type": "CONNECTED", "clientId": "<UUID>"}` message. |
| **HTTP POST** | `http://localhost:8080/notification/{clientId}` | Accepts string or JSON payload in body and broadcasts notification via Valkey Pub/Sub. |
