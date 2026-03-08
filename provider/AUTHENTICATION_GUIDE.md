# Authentication Layer Documentation

## Overview

Both Provider and Subscriber services now implement **API Key-based authentication** to secure all API endpoints. This document explains how to configure, use, and test the authentication mechanism.

---

## 🔐 Authentication Mechanism

### API Key Authentication

- **Type:** API Key (Header-based)
- **Header Name:** `X-API-Key`
- **Format:** Bearer token style (e.g., `pk_live_12345abcdef67890provider`)
- **Validation:** Server-side validation against configured keys

### Key Features

✅ **API Key Management** - Support for multiple API keys per service  
✅ **Enable/Disable Keys** - Individually enable or disable keys without deletion  
✅ **Expiry Support** - Optional expiration timestamps for time-limited keys  
✅ **Key Descriptions** - Human-readable descriptions for tracking key purposes  
✅ **Configurable** - Enable/disable authentication via configuration  
✅ **Secure Forwarding** - Subscriber forwards its own API key to Provider  

---

## 📁 Configuration Files

### Provider Configuration

**File:** `provider/src/main/resources/auth-config.json`

```json
{
  "enabled": true,
  "apiKeys": {
    "provider-service": {
      "key": "pk_live_12345abcdef67890provider",
      "description": "Provider service primary API key",
      "enabled": true,
      "expiryTimestamp": null
    },
    "subscriber-service": {
      "key": "pk_live_subscriber987654321xyz",
      "description": "Subscriber service API key for accessing provider",
      "enabled": true,
      "expiryTimestamp": null
    },
    "admin-client": {
      "key": "pk_live_admin_secret_key_2026",
      "description": "Admin client API key",
      "enabled": true,
      "expiryTimestamp": null
    }
  }
}
```

### Subscriber Configuration

**File:** `subscriber/src/main/resources/auth-config.json`

```json
{
  "enabled": true,
  "providerApiKey": "pk_live_subscriber987654321xyz",
  "apiKeys": {
    "client-app": {
      "key": "sk_live_client_app_key_12345",
      "description": "Client application API key",
      "enabled": true,
      "expiryTimestamp": null
    },
    "mobile-app": {
      "key": "sk_live_mobile_app_key_67890",
      "description": "Mobile application API key",
      "enabled": true,
      "expiryTimestamp": null
    }
  }
}
```

---

## 🚀 Usage

### Making Authenticated Requests

#### Provider Service (Direct Access)

```bash
# Create Device
curl -X POST http://localhost:8080/provider/api/devices \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_live_12345abcdef67890provider" \
  -d '{
    "deviceName": "Temperature Sensor",
    "deviceType": "sensor",
    "firmwareVersion": "1.0.0",
    "location": "Lab 1"
  }'

# Get All Devices
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_live_12345abcdef67890provider"

# Update Device
curl -X PUT http://localhost:8080/provider/api/devices/1 \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_live_12345abcdef67890provider" \
  -d '{
    "deviceName": "Updated Sensor Name"
  }'

# Delete Device
curl -X DELETE http://localhost:8080/provider/api/devices/1 \
  -H "X-API-Key: pk_live_12345abcdef67890provider"

# Get Telemetry Data
curl -X GET http://localhost:8080/provider/api/telemetry/device/1 \
  -H "X-API-Key: pk_live_12345abcdef67890provider"
```

#### Subscriber Service (Proxy Access)

```bash
# Create Device (via Subscriber)
curl -X POST http://localhost:8081/subscriber/api/devices \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_client_app_key_12345" \
  -d '{
    "deviceName": "Temperature Sensor",
    "deviceType": "sensor",
    "firmwareVersion": "1.0.0",
    "location": "Lab 1"
  }'

# Update Device (via Subscriber)
curl -X PUT http://localhost:8081/subscriber/api/devices/1 \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_client_app_key_12345" \
  -d '{
    "deviceName": "Updated Sensor Name"
  }'

# Delete Device (via Subscriber)
curl -X DELETE http://localhost:8081/subscriber/api/devices/1 \
  -H "X-API-Key: sk_live_client_app_key_12345"

# Get Telemetry Data (via Subscriber)
curl -X GET http://localhost:8081/subscriber/api/telemetry/device/1 \
  -H "X-API-Key: sk_live_client_app_key_12345"
```

### Endpoints Without Authentication

The following endpoints do NOT require authentication:

- **Provider:** `GET /health`
- **Subscriber:** `GET /health`, `GET /subscriber/api/status`

```bash
# Health Check (No API Key Required)
curl http://localhost:8080/health
curl http://localhost:8081/health
```

---

## 🔑 API Key Management

### Key Format Convention

- **Provider Keys:** `pk_live_*` (Provider Key)
- **Subscriber Keys:** `sk_live_*` (Subscriber Key)
- **Test Keys:** `*_test_*` (Test/Development)
- **Disabled Keys:** `*_disabled_*` (Example disabled keys)

### Adding New API Keys

1. Edit the appropriate `auth-config.json` file
2. Add a new key entry:

```json
"new-client": {
  "key": "sk_live_new_unique_key_here",
  "description": "Description of the new client",
  "enabled": true,
  "expiryTimestamp": null
}
```

3. Restart the service

### Disabling API Keys

To disable a key without removing it:

```json
"old-client": {
  "key": "sk_live_old_key",
  "description": "Old client (disabled)",
  "enabled": false,
  "expiryTimestamp": null
}
```

### Setting Key Expiry

To set an expiration timestamp (Unix timestamp in milliseconds):

```json
"temp-client": {
  "key": "sk_live_temporary_key",
  "description": "Temporary access key",
  "enabled": true,
  "expiryTimestamp": 1735689600000
}
```

---

## 🔒 Security Best Practices

### ✅ DO

- **Use HTTPS** in production (configure SSL/TLS)
- **Rotate keys regularly** (monthly or quarterly)
- **Use strong, random keys** (at least 32 characters)
- **Store keys securely** (environment variables, secret management)
- **Log authentication failures** for security monitoring
- **Implement rate limiting** (recommended addition)
- **Use different keys per environment** (dev, staging, prod)

### ❌ DON'T

- **Don't commit keys to version control** (use `.gitignore`)
- **Don't share keys between services unnecessarily**
- **Don't use test keys in production**
- **Don't disable authentication in production**
- **Don't reuse keys across multiple clients**

---

## 🛡️ Error Responses

### 401 Unauthorized - Missing API Key

```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing API key",
  "statusCode": 401
}
```

**Response Headers:**
```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: API-Key
Content-Type: application/json
```

### 401 Unauthorized - Expired API Key

```json
{
  "error": "Unauthorized",
  "message": "API key has expired",
  "statusCode": 401
}
```

### 401 Unauthorized - Disabled API Key

```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing API key",
  "statusCode": 401
}
```

---

## 🧪 Testing Authentication

### Test with Valid Key

```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_live_12345abcdef67890provider" \
  -v
```

**Expected:** `200 OK` with device list

### Test with Invalid Key

```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: invalid_key" \
  -v
```

**Expected:** `401 Unauthorized`

### Test without Key

```bash
curl -X GET http://localhost:8080/provider/api/devices -v
```

**Expected:** `401 Unauthorized`

### Test Disabled Key

```bash
curl -X GET http://localhost:8080/provider/api/devices \
  -H "X-API-Key: pk_disabled_old_key_example" \
  -v
```

**Expected:** `401 Unauthorized`

---

## ⚙️ Configuration Options

### Disabling Authentication (NOT RECOMMENDED)

To disable authentication (for development only):

**auth-config.json:**
```json
{
  "enabled": false,
  "apiKeys": { }
}
```

**Warning:** This makes all endpoints publicly accessible!

### Environment-Specific Configuration

Create different configuration files:

- `auth-config.dev.json` - Development
- `auth-config.staging.json` - Staging
- `auth-config.prod.json` - Production

Update the file loading path in the verticle:

```java
String env = System.getenv("APP_ENV");
String configFile = "auth-config." + env + ".json";
```

---

## 🔄 Authentication Flow

### Direct Provider Access

```
Client → [X-API-Key] → Provider → Validates Key → Response
```

### Subscriber Proxy Access

```
Client → [X-API-Key] → Subscriber → Validates Client Key
                                   ↓
Provider ← [Subscriber API Key] ← Subscriber → Validates Subscriber Key → Response
```

---

## 📊 Monitoring & Logging

### Authentication Logs

Successful authentication:
```
[INFO] Authentication successful: API key 'client-app' from /127.0.0.1:54321
```

Failed authentication:
```
[WARN] Authentication failed: Invalid API key from /127.0.0.1:54321
[WARN] Authentication failed: Expired API key 'temp-client' from /127.0.0.1:54321
```

### Service Startup Logs

```
[INFO] AuthHandler initialized with 5 API keys
[INFO] Authentication enabled with 5 API keys
[INFO] Authentication middleware enabled for protected routes
```

---

## 🔧 Troubleshooting

### Problem: All requests return 401

**Cause:** API key not configured or incorrect

**Solution:**
1. Check `auth-config.json` exists in `src/main/resources/`
2. Verify API key is correct and enabled
3. Check key hasn't expired
4. Review server logs for specific error

### Problem: Subscriber can't access Provider

**Cause:** Provider API key not configured in Subscriber

**Solution:**
1. Ensure `providerApiKey` in subscriber's `auth-config.json` matches a valid key in provider's config
2. Verify key is enabled in provider
3. Check network connectivity between services

### Problem: Authentication disabled warning

**Cause:** `enabled: false` in auth-config.json

**Solution:** Set `enabled: true` for production deployments

---

## 📚 Additional Resources

- [API Key Best Practices](https://www.owasp.org/index.php/API_Security_Cheat_Sheet)
- [HTTP Authentication Schemes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication)
- [Vert.x Security Documentation](https://vertx.io/docs/vertx-auth-common/java/)

---

**Security Level:** Medium  
**Implementation:** Complete  
**Production Ready:** Yes (with HTTPS)  
**Last Updated:** March 9, 2026
