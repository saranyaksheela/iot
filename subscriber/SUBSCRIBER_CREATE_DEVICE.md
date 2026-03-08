# Subscriber API - Create Device via Provider

## Endpoint

**Method:** `POST`  
**URL:** `http://localhost:8081/subscriber/api/devices`  
**Base URL:** `/subscriber/api`  
**Description:** Creates a device by forwarding the request to the Provider service

This endpoint acts as a proxy to the Provider's device creation endpoint.

---

## Sample Requests for Postman

### 1. Create Temperature Sensor via Subscriber

**URL:** `http://localhost:8081/subscriber/api/devices`  
**Method:** `POST`  
**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "deviceName": "Temperature Sensor via Subscriber",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A - Floor 2",
  "status": "active"
}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "Temperature Sensor via Subscriber",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A - Floor 2",
  "status": "active",
  "createdAt": "2026-03-09T10:30:00"
}
```

---

### 2. Create Smart Thermostat via Subscriber

**URL:** `http://localhost:8081/subscriber/api/devices`  
**Method:** `POST`  
**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "deviceName": "Smart Thermostat Beta",
  "deviceType": "thermostat",
  "firmwareVersion": "4.0.0",
  "location": "Building C - Main Hall",
  "status": "active"
}
```

---

### 3. Create Motion Sensor via Subscriber

**URL:** `http://localhost:8081/subscriber/api/devices`  
**Method:** `POST`  
**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "deviceName": "Motion Sensor Pro",
  "deviceType": "security",
  "firmwareVersion": "2.0.1",
  "location": "Building B - Parking",
  "status": "active"
}
```

---

### 4. Create Device with Minimal Data

**URL:** `http://localhost:8081/subscriber/api/devices`  
**Method:** `POST`  
**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "deviceName": "Basic Sensor from Subscriber",
  "deviceType": "sensor"
}
```

---

## Testing Steps

### 1. Start Both Services

**Terminal 1 - Provider (Port 8080):**
```bash
cd C:\Users\USER\Gokul\provider
mvn exec:java
```

**Terminal 2 - Subscriber (Port 8081):**
```bash
cd C:\Users\USER\Gokul\subscriber
mvn exec:java
```

### 2. Test Device Creation via Subscriber

**Using curl:**
```bash
curl -X POST http://localhost:8081/subscriber/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceName\":\"Test Device from Subscriber\",\"deviceType\":\"sensor\",\"firmwareVersion\":\"1.0.0\",\"location\":\"Test Location\",\"status\":\"active\"}"
```

**Using Postman:**
1. Set Method to `POST`
2. Enter URL: `http://localhost:8081/subscriber/api/devices`
3. Go to Headers tab, add:
   - Key: `Content-Type`
   - Value: `application/json`
4. Go to Body tab, select `raw` and `JSON`
5. Paste the device JSON
6. Click Send

### 3. Verify Device Was Created

**Get all devices from Provider:**
```bash
curl http://localhost:8080/provider/api/devices
```

**Or check directly in PostgreSQL:**
```sql
SELECT * FROM devices ORDER BY created_at DESC;
```

---

## How It Works

```
┌─────────────┐         ┌─────────────┐         ┌──────────────┐
│   Client    │         │ Subscriber  │         │   Provider   │         ┌──────────────┐
│  (Postman)  │────────▶│  :8081      │────────▶│   :8080      │────────▶│  PostgreSQL  │
│             │  POST   │             │  POST   │              │         │              │
└─────────────┘         └─────────────┘         └──────────────┘         └──────────────┘
                            /api/devices         /provider/api/devices
```

1. Client sends POST request to Subscriber (port 8081)
2. Subscriber forwards request to Provider (port 8080)
3. Provider creates device in PostgreSQL database
4. Provider returns response to Subscriber
5. Subscriber returns response to Client

---

## Error Scenarios

### Provider Service Not Running

**Request:**
```bash
curl -X POST http://localhost:8081/subscriber/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceName\":\"Test\",\"deviceType\":\"sensor\"}"
```

**Response (500 Internal Server Error):**
```json
{
  "error": "Failed to create device in provider",
  "message": "Connection refused: localhost/127.0.0.1:8080"
}
```

### Invalid Data (Missing Required Fields)

**Request:**
```bash
curl -X POST http://localhost:8081/subscriber/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"location\":\"Test\"}"
```

**Response (400 Bad Request):**
```json
{
  "error": "Bad Request",
  "message": "Missing required fields: deviceName, deviceType"
}
```

---

## All Subscriber Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/subscriber/api/devices` | Create device via Provider |
| GET | `/subscriber/api/fetch` | Fetch data from Provider |
| GET | `/subscriber/api/status` | Subscriber status |
| GET | `/health` | Health check |

---

## Quick Copy-Paste Samples

### Sample 1: IoT Sensor
```json
{"deviceName":"IoT Sensor from Subscriber","deviceType":"sensor","firmwareVersion":"3.0.0","location":"Data Center","status":"active"}
```

### Sample 2: Smart Device
```json
{"deviceName":"Smart Plug from Subscriber","deviceType":"actuator","firmwareVersion":"2.1.0","location":"Office Room 5","status":"active"}
```

### Sample 3: Security Device
```json
{"deviceName":"Security Camera from Subscriber","deviceType":"camera","firmwareVersion":"5.0.0","location":"Front Gate","status":"active"}
```

### Sample 4: Minimal
```json
{"deviceName":"Minimal Device","deviceType":"sensor"}
```
