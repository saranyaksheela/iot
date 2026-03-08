# Authentication Implementation - Troubleshooting Guide

## Common Issues and Solutions

### Issue 1: NullPointerException on Startup ✅ FIXED

**Error Message:**
```
java.lang.NullPointerException: Cannot invoke "io.vertx.core.json.JsonObject.getInteger(String, java.lang.Integer)" 
because the return value of "io.vertx.core.json.JsonObject.getJsonObject(String)" is null
```

**Cause:**
The `startHttpServer` method was trying to access a nested configuration object that wasn't being passed correctly after the authentication config changes.

**Solution:**
Changed from:
```java
int port = config.getJsonObject("http").getInteger("port", Constants.DEFAULT_HTTP_PORT);
```

To:
```java
int port = config().getInteger(Constants.CONFIG_HTTP_PORT, Constants.DEFAULT_HTTP_PORT);
```

This uses the Vert.x verticle's built-in `config()` method which retrieves deployment configuration.

**Status:** ✅ Fixed in ProviderVerticle.java

---

### Issue 2: Authentication Config File Not Found

**Error Message:**
```
Failed to load config files: ... auth-config.json (No such file or directory)
```

**Cause:**
The `auth-config.json` file doesn't exist in `src/main/resources/`

**Solution:**
Ensure the following files exist:
- `provider/src/main/resources/auth-config.json`
- `subscriber/src/main/resources/auth-config.json`

These files are already created during authentication implementation.

---

### Issue 3: Database Connection Failed

**Error Message:**
```
Failed to initialize database connection pool
```

**Cause:**
PostgreSQL is not running or db-config.json has incorrect credentials.

**Solution:**
1. Start PostgreSQL service
2. Verify credentials in `provider/src/main/resources/db-config.json`
3. Ensure database and tables exist

**Note:** This is a Provider-only issue. Subscriber doesn't need database access.

---

### Issue 4: Port Already in Use

**Error Message:**
```
Failed to start HTTP server on port 8080: Address already in use
```

**Cause:**
Another instance of the service is already running on the same port.

**Solution:**
1. Stop the existing service
2. Or change the port by passing config:
   ```bash
   mvn exec:java -Dvertx.config='{"http.port":8090}'
   ```

---

### Issue 5: All Requests Return 401

**Symptom:**
All API requests fail with 401 Unauthorized, even with correct API key.

**Possible Causes:**
1. API key not in `auth-config.json`
2. API key is disabled (`"enabled": false`)
3. API key has expired
4. Wrong header name (should be `X-API-Key`)
5. Authentication disabled in config (`"enabled": false`)

**Solution:**
1. Verify key exists in auth-config.json
2. Check `"enabled": true` for that key
3. Check `"expiryTimestamp": null` or valid future timestamp
4. Use correct header: `X-API-Key` (case-sensitive)
5. Ensure `"enabled": true` in root config

---

### Issue 6: Subscriber Can't Access Provider

**Symptom:**
Subscriber returns 500 error when trying to proxy requests to Provider.

**Possible Causes:**
1. Provider service not running
2. Provider API key not configured in Subscriber
3. Network connectivity issue
4. Provider authentication rejecting Subscriber's key

**Solution:**
1. Ensure Provider is running on port 8080
2. Verify `"providerApiKey"` in subscriber's auth-config.json
3. Verify that key exists and is enabled in Provider's auth-config.json
4. Check Provider logs for authentication errors

---

### Issue 7: Tests Failing

**Symptom:**
Authentication tests fail with connection errors.

**Cause:**
Tests automatically deploy services, but may conflict with running instances.

**Solution:**
1. Stop all running instances of Provider/Subscriber
2. Ensure ports 8080 and 8081 are free
3. Run tests:
   ```bash
   mvn test -Dtest=ProviderAuthenticationTest
   mvn test -Dtest=SubscriberAuthenticationTest
   ```

---

## Quick Diagnostics

### Check Service Status

**Provider:**
```bash
curl http://localhost:8080/health
# Should return: {"status":"UP","service":"provider","timestamp":"..."}
```

**Subscriber:**
```bash
curl http://localhost:8081/health
# Should return: {"status":"UP","service":"subscriber","timestamp":"..."}
```

### Test Authentication

**Provider (with valid key):**
```bash
curl -H "X-API-Key: pk_live_12345abcdef67890provider" \
     http://localhost:8080/provider/api/devices
# Should return: 200 with device list
```

**Provider (without key):**
```bash
curl http://localhost:8080/provider/api/devices
# Should return: 401 Unauthorized
```

### Check Logs

**Look for these startup messages:**

Provider:
```
[INFO] AuthHandler initialized with 5 API keys
[INFO] Authentication enabled with 5 API keys
[INFO] Provider service started successfully on port 8080
```

Subscriber:
```
[INFO] AuthHandler initialized with 5 API keys
[INFO] Authentication enabled with 5 API keys
[INFO] Provider API key configured: true
[INFO] Subscriber service started successfully on port 8081
```

---

## Configuration Checklist

Before running services, verify:

### Provider
- ✅ `db-config.json` exists with valid database credentials
- ✅ `auth-config.json` exists with at least one enabled API key
- ✅ PostgreSQL is running
- ✅ Database tables created
- ✅ Port 8080 is available

### Subscriber
- ✅ `auth-config.json` exists with enabled client keys
- ✅ `providerApiKey` is set in auth-config.json
- ✅ Provider service is running
- ✅ Port 8081 is available

---

## Environment-Specific Configuration

### Development
- Authentication can be disabled: `"enabled": false` in auth-config.json
- Use test keys: `pk_test_*` or `sk_test_*`

### Production
- ⚠️ **MUST enable authentication:** `"enabled": true`
- ⚠️ **MUST use HTTPS/TLS**
- ⚠️ **Change default API keys**
- ⚠️ **Use strong, unique keys**
- ⚠️ **Remove or disable test keys**

---

## Getting Help

1. **Check logs** - Most issues show clear error messages
2. **Verify configuration** - Ensure all config files are correct
3. **Review documentation:**
   - [AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md)
   - [AUTH_QUICK_REF.md](AUTH_QUICK_REF.md)
4. **Test step-by-step:**
   - Start Provider first
   - Test Provider endpoints
   - Start Subscriber
   - Test Subscriber endpoints

---

**Last Updated:** March 9, 2026  
**Status:** All known issues documented and resolved
