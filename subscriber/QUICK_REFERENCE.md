# Subscriber API - Quick Reference Card

## Service Information
- **Port**: 8081
- **Base URL**: `/subscriber/api`
- **Service Name**: subscriber

---

## Endpoints

### Health Check
```
GET http://localhost:8081/health
```

### Subscriber Status
```
GET http://localhost:8081/subscriber/api/status
```

### Fetch from Provider
```
GET http://localhost:8081/subscriber/api/fetch
```

### Create Device via Provider
```
POST http://localhost:8081/subscriber/api/devices
Content-Type: application/json

{
  "deviceName": "Device Name",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Location",
  "status": "active"
}
```

---

## Quick Test Commands

### Health Check
```bash
curl http://localhost:8081/health
```

### Status
```bash
curl http://localhost:8081/subscriber/api/status
```

### Fetch Data
```bash
curl http://localhost:8081/subscriber/api/fetch
```

### Create Device
```bash
curl -X POST http://localhost:8081/subscriber/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceName\":\"Test Device\",\"deviceType\":\"sensor\"}"
```

---

## Important Notes

⚠️ **Provider Must Be Running**: For device creation and data fetch endpoints, the Provider service must be running on port 8080.

✅ **Health Endpoint**: Can be accessed without Provider service.

✅ **Status Endpoint**: Can be accessed without Provider service.

---

## Start Service

```bash
cd C:\Users\USER\Gokul\subscriber
mvn exec:java
```

**Expected Output:**
```
Subscriber service started on port 8081
Base URL: /subscriber/api
```

---

## Architecture

```
Subscriber (8081)  →  Provider (8080)  →  PostgreSQL (5432)
/subscriber/api        /provider/api
```

---

## Files Modified

✅ Constants.java (NEW)
✅ SubscriberVerticle.java (REFACTORED)
✅ README.md (UPDATED)
✅ Postman Collection (UPDATED)
✅ Documentation (UPDATED)

---

Print this card for quick reference! 📋
