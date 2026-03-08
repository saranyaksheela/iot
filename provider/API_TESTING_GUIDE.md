# API Testing Guide with API Keys

## 🔑 API Keys Reference

### Provider Service API Keys
```
PRIMARY_KEY     = pk_live_12345abcdef67890provider
SUBSCRIBER_KEY  = pk_live_subscriber987654321xyz
ADMIN_KEY       = pk_live_admin_secret_key_2026
TEST_KEY        = pk_test_development_only_key
```

### Subscriber Service API Keys
```
CLIENT_APP_KEY  = sk_live_client_app_key_12345
MOBILE_APP_KEY  = sk_live_mobile_app_key_67890
DASHBOARD_KEY   = sk_live_dashboard_key_abcdef
TEST_CLIENT_KEY = sk_test_development_client_key
```

---

## 📡 Provider Service APIs (Port 8080)

### 1. Health Check (Public - No Auth Required)
```bash
curl http://localhost:8080/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "service": "provider",
  "timestamp": "2026-03-09T..."
}
```

---

### 2. Create Device (Protected)
```bash
curl -X POST http://localhost:8080/provider/api/devices \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_live_12345abcdef67890provider" \
  -d '{
    "deviceName": "Temperature Sensor 01",
    "deviceType": "sensor",
    "firmwareVersion": "1.0.0",
    "location": "Lab Room 101",
    "status": "active"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "Temperature Sensor 01",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Lab Room 101",
  "status": "active",
  "createdAt": "2026-03-09T10:30:00"
}
```

---

### 3. Get All Devices (Protected)
```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_live_12345abcdef67890provider"
```

**Expected Response (200 OK):**
```json
{
  "devices": [
    {
      "id": 1,
      "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
      "deviceName": "Temperature Sensor 01",
      "deviceType": "sensor",
      "firmwareVersion": "1.0.0",
      "location": "Lab Room 101",
      "status": "active",
      "createdAt": "2026-03-09T10:30:00"
    }
  ],
  "count": 1
}
```

---

### 4. Update Device (Protected)
```bash
curl -X PUT http://localhost:8080/provider/api/devices/1 \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_live_12345abcdef67890provider" \
  -d '{
    "deviceName": "Temperature Sensor 01 - Updated"
  }'
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "Temperature Sensor 01 - Updated",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Lab Room 101",
  "status": "active",
  "createdAt": "2026-03-09T10:30:00"
}
```

---

### 5. Delete Device (Protected)
```bash
curl -X DELETE http://localhost:8080/provider/api/devices/1 \
  -H "X-API-Key: pk_live_12345abcdef67890provider"
```

**Expected Response (200 OK):**
```json
{
  "message": "Device deleted successfully",
  "id": 1
}
```

---

### 6. Get Telemetry Data by Device ID (Protected)
```bash
curl -X GET http://localhost:8080/provider/api/telemetry/device/1 \
  -H "X-API-Key: pk_live_12345abcdef67890provider"
```

**Expected Response (200 OK):**
```json
{
  "deviceId": 1,
  "telemetry": [
    {
      "id": 1,
      "deviceId": 1,
      "topicId": 5,
      "payload": {
        "temperature": 25.5,
        "humidity": 60
      },
      "receivedAt": "2026-03-09T10:30:00"
    }
  ],
  "count": 1
}
```

---

### 7. Get Data (Protected)
```bash
curl -X GET http://localhost:8080/provider/api/data \
  -H "X-API-Key: pk_live_12345abcdef67890provider"
```

**Expected Response (200 OK):**
```json
{
  "message": "Hello from Provider Service",
  "timestamp": "2026-03-09T..."
}
```

---

## 📡 Subscriber Service APIs (Port 8081)

### 1. Health Check (Public - No Auth Required)
```bash
curl http://localhost:8081/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "service": "subscriber",
  "timestamp": "2026-03-09T..."
}
```

---

### 2. Service Status (Public - No Auth Required)
```bash
curl http://localhost:8081/subscriber/api/status
```

**Expected Response:**
```json
{
  "status": "UP",
  "service": "subscriber",
  "timestamp": "2026-03-09T..."
}
```

---

### 3. Create Device via Proxy (Protected)
```bash
curl -X POST http://localhost:8081/subscriber/api/devices \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_client_app_key_12345" \
  -d '{
    "deviceName": "Humidity Sensor 01",
    "deviceType": "sensor",
    "firmwareVersion": "1.0.0",
    "location": "Lab Room 102",
    "status": "active"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 2,
  "deviceUuid": "660e8400-e29b-41d4-a716-446655440001",
  "deviceName": "Humidity Sensor 01",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Lab Room 102",
  "status": "active",
  "createdAt": "2026-03-09T10:35:00"
}
```

---

### 4. Update Device via Proxy (Protected)
```bash
curl -X PUT http://localhost:8081/subscriber/api/devices/2 \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_client_app_key_12345" \
  -d '{
    "deviceName": "Humidity Sensor 01 - Updated"
  }'
```

**Expected Response (200 OK):**
```json
{
  "id": 2,
  "deviceUuid": "660e8400-e29b-41d4-a716-446655440001",
  "deviceName": "Humidity Sensor 01 - Updated",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Lab Room 102",
  "status": "active",
  "createdAt": "2026-03-09T10:35:00"
}
```

---

### 5. Delete Device via Proxy (Protected)
```bash
curl -X DELETE http://localhost:8081/subscriber/api/devices/2 \
  -H "X-API-Key: sk_live_client_app_key_12345"
```

**Expected Response (200 OK):**
```json
{
  "message": "Device deleted successfully",
  "id": 2
}
```

---

### 6. Get Telemetry via Proxy (Protected)
```bash
curl -X GET http://localhost:8081/subscriber/api/telemetry/device/1 \
  -H "X-API-Key: sk_live_client_app_key_12345"
```

**Expected Response (200 OK):**
```json
{
  "deviceId": 1,
  "telemetry": [
    {
      "id": 1,
      "deviceId": 1,
      "topicId": 5,
      "payload": {
        "temperature": 25.5,
        "humidity": 60
      },
      "receivedAt": "2026-03-09T10:30:00"
    }
  ],
  "count": 1
}
```

---

### 7. Fetch Data from Provider (Protected)
```bash
curl -X GET http://localhost:8081/subscriber/api/fetch \
  -H "X-API-Key: sk_live_client_app_key_12345"
```

**Expected Response (200 OK):**
```json
{
  "message": "Hello from Provider Service",
  "source": "provider",
  "timestamp": "2026-03-09T..."
}
```

---

## 🧪 Testing Different API Keys

### Test with Different Provider Keys

**Using Admin Key:**
```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_live_admin_secret_key_2026"
```

**Using Test Key:**
```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_test_development_only_key"
```

**Using Subscriber Key (for subscriber to access provider):**
```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_live_subscriber987654321xyz"
```

---

### Test with Different Subscriber Keys

**Using Mobile App Key:**
```bash
curl -X POST http://localhost:8081/subscriber/api/devices \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_mobile_app_key_67890" \
  -d '{"deviceName":"Mobile Test","deviceType":"sensor"}'
```

**Using Dashboard Key:**
```bash
curl -X GET http://localhost:8081/subscriber/api/telemetry/device/1 \
  -H "X-API-Key: sk_live_dashboard_key_abcdef"
```

**Using Test Client Key:**
```bash
curl -X DELETE http://localhost:8081/subscriber/api/devices/1 \
  -H "X-API-Key: sk_test_development_client_key"
```

---

## ❌ Testing Authentication Failures

### 1. Missing API Key (Should return 401)
```bash
curl -X GET http://localhost:8080/provider/api/devices
```

**Expected Response:**
```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing API key",
  "statusCode": 401
}
```

---

### 2. Invalid API Key (Should return 401)
```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: invalid_key_12345"
```

**Expected Response:**
```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing API key",
  "statusCode": 401
}
```

---

### 3. Disabled API Key (Should return 401)
```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_disabled_old_key_example"
```

**Expected Response:**
```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing API key",
  "statusCode": 401
}
```

---

## 📋 Complete Testing Workflow

### Step 1: Start Services
```bash
# Terminal 1 - Provider
cd provider
mvn exec:java

# Terminal 2 - Subscriber
cd subscriber
mvn exec:java
```

---

### Step 2: Test Public Endpoints
```bash
# Provider health
curl http://localhost:8080/health

# Subscriber health
curl http://localhost:8081/health

# Subscriber status
curl http://localhost:8081/subscriber/api/status
```

---

### Step 3: Test Provider Direct Access
```bash
# 1. Create a device
curl -X POST http://localhost:8080/provider/api/devices \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_live_12345abcdef67890provider" \
  -d '{"deviceName":"Test Sensor","deviceType":"sensor","firmwareVersion":"1.0.0","location":"Lab 1"}'

# 2. Get all devices (note the ID from response)
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_live_12345abcdef67890provider"

# 3. Update device (use ID from step 1)
curl -X PUT http://localhost:8080/provider/api/devices/1 \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_live_12345abcdef67890provider" \
  -d '{"deviceName":"Updated Sensor"}'

# 4. Get telemetry
curl -X GET http://localhost:8080/provider/api/telemetry/device/1 \
  -H "X-API-Key: pk_live_12345abcdef67890provider"

# 5. Delete device
curl -X DELETE http://localhost:8080/provider/api/devices/1 \
  -H "X-API-Key: pk_live_12345abcdef67890provider"
```

---

### Step 4: Test Subscriber Proxy Access
```bash
# 1. Create device via subscriber
curl -X POST http://localhost:8081/subscriber/api/devices \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_client_app_key_12345" \
  -d '{"deviceName":"Proxy Sensor","deviceType":"sensor","firmwareVersion":"1.0.0","location":"Lab 2"}'

# 2. Update device via subscriber
curl -X PUT http://localhost:8081/subscriber/api/devices/1 \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_client_app_key_12345" \
  -d '{"deviceName":"Updated Proxy Sensor"}'

# 3. Get telemetry via subscriber
curl -X GET http://localhost:8081/subscriber/api/telemetry/device/1 \
  -H "X-API-Key: sk_live_client_app_key_12345"

# 4. Fetch data
curl -X GET http://localhost:8081/subscriber/api/fetch \
  -H "X-API-Key: sk_live_client_app_key_12345"

# 5. Delete device via subscriber
curl -X DELETE http://localhost:8081/subscriber/api/devices/1 \
  -H "X-API-Key: sk_live_client_app_key_12345"
```

---

### Step 5: Test Authentication
```bash
# Test without API key (should fail with 401)
curl -X GET http://localhost:8080/provider/api/devices

# Test with invalid API key (should fail with 401)
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: wrong_key"

# Test with disabled API key (should fail with 401)
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_disabled_old_key_example"
```

---

## 🎯 Quick Copy-Paste Commands

### Provider - Complete CRUD Test
```bash
# Create
curl -X POST http://localhost:8080/provider/api/devices -H "Content-Type: application/json" -H "X-API-Key: pk_live_12345abcdef67890provider" -d '{"deviceName":"Quick Test","deviceType":"sensor","firmwareVersion":"1.0.0","location":"Lab"}'

# Read
curl -X GET http://localhost:8080/provider/api/devices -H "X-API-Key: pk_live_12345abcdef67890provider"

# Update
curl -X PUT http://localhost:8080/provider/api/devices/1 -H "Content-Type: application/json" -H "X-API-Key: pk_live_12345abcdef67890provider" -d '{"deviceName":"Updated Quick Test"}'

# Delete
curl -X DELETE http://localhost:8080/provider/api/devices/1 -H "X-API-Key: pk_live_12345abcdef67890provider"
```

---

### Subscriber - Complete CRUD Test
```bash
# Create
curl -X POST http://localhost:8081/subscriber/api/devices -H "Content-Type: application/json" -H "X-API-Key: sk_live_client_app_key_12345" -d '{"deviceName":"Subscriber Test","deviceType":"sensor","firmwareVersion":"1.0.0","location":"Lab"}'

# Update
curl -X PUT http://localhost:8081/subscriber/api/devices/1 -H "Content-Type: application/json" -H "X-API-Key: sk_live_client_app_key_12345" -d '{"deviceName":"Updated Subscriber Test"}'

# Telemetry
curl -X GET http://localhost:8081/subscriber/api/telemetry/device/1 -H "X-API-Key: sk_live_client_app_key_12345"

# Delete
curl -X DELETE http://localhost:8081/subscriber/api/devices/1 -H "X-API-Key: sk_live_client_app_key_12345"
```

---

## 📊 API Summary Table

| Service | Method | Endpoint | Auth Required | API Key Example |
|---------|--------|----------|---------------|-----------------|
| Provider | GET | `/health` | ❌ No | - |
| Provider | POST | `/provider/api/devices` | ✅ Yes | pk_live_12345... |
| Provider | GET | `/provider/api/devices` | ✅ Yes | pk_live_12345... |
| Provider | PUT | `/provider/api/devices/:id` | ✅ Yes | pk_live_12345... |
| Provider | DELETE | `/provider/api/devices/:id` | ✅ Yes | pk_live_12345... |
| Provider | GET | `/provider/api/telemetry/device/:id` | ✅ Yes | pk_live_12345... |
| Provider | GET | `/provider/api/data` | ✅ Yes | pk_live_12345... |
| Subscriber | GET | `/health` | ❌ No | - |
| Subscriber | GET | `/subscriber/api/status` | ❌ No | - |
| Subscriber | POST | `/subscriber/api/devices` | ✅ Yes | sk_live_client... |
| Subscriber | PUT | `/subscriber/api/devices/:id` | ✅ Yes | sk_live_client... |
| Subscriber | DELETE | `/subscriber/api/devices/:id` | ✅ Yes | sk_live_client... |
| Subscriber | GET | `/subscriber/api/telemetry/device/:id` | ✅ Yes | sk_live_client... |
| Subscriber | GET | `/subscriber/api/fetch` | ✅ Yes | sk_live_client... |

---

**Total APIs:** 14 (7 Provider + 7 Subscriber)  
**Protected APIs:** 11  
**Public APIs:** 3  
**Date:** March 9, 2026
