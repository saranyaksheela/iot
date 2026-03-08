# Quick Test Guide - Create Device via Subscriber

## What Was Implemented

Added a new endpoint in the **Subscriber** service that forwards device creation requests to the **Provider** service.

```
Client → Subscriber (8081) → Provider (8080) → PostgreSQL
```

---

## Quick Test

### Step 1: Start Both Services

**Terminal 1 - Provider:**
```bash
cd C:\Users\USER\Gokul\provider
mvn clean compile exec:java
```

**Terminal 2 - Subscriber:**
```bash
cd C:\Users\USER\Gokul\subscriber
mvn clean compile exec:java
```

Wait for both services to start. You should see:
```
Subscriber service started on port 8081
Base URL: /subscriber/api
```

### Step 2: Test with Postman

**Import Collection:**
- Import `Subscriber_API_Postman_Collection.json` from the subscriber folder

**Or Manual Test:**

**URL:** `http://localhost:8081/subscriber/api/devices`  
**Method:** `POST`  
**Headers:**
```
Content-Type: application/json
```
**Body (raw JSON):**
```json
{
  "deviceName": "Temperature Sensor via Subscriber",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A - Floor 2",
  "status": "active"
}
```

**Click Send!**

### Step 3: Expected Response (201 Created)

```json
{
  "id": 1,
  "deviceUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "deviceName": "Temperature Sensor via Subscriber",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A - Floor 2",
  "status": "active",
  "createdAt": "2026-03-09T10:30:00"
}
```

---

## More Sample Requests

### Minimal Device:
```json
{
  "deviceName": "Basic Device",
  "deviceType": "sensor"
}
```

### Smart Light:
```json
{
  "deviceName": "Smart LED Strip",
  "deviceType": "lighting",
  "firmwareVersion": "3.5.2",
  "location": "Meeting Room",
  "status": "active"
}
```

### Security Camera:
```json
{
  "deviceName": "IP Camera 4K",
  "deviceType": "camera",
  "firmwareVersion": "6.0.0",
  "location": "Front Gate",
  "status": "active"
}
```

---

## Verify Device Creation

### Option 1: Query via Provider
```bash
curl http://localhost:8080/provider/api/devices
```

### Option 2: Check Database
```sql
SELECT * FROM devices ORDER BY created_at DESC LIMIT 5;
```

---

## Troubleshooting

### Error: "Failed to create device in provider"
- **Cause**: Provider service is not running
- **Fix**: Start the provider service on port 8080

### Error: "Connection refused"
- **Cause**: Provider service not accessible
- **Fix**: Verify provider is running: `curl http://localhost:8080/health`

### Error: 400 Bad Request
- **Cause**: Missing required fields (deviceName, deviceType)
- **Fix**: Include both fields in your request

---

## All Files Created

1. **SubscriberVerticle.java** - Updated with POST /api/devices endpoint
2. **SUBSCRIBER_CREATE_DEVICE.md** - Detailed documentation
3. **Subscriber_API_Postman_Collection.json** - Postman collection with 10 pre-configured requests
4. **README.md** - Updated with new endpoint info
5. **QUICK_TEST.md** - This file

---

## Architecture

```
┌──────────────┐
│   Postman    │
└──────┬───────┘
       │ POST /subscriber/api/devices
       │ Port 8081
       ▼
┌──────────────────────┐
│   Subscriber         │
│   (Port 8081)        │
│   Base: /subscriber  │
│   /api               │
│   - Receives request │
│   - Forwards to      │
│     Provider         │
└──────┬───────────────┘
       │ POST /provider/api/devices
       │ Port 8080
       ▼
┌──────────────────────┐         ┌──────────────┐
│   Provider           │         │  PostgreSQL  │
│   (Port 8080)        │────────▶│  Database    │
│   Base: /provider    │         │  :5432       │
│   /api               │         │              │
│   - Validates data   │         └──────────────┘
│   - Creates device   │
│   - Returns response │
└──────────────────────┘
```

---

## Success Indicators

✅ Both services start without errors  
✅ POST request returns 201 Created  
✅ Response contains deviceUuid (auto-generated)  
✅ Device appears in database  
✅ Can retrieve device via GET /provider/api/devices  

Happy Testing! 🚀
