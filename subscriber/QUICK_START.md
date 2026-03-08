# 🚀 QUICK START - Refactored Subscriber

## What Changed?

The Subscriber API now uses a base URL pattern:

**OLD**: `http://localhost:8081/api/devices`  
**NEW**: `http://localhost:8081/subscriber/api/devices`

---

## Start the Service

```bash
cd C:\Users\USER\Gokul\subscriber
mvn exec:java
```

**Expected:**
```
Subscriber service started on port 8081
Base URL: /subscriber/api
```

---

## Test It Now!

### 1. Health Check (unchanged)
```bash
curl http://localhost:8081/health
```

### 2. Status (NEW URL)
```bash
curl http://localhost:8081/subscriber/api/status
```

### 3. Create Device (NEW URL - requires Provider running)
```bash
curl -X POST http://localhost:8081/subscriber/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceName\":\"Quick Test Sensor\",\"deviceType\":\"sensor\"}"
```

---

## Postman Users

**Re-import the collection:**
- File: `Subscriber_API_Postman_Collection.json`
- All URLs updated automatically

---

## Summary

✅ Base URL: `/subscriber/api`  
✅ Constants: In `config/Constants.java`  
✅ Code: Clean and organized  
✅ Docs: All updated  

**You're good to go!** 🎉
