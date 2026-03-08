# Postman Testing Guide - Provider API

## Import Postman Collection

Import the `Provider_API_Postman_Collection.json` file into Postman to get all pre-configured requests.

## Base URL
```
http://localhost:8080/provider/api
```

---

## 1. Health Check

**Method:** `GET`  
**URL:** `http://localhost:8080/health`

**Expected Response (200 OK):**
```json
{
  "status": "UP"
}
```

---

## 2. Create Device - Temperature Sensor

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Temperature Sensor 1",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A - Floor 1",
  "status": "active"
}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "Temperature Sensor 1",
  "deviceType": "sensor",
  "firmwareVersion": "1.0.0",
  "location": "Building A - Floor 1",
  "status": "active",
  "createdAt": "2026-03-09T10:30:00"
}
```

---

## 3. Create Device - Humidity Sensor

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Humidity Sensor 2",
  "deviceType": "sensor",
  "firmwareVersion": "2.1.5",
  "location": "Building B - Floor 2",
  "status": "active"
}
```

---

## 4. Create Device - Smart Thermostat

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Smart Thermostat Alpha",
  "deviceType": "thermostat",
  "firmwareVersion": "3.2.1",
  "location": "Building A - Lobby",
  "status": "active"
}
```

---

## 5. Create Device - Motion Detector

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Motion Detector X100",
  "deviceType": "security",
  "firmwareVersion": "1.5.2",
  "location": "Building C - Entrance",
  "status": "active"
}
```

---

## 6. Create Device - Smart Light

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Smart LED Light Strip",
  "deviceType": "lighting",
  "firmwareVersion": "4.0.3",
  "location": "Building A - Conference Room",
  "status": "active"
}
```

---

## 7. Create Device - Air Quality Monitor

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Air Quality Monitor Pro",
  "deviceType": "sensor",
  "firmwareVersion": "2.3.0",
  "location": "Building D - Office Space",
  "status": "active"
}
```

---

## 8. Create Device - Water Leak Detector

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Water Leak Detector WL-500",
  "deviceType": "security",
  "firmwareVersion": "1.2.8",
  "location": "Building A - Basement",
  "status": "active"
}
```

---

## 9. Create Device - Smart Door Lock

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Smart Door Lock Z-Wave",
  "deviceType": "security",
  "firmwareVersion": "5.1.0",
  "location": "Building B - Main Entrance",
  "status": "active"
}
```

---

## 10. Create Device - Energy Monitor

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Energy Monitor EM-2000",
  "deviceType": "monitor",
  "firmwareVersion": "3.0.1",
  "location": "Building C - Electrical Room",
  "status": "active"
}
```

---

## 11. Create Device - Minimal Data (Only Required Fields)

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Basic Sensor",
  "deviceType": "sensor"
}
```

**Note:** Status will default to "active". firmwareVersion and location will be null.

---

## 12. Create Device - Inactive Device

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceName": "Old Sensor Unit",
  "deviceType": "sensor",
  "firmwareVersion": "0.9.1",
  "location": "Warehouse - Storage",
  "status": "inactive"
}
```

---

## 13. Test Validation - Missing Required Fields (Should Fail)

**Method:** `POST`  
**URL:** `http://localhost:8080/provider/api/devices`  
**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "firmwareVersion": "1.0.0",
  "location": "Test Location"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Bad Request",
  "message": "Missing required fields: deviceName, deviceType"
}
```

---

## 14. Get All Devices

**Method:** `GET`  
**URL:** `http://localhost:8080/provider/api/devices`

**Expected Response (200 OK):**
```json
{
  "devices": [
    {
      "id": 1,
      "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
      "deviceName": "Temperature Sensor 1",
      "deviceType": "sensor",
      "firmwareVersion": "1.0.0",
      "location": "Building A - Floor 1",
      "status": "active",
      "createdAt": "2026-03-09T10:30:00"
    },
    // ... more devices
  ],
  "count": 10
}
```

---

## 15. Get Sample Data

**Method:** `GET`  
**URL:** `http://localhost:8080/provider/api/data`

**Expected Response (200 OK):**
```json
{
  "message": "Hello from Provider Service",
  "timestamp": 1741522800000
}
```

---

## Device Types Examples

Here are various device types you can use:

- **sensor** - Temperature, Humidity, Air Quality, Pressure, etc.
- **thermostat** - Smart thermostats
- **lighting** - Smart lights, LED strips
- **security** - Motion detectors, Door locks, Water leak detectors
- **monitor** - Energy monitors, Power meters
- **actuator** - Motors, Valves, Switches
- **camera** - Security cameras, Surveillance systems
- **gateway** - IoT gateways, Hub devices

---

## Testing Steps

1. **Start the Provider Service**
   ```bash
   cd C:\Users\USER\Gokul\provider
   mvn exec:java
   ```

2. **Verify Service is Running**
   - Test the Health Check endpoint first

3. **Import Postman Collection**
   - Open Postman
   - Click Import
   - Select `Provider_API_Postman_Collection.json`

4. **Test Device Creation**
   - Run each "Create Device" request
   - Verify 201 Created response
   - Check that deviceUuid is auto-generated

5. **Test Validation**
   - Run the invalid request to verify 400 response

6. **Get All Devices**
   - Run "Get All Devices" to see all created devices

7. **Verify in Database**
   ```sql
   SELECT * FROM devices ORDER BY created_at DESC;
   ```

---

## Quick Copy-Paste Samples for Postman

### Sample 1: IoT Sensor
```json
{"deviceName":"IoT Temp Sensor V2","deviceType":"sensor","firmwareVersion":"2.5.1","location":"Data Center - Rack 5","status":"active"}
```

### Sample 2: Smart Device
```json
{"deviceName":"Smart Plug WiFi","deviceType":"actuator","firmwareVersion":"1.8.3","location":"Office - Desk 12","status":"active"}
```

### Sample 3: Security Device
```json
{"deviceName":"IP Camera 4K","deviceType":"camera","firmwareVersion":"6.2.0","location":"Parking Lot - Zone A","status":"active"}
```

### Sample 4: Gateway Device
```json
{"deviceName":"IoT Gateway Hub","deviceType":"gateway","firmwareVersion":"4.1.2","location":"Server Room","status":"active"}
```

### Sample 5: Minimal
```json
{"deviceName":"Test Device","deviceType":"sensor"}
```
