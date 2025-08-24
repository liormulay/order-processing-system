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

### 🔄 Handling Code Changes

**Important:** If you make changes to the source code, running `docker-compose up -d` alone will **NOT** publish your changes. The services use pre-built Docker images.

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
│   Client        │    │   Order Service │    │  Inventory      │
│   (HTTP)        │───▶│   (Port 8081)   │───▶│  Service        │
│                 │    │                 │    │  (Port 8082)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────┐         ┌─────────────┐
                       │    Redis    │         │    Kafka    │
                       │  (Port 6379)│         │  (Port 9092)│
                       └─────────────┘         └─────────────┘
                              ▲                        │
                              │                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │  Notification   │    │  Order Service  │
                       │  Service        │◀───│  (Status Update)│
                       │  (Port 8083)    │    │                 │
                       └─────────────────┘    └─────────────────┘
```

## 🔧 Service Responsibilities

### Order Service (Port 8081)
- Exposes REST API: `POST /orders` and `GET /orders/{orderId}`
- Receives order details from clients
- Publishes order events to Kafka
- Temporarily stores orders in Redis with PENDING status
- Provides order status retrieval endpoint

### Inventory Service (Port 8082)
- Listens to Kafka for incoming order events
- Checks product availability based on category-specific rules
- Updates order status directly in Redis
- Publishes inventory check results to Kafka for notifications
- Maintains in-memory product catalog

### Notification Service (Port 8083)
- Listens to inventory check results from Kafka
- Retrieves original orders from Redis
- Logs confirmation or rejection messages

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

### Kafka Topics
- `order-events` - Order creation events
- `inventory-check-results` - Inventory check results

### Redis Keys
- `order:{orderId}` - Stored order data with TTL

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

### Local Development (Optional)

1. **Build Shared Library:**
   ```bash
   cd shared-lib
   mvn clean install
   ```

2. **Build Services:**
   ```bash
   cd order-service && mvn clean package
   cd ../inventory-service && mvn clean package
   cd ../notification-service && mvn clean package
   ```

3. **Run Services Locally:**
   ```bash
   # Start Kafka and Redis only
   docker-compose up -d zookeeper kafka redis

   # Run services individually
   java -jar order-service/target/order-service-1.0.0.jar
   java -jar inventory-service/target/inventory-service-1.0.0.jar
   java -jar notification-service/target/notification-service-1.0.0.jar
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

