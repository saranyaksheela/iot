# Running Authentication Tests - Quick Guide

## ✅ Configuration Files Added

The following configuration files have been added to test resources to ensure tests run successfully:

### Provider Test Resources
- `provider/src/test/resources/auth-config.json` ✅ Created
- `provider/src/test/resources/db-config.json` ✅ Created

### Subscriber Test Resources
- `subscriber/src/test/resources/auth-config.json` ✅ Created

## 🧪 Running the Tests

### Run Subscriber Authentication Tests

**From command line:**
```bash
cd subscriber
mvn test -Dtest=SubscriberAuthenticationTest
```

**Expected Output:**
```
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
```

### Run Provider Authentication Tests

**From command line:**
```bash
cd provider
mvn test -Dtest=ProviderAuthenticationTest
```

**Expected Output:**
```
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
```

### Run All Tests (Both Services)

**Provider:**
```bash
cd provider
mvn test
```

**Subscriber:**
```bash
cd subscriber
mvn test
```

## 🔍 What Was Fixed

### Issue
When running authentication tests, the verticles try to load configuration files from `src/main/resources/` which may not be accessible during test execution depending on the working directory.

### Solution
Created copies of all required configuration files in the test resources directories:
- `src/test/resources/auth-config.json` - Authentication configuration
- `src/test/resources/db-config.json` - Database configuration (Provider only)

Maven automatically includes files from both `src/main/resources/` and `src/test/resources/` in the test classpath, ensuring configuration files are accessible during test runs.

## 📋 Test Coverage

### Subscriber Authentication Tests (22 tests)
- ✅ 2 Public endpoint tests (no auth required)
- ✅ 4 Valid API key tests
- ✅ 5 Missing API key tests
- ✅ 3 Invalid API key tests
- ✅ 1 Disabled API key test
- ✅ 1 WWW-Authenticate header test
- ✅ 3 Multiple endpoint tests
- ✅ 1 Case sensitivity test
- ✅ 2 Integration tests

### Provider Authentication Tests (18 tests)
- ✅ 1 Public endpoint test (no auth required)
- ✅ 4 Valid API key tests
- ✅ 5 Missing API key tests
- ✅ 3 Invalid API key tests
- ✅ 1 Disabled API key test
- ✅ 1 WWW-Authenticate header test
- ✅ 3 Authenticated operation tests

## 🚀 Quick Test Commands

### Test Specific Scenario

**Test missing API key:**
```bash
mvn test -Dtest=SubscriberAuthenticationTest#testMissingApiKeyDevices
```

**Test valid API key:**
```bash
mvn test -Dtest=SubscriberAuthenticationTest#testValidClientAppKey
```

**Test disabled key:**
```bash
mvn test -Dtest=SubscriberAuthenticationTest#testDisabledApiKey
```

### Run Tests in IDE

Most IDEs (IntelliJ IDEA, Eclipse, VS Code) should now be able to run the tests directly:
1. Open the test file
2. Right-click on the test class or individual test method
3. Select "Run Test" or "Debug Test"

## 📊 Expected Test Results

### Subscriber Tests
All 22 tests should pass regardless of whether the Provider service is running. The tests are designed to:
- Test authentication logic independently
- Handle Provider unavailability gracefully (500 errors are expected when Provider is not running)
- Focus on validating authentication behavior

### Provider Tests
Tests require:
- ✅ Configuration files present (now fixed)
- ⚠️ PostgreSQL database running (for full functionality)
- ⚠️ Port 8080 available

**Note:** Some Provider tests may fail if database is not accessible, but authentication-specific tests should pass as they test the authentication layer before database access.

## 🐛 Troubleshooting

### Tests Still Fail to Load Config

If tests still can't find config files, verify Maven is building the test resources:
```bash
mvn clean test-compile
```

### Port Already in Use

If you see "Address already in use" errors:
1. Stop any running instances of the services
2. Kill processes using ports 8080/8081:
   ```bash
   # Windows
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F
   ```

### Database Connection Errors (Provider only)

Provider tests may show database errors if PostgreSQL is not running. This is expected and won't affect authentication tests specifically.

## ✅ Verification

To verify tests are working correctly:

```bash
# Run subscriber tests
cd subscriber
mvn clean test -Dtest=SubscriberAuthenticationTest

# You should see output like:
# [INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS
```

---

**Status:** ✅ Fixed and ready to run  
**Test Files Added:** 3 configuration files  
**Date:** March 9, 2026
