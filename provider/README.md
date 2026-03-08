# Provider Vert.x Application

This is a Vert.x-based provider service application with PostgreSQL database integration, following REST API standards.

## Project Structure

```
provider/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── iot/
│   │   │       └── provider/
│   │   │           ├── ProviderApp.java        # Main application entry point
│   │   │           ├── ProviderVerticle.java   # Vert.x verticle with HTTP server
│   │   │           ├── config/
│   │   │           │   └── Constants.java      # Application constants
│   │   │           └── model/
│   │   │               └── Device.java         # Device model class
│   │   └── resources/
│   │       └── db-config.json                  # Database configuration
│   └── test/
│       └── java/
│           └── iot/
│               └── provider/
│                   └── ProviderVerticleTest.java  # Unit tests
└── pom.xml
```

## Features

- **Vert.x Core**: Reactive event-driven framework
- **Vert.x Web**: HTTP server with routing
- **Vert.x PostgreSQL Client**: Reactive PostgreSQL database client
- **REST API Standards**: Proper HTTP status codes, error handling, and response formats
- **Configuration Management**: Externalized database configuration
- **Constants Management**: Centralized constants for maintainability

## REST API Endpoints

**Base URL**: `/provider/api`

| Method | Endpoint | Description | Status Codes |
|--------|----------|-------------|--------------|
| POST | `/provider/api/devices` | Create a new device | 201, 400, 500 |
| GET | `/provider/api/devices` | Get all devices | 200, 500 |
| GET | `/provider/api/data` | Get sample data | 200 |
| GET | `/health` | Health check | 200 |

## Database Setup

### PostgreSQL Configuration
Configuration is stored in `src/main/resources/db-config.json`:

```json
{
  "http": {
    "port": 8080
  },
  "database": {
    "host": "localhost",
    "port": 5432,
    "database": "postgres",
    "user": "postgres",
    "password": "123",
    "pool_size": 5
  }
}
```

### Database Schema

Create the devices table in your PostgreSQL database:

```sql
CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_uuid UUID UNIQUE NOT NULL,
    device_name VARCHAR(100),
    device_type VARCHAR(50),
    firmware_version VARCHAR(50),
    location VARCHAR(100),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Building the Project

```bash
mvn clean package
```

## Running the Application

### Using Maven Exec Plugin:
```bash
mvn exec:java
```

The application will start and display:
```
Database connection pool initialized
Provider service started on port 8080
Base URL: /provider/api
```

## API Usage Examples

### 1. Health Check
```bash
curl http://localhost:8080/health
```

**Response (200 OK)**:
```json
{"status":"UP"}
```

### 2. Create a Device
```bash
curl -X POST http://localhost:8080/provider/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceName\":\"Temperature Sensor 1\",\"deviceType\":\"sensor\",\"firmwareVersion\":\"1.0.0\",\"location\":\"Building A\",\"status\":\"active\"}"
```

**Response (201 Created)**:
```json
{
  "id": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "Temperature Sensor 1",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A",
  "status": "active",
  "createdAt": "2026-03-09T10:30:00"
}
```

**Error Response (400 Bad Request)** - Missing required fields:
```json
{
  "error": "Bad Request",
  "message": "Missing required fields: deviceName, deviceType"
}
```

### 3. Get All Devices
```bash
curl http://localhost:8080/provider/api/devices
```

**Response (200 OK)**:
```json
{
  "devices": [
    {
      "id": 1,
      "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
      "deviceName": "Temperature Sensor 1",
      "deviceType": "sensor",
      "firmwareVersion": "1.0.0",
      "location": "Building A",
      "status": "active",
      "createdAt": "2026-03-09T10:30:00"
    }
  ],
  "count": 1
}
```

### 4. Get Sample Data
```bash
curl http://localhost:8080/provider/api/data
```

**Response (200 OK)**:
```json
{
  "message": "Hello from Provider Service",
  "timestamp": 1234567890
}
```

## REST API Standards

The API follows REST standards:

- **HTTP Status Codes**:
  - `200 OK` - Successful GET request
  - `201 Created` - Successful POST request
  - `400 Bad Request` - Invalid input
  - `500 Internal Server Error` - Server error

- **Content Type**: All responses use `application/json`

- **Error Format**: Consistent error response structure
  ```json
  {
    "error": "Error Type",
    "message": "Detailed error message"
  }
  ```

- **Validation**: Required fields are validated before processing

## Configuration

### HTTP Configuration
- **Default Port**: 8080 (configurable in `db-config.json`)

### Database Configuration
All database settings are externalized to `db-config.json`:
- Host, port, database name
- User credentials
- Connection pool size

### Constants
Application constants are centralized in `Constants.java`:
- API endpoints and base URL
- HTTP status codes
- JSON keys and database column names
- SQL queries
- Default values

## Running Tests

```bash
mvn test
```

## Dependencies

- Vert.x Core 4.5.6
- Vert.x Web 4.5.6
- Vert.x PostgreSQL Client 4.5.6
- Vert.x SQL Client Templates 4.5.6
- Vert.x Web Client 4.5.6 (test)
- Vert.x JUnit 5 4.5.6 (test)
- JUnit Jupiter 5.10.2 (test)
