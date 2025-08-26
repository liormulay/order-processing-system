# Order Processing Microservices System

A distributed microservice system simulating an order processing workflow with asynchronous communication using Kafka, temporary data storage with Redis, and varying logic based on product types.

## 🚀 Quick Start

### Prerequisites
- **Docker and Docker Compose** (required)
- Java 17 (for local development only)
- Maven 3.9+ (for local development only)

### Running the System

1. **Clone and navigate to the project:**
   ```bash
   cd order-processing-system
   ```

2. **Start all services with Docker Compose:**
   ```bash
   docker-compose up -d
   ```

3. **Verify all services are running:**
   ```bash
   docker-compose ps
   ```

4. **Monitor service startup:**
   ```bash
   docker-compose logs -f
   ```
   Wait until all services show "Started" messages.

### 🔄 Event Flow and Communication

### Complete Order Processing Flow

1. **Order Creation (HTTP → Order Service)**
   ```
   Client POST /orders → Order Service
   ```
   - Order Service validates request
   - Generates unique orderId (format: ORD-{random})
   - Stores complete order data in Redis: `order:{orderId}`
   - Publishes event to Kafka topic `order-events` with orderId only

2. **Inventory Check (Kafka → Inventory Service)**
   ```
   Kafka order-events → Inventory Service
   ```
   - Inventory Service receives orderId from Kafka
   - Retrieves complete order data from Redis: `order:{orderId}`
   - Checks product availability based on category rules
   - Updates order status in Redis: `order:{orderId}` (APPROVED/REJECTED)
   - If rejected, stores missing items in Redis: `missingItems:{orderId}`
   - Publishes result to Kafka topic `inventory-check-results`

3. **Notification (Kafka → Notification Service)**
   ```
   Kafka inventory-check-results → Notification Service
   ```
   - Notification Service receives inventory check result
   - Retrieves original order data from Redis: `order:{orderId}`
   - Logs detailed confirmation or rejection message
   - If rejected, retrieves missing items from Redis: `missingItems:{orderId}`

### Sample Event Flow Timeline

```
Time 0ms:   Client sends POST /orders
Time 50ms:  Order Service stores in Redis, publishes to Kafka
Time 100ms: Inventory Service receives Kafka event
Time 150ms: Inventory Service checks inventory, updates Redis
Time 200ms: Inventory Service publishes result to Kafka
Time 250ms: Notification Service receives result, logs notification
```

## 🔄 Handling Code Changes

**To apply code changes, you must rebuild the images:**

```bash
# Option 1: Rebuild and start in one command
docker-compose up -d --build

# Option 2: Rebuild first, then start
docker-compose build
docker-compose up -d
```

**Why this happens:**
- Each service uses a multi-stage Dockerfile that builds the application during image creation
- Source code is copied into the container and compiled with Maven
- No volume mounts are used for source code, so local changes don't affect running containers
- Docker Compose uses existing images unless forced to rebuild

**For development with hot-reload:**
Consider setting up a development environment with volume mounts and Spring Boot DevTools for automatic restarts.

### Testing the System

Once all services are running, you can test the system:

**Create an order:**
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Alice",
    "items": [
      {
        "productId": "P1001",
        "quantity": 2,
        "category": "standard"
      }
    ],
    "requestedAt": "2025-06-30T14:00:00Z"
  }'
```

**Check order status:**
```bash
curl http://localhost:8081/orders/ORD-ABC12345
```

## 📋 System Components

| Service | Port | Description |
|---------|------|-------------|
| **Order Service** | 8081 | REST API for order creation and status retrieval |
| **Inventory Service** | 8082 | Product availability checking and validation |
| **Notification Service** | 8083 | Order confirmation and rejection notifications |
| **Kafka** | 9092 | Asynchronous messaging between services |
| **Redis** | 6379 | Temporary order data storage |
| **Zookeeper** | 2181 | Kafka coordination service |

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client        │    │   Order Service │    │    Kafka        │
│   (HTTP)        │───▶│   (Port 8081)   │───▶│  (Port 9092)    │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────┐         ┌─────────────────┐
                       │    Redis    │         │  Inventory      │
                       │  (Port 6379)│◀────────│  Service        │
                       │             │         │  (Port 8082)    │
                       └─────────────┘         └─────────────────┘
                              ▲                        │
                              │                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │  Notification   │    │    Kafka        │
                       │  Service        │◀───│  (Port 9092)    │
                       │  (Port 8083)    │    │                 │
                       └─────────────────┘    └─────────────────┘

```

**Flow:**
1. **Client** → **Order Service** (HTTP REST API)
2. **Order Service** → **Redis** (stores order data)
3. **Order Service** → **Kafka** (publishes order event)
4. **Kafka** → **Inventory Service** (consumes order event)
5. **Inventory Service** → **Redis** (retrieves order data, updates status)
6. **Inventory Service** → **Kafka** (publishes inventory check result)
7. **Kafka** → **Notification Service** (consumes inventory check result)
8. **Notification Service** → **Redis** (retrieves order data for notifications)

## 🔧 Service Responsibilities

### Order Service (Port 8081)
- Exposes REST API: `POST /orders` and `GET /orders/{orderId}`
- Receives order details from clients
- Stores complete order data in Redis with PENDING status
- Publishes order events to Kafka (containing only orderId)
- Provides order status retrieval endpoint by querying Redis

### Inventory Service (Port 8082)
- Listens to Kafka for incoming order events
- Retrieves complete order data from Redis using orderId
- Checks product availability based on category-specific rules
- Updates order status directly in Redis (APPROVED/REJECTED)
- Stores missing items in Redis if order is rejected
- Publishes inventory check results to Kafka for notifications
- Maintains in-memory product catalog with sample data

### Notification Service (Port 8083)
- Listens to inventory check results from Kafka
- Retrieves original order data from Redis using orderId
- Logs detailed confirmation or rejection messages with order details
- No direct communication with other services

## 📦 Product Categories and Rules

| Category | Rules |
|----------|-------|
| **Standard Products** | Order can be fulfilled if available quantity >= requested amount |
| **Perishable Products** | Product must not be expired AND have sufficient quantity |
| **Digital Products** | Always considered available (no quantity restrictions) |

## 🛠️ Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Apache Kafka** - Asynchronous messaging
- **Redis** - Temporary data storage
- **Docker & Docker Compose** - Containerization
- **Maven** - Build tool

## 📚 API Documentation

### Create Order
**Endpoint:** `POST http://localhost:8081/orders`

**Request Body:**
```json
{
  "customerName": "Alice",
  "items": [
    {
      "productId": "P1001",
      "quantity": 2,
      "category": "standard"
    }
  ],
  "requestedAt": "2025-06-30T14:00:00Z"
}
```

**Response:**
```json
{
  "orderId": "ORD-ABC12345",
  "status": "PENDING",
  "message": "Order received and being processed"
}
```

### Check Order Status
**Endpoint:** `GET http://localhost:8081/orders/{orderId}`

**Response:**
```json
{
  "orderId": "ORD-ABC12345",
  "status": "APPROVED"
}
```

## 🧪 Sample API Calls and Test Scenarios

### Test Scenario 1: Standard Product (Should Approve)
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "items": [
      {
        "productId": "P1001",
        "quantity": 1,
        "category": "standard"
      }
    ],
    "requestedAt": "2025-06-30T14:00:00Z"
  }'
```

### Test Scenario 2: Perishable Product (Should Approve)
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "items": [
      {
        "productId": "P2001",
        "quantity": 3,
        "category": "perishable"
      }
    ],
    "requestedAt": "2025-06-30T14:00:00Z"
  }'
```

### Test Scenario 3: Digital Product (Should Always Approve)
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Bob Wilson",
    "items": [
      {
        "productId": "P3001",
        "quantity": 5,
        "category": "digital"
      }
    ],
    "requestedAt": "2025-06-30T14:00:00Z"
  }'
```

### Test Scenario 4: Insufficient Stock (Should Reject)
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Alice Johnson",
    "items": [
      {
        "productId": "P1001",
        "quantity": 100,
        "category": "standard"
      }
    ],
    "requestedAt": "2025-06-30T14:00:00Z"
  }'
```

### Test Scenario 5: Mixed Order (Some Items Available)
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Charlie Brown",
    "items": [
      {
        "productId": "P1001",
        "quantity": 1,
        "category": "standard"
      },
      {
        "productId": "P2001",
        "quantity": 10,
        "category": "perishable"
      }
    ],
    "requestedAt": "2025-06-30T14:00:00Z"
  }'
```

### Check Order Status (Use orderId from previous responses)
```bash
curl http://localhost:8081/orders/ORD-ABC12345
```

## 🔍 Monitoring and Debugging

### View Service Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f order-service
docker-compose logs -f inventory-service
docker-compose logs -f notification-service
```

### Check Service Health
```bash
docker-compose ps
```

## 📡 Kafka Topics

| Topic Name | Description | Publisher | Consumer |
|------------|-------------|-----------|----------|
| `order-events` | Order creation events containing orderId | Order Service | Inventory Service |
| `inventory-check-results` | Inventory check results with approval/rejection status | Inventory Service | Notification Service |

## 🗄️ Redis Key Format

| Key Pattern | Description | TTL | Example |
|-------------|-------------|-----|---------|
| `order:{orderId}` | Complete order data (JSON) | 1 hour | `order:ORD-ABC12345` |
| `missingItems:{orderId}` | Missing items for rejected orders (JSON) | 1 hour | `missingItems:ORD-ABC12345` |

**Note:** All Redis keys have a TTL (Time To Live) of 1 hour to prevent data accumulation.

### Useful Debug Commands
```bash
# View Kafka topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check Redis data
docker exec redis redis-cli keys "*"

# Restart specific service
docker-compose restart order-service
```

## 💻 Development Setup

### Building and Running Each Service

#### Prerequisites
- Java 17
- Maven 3.9+
- Docker (for Kafka and Redis)

#### Step 1: Build Shared Library
```bash
cd shared-lib
mvn clean install
```

#### Step 2: Build Individual Services

**Order Service:**
```bash
cd order-service
mvn clean package
```

**Inventory Service:**
```bash
cd inventory-service
mvn clean package
```

**Notification Service:**
```bash
cd notification-service
mvn clean package
```

#### Step 3: Start Infrastructure (Kafka + Redis)
```bash
# From project root
docker-compose up -d zookeeper kafka redis
```

#### Step 4: Run Services Individually

**Order Service (Port 8081):**
```bash
cd order-service
java -jar target/order-service-1.0.0.jar
```

**Inventory Service (Port 8082):**
```bash
cd inventory-service
java -jar target/inventory-service-1.0.0.jar
```

**Notification Service (Port 8083):**
```bash
cd notification-service
java -jar target/notification-service-1.0.0.jar
```

### Alternative: Run All Services with Docker Compose
```bash
# Build and start all services
docker-compose up -d --build

# Or build first, then start
docker-compose build
docker-compose up -d
```

## 🗂️ Project Structure
```
order-processing-system/
├── shared-lib/                 # Shared DTOs and events
├── order-service/             # Order management service
├── inventory-service/         # Inventory checking service
├── notification-service/      # Notification service
├── docker-compose.yml         # Docker orchestration
└── README.md                  # This file
```

## 🚨 Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| **Services not starting** | Check if Kafka and Redis are running, verify port availability |
| **Kafka connection issues** | Ensure Zookeeper is running, check Kafka bootstrap servers |
| **Redis connection issues** | Verify Redis is running on port 6379, check connection pool settings |

### Stopping the System
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clears all data)
docker-compose down -v
```

## 📈 Performance Considerations

- **Kafka:** Configured with appropriate batch sizes and retry policies
- **Redis:** Connection pooling enabled
- **Services:** Concurrent Kafka listeners (3 threads per service)
- **Docker:** Resource limits can be configured in docker-compose.yml

## 🤝 Contributing

1. Follow the existing code structure and naming conventions
2. Add appropriate logging and error handling
3. Update documentation for any new features
4. Test with the provided scenarios

## 📄 License

This project is for educational purposes.

