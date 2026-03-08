# Subscriber Vert.x Application

This is a Vert.x-based subscriber service application that communicates with the provider service.

## Project Structure

```
subscriber/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── iot/
│   │           └── subscriber/
│   │               ├── SubscriberApp.java        # Main application entry point
│   │               ├── SubscriberVerticle.java   # Vert.x verticle with HTTP server
│   │               └── config/
│   │                   └── Constants.java        # Application constants
│   └── test/
│       └── java/
│           └── iot/
│               └── subscriber/
│                   └── SubscriberVerticleTest.java  # Unit tests
└── pom.xml
```

## Features

- **Vert.x Core**: Reactive event-driven framework
- **Vert.x Web**: HTTP server with routing
- **Vert.x Web Client**: HTTP client for calling provider service
- **REST API Standards**: Proper base URL structure and constants management
- **Configuration Management**: Centralized constants
- **REST API Endpoints**:
  - `POST /subscriber/api/devices` - Create device via Provider service
  - `GET /subscriber/api/fetch` - Fetches data from provider service
  - `GET /subscriber/api/status` - Subscriber's own status endpoint
  - `GET /health` - Health check endpoint
- **JUnit 5 Tests**: Integration tests using Vert.x JUnit 5

## Building the Project

```bash
mvn clean package
```

## Running the Application

### Using Maven Exec Plugin:
```bash
mvn exec:java
```

### Using Java directly:
```bash
java -cp target/classes;%HOME%\.m2\repository\io\vertx\vertx-core\4.5.6\* iot.subscriber.SubscriberApp
```

**Note**: The subscriber runs on **port 8081** (different from provider which runs on 8080)

## Running Tests

```bash
mvn test
```

## Testing the Endpoints

**Base URL**: `/subscriber/api`

Once the application is running, you can test the endpoints:

### Health Check:
```bash
curl http://localhost:8081/health
```

Response:
```json
{"status":"UP","service":"subscriber"}
```

### Status Endpoint:
```bash
curl http://localhost:8081/subscriber/api/status
```

Response:
```json
{"message":"Hello from Subscriber Service","timestamp":1234567890}
```

### Create Device via Provider:
**Prerequisites**: Make sure the provider service is running on port 8080

```bash
curl -X POST http://localhost:8081/subscriber/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceName\":\"Temperature Sensor\",\"deviceType\":\"sensor\",\"firmwareVersion\":\"1.0.0\",\"location\":\"Building A\",\"status\":\"active\"}"
```

Response:
```json
{
  "id": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "Temperature Sensor",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A",
  "status": "active",
  "createdAt": "2026-03-09T10:30:00"
}
```

### Fetch from Provider:
**Prerequisites**: Make sure the provider service is running on port 8080

```bash
curl http://localhost:8081/subscriber/api/fetch
```

Response:
```json
{"source":"provider","data":{"message":"Hello from Provider Service","timestamp":1234567890}}
```

## Running Both Services

To run both provider and subscriber services together:

1. **Terminal 1** - Start Provider:
   ```bash
   cd C:\Users\USER\Gokul\provider
   mvn exec:java
   ```

2. **Terminal 2** - Start Subscriber:
   ```bash
   cd C:\Users\USER\Gokul\subscriber
   mvn exec:java
   ```

## Configuration

- **Provider**: Runs on port 8080
- **Subscriber**: Runs on port 8081 and connects to provider on port 8080

You can change ports by modifying the configuration in the respective Verticle classes.

## Dependencies

- Vert.x Core 4.5.6
- Vert.x Web 4.5.6
- Vert.x Web Client 4.5.6
- Vert.x JUnit 5 4.5.6 (test)
- JUnit Jupiter 5.10.2 (test)
