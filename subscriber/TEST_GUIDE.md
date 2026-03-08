# Subscriber Service Test Guide

## Overview
This guide explains how to run the comprehensive test suite for the Subscriber service.

## Test Coverage

The test suite covers all Subscriber proxy API endpoints:

### 1. Health Check Tests
- ✅ Health endpoint (`/health`)
- ✅ Status endpoint (`/subscriber/api/status`)

### 2. Device Proxy Tests
- ✅ Proxy device creation to provider
- ✅ Empty body validation
- ✅ Missing body validation

### 3. Device Update Proxy Tests
- ✅ Proxy device update to provider
- ✅ Missing device ID validation
- ✅ Empty body validation
- ✅ Invalid device ID validation

### 4. Device Deletion Proxy Tests
- ✅ Proxy device deletion to provider
- ✅ Missing device ID validation
- ✅ Invalid device ID validation

### 5. Telemetry Proxy Tests
- ✅ Proxy telemetry fetch to provider
- ✅ Missing device ID validation
- ✅ Invalid device ID validation

### 6. Data Fetch Tests
- ✅ Fetch data from provider

### 7. Integration Tests
- ✅ Request timeout handling
- ✅ Content-type header validation
- ✅ Non-existent route handling (404)

## Prerequisites

1. **Java 17** or higher installed
2. **Maven** installed
3. **Provider service** (optional - some tests will pass without it)

## Running Tests

### Run All Tests
```bash
cd subscriber
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=SubscriberVerticleTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=SubscriberVerticleTest#testHealthEndpoint
```

### Run Tests with Verbose Output
```bash
mvn test -X
```

### Generate Test Report
```bash
mvn surefire-report:report
```
The report will be generated in `target/surefire-reports/`

## Test Modes

### Standalone Mode (Provider Not Running)
Most tests are designed to pass even without the Provider service running:
- Health check tests ✅
- Status endpoint tests ✅
- Input validation tests ✅

These tests verify that the Subscriber service:
- Starts correctly
- Validates input properly
- Returns appropriate error codes

### Integration Mode (Provider Running)
When the Provider service is running on `localhost:8080`, additional tests will pass:
- Device creation proxy ✅
- Device update proxy ✅
- Device deletion proxy ✅
- Telemetry data fetch proxy ✅

## Test Configuration

The tests use the following default configuration:
- **Subscriber Port:** 8081
- **Subscriber Host:** localhost
- **Provider Host:** localhost (configured in Constants)
- **Provider Port:** 8080 (configured in Constants)

## Running Integration Tests

For full integration testing:

1. **Start the Provider service:**
   ```bash
   cd provider
   mvn exec:java
   ```

2. **In another terminal, run Subscriber tests:**
   ```bash
   cd subscriber
   mvn test
   ```

## Test Results Interpretation

### Test Status Codes

When Provider is **NOT** running:
- `500 Internal Server Error` - Expected for proxy tests (connection refused)
- `400 Bad Request` - Validation errors caught by Subscriber
- `200 OK` - Local endpoints (health, status)

When Provider **IS** running:
- `200 OK` - Successful operations
- `201 Created` - Successful device creation
- `404 Not Found` - Resource not found
- `400 Bad Request` - Validation errors

## Important Notes

1. **Service Dependencies:** Some tests require the Provider service to be running. Tests are designed to gracefully handle Provider unavailability.

2. **Test Isolation:** The Subscriber service is stateless and acts as a proxy, so tests don't need database cleanup.

3. **Async Testing:** All tests use Vert.x's async testing framework with `VertxTestContext` for proper async handling.

4. **Network Timeouts:** Tests include timeout configurations to prevent hanging on Provider connection failures.

## Common Test Issues

### All Proxy Tests Returning 500
```
Status Code: 500
Error: Failed to connect to provider
```
**Solution:** This is expected if Provider is not running. Tests validate the error handling.

### Port Already in Use
```
Error: Address already in use (bind failed)
```
**Solution:** Stop any running instance of the Subscriber service or change the test port.

### WebClient Timeout
```
Error: Request timeout
```
**Solution:** Increase timeout in test configuration or check network connectivity.

## Continuous Integration

### CI Configuration Without Provider

```yaml
# Example GitHub Actions workflow
- name: Run Subscriber Tests (Standalone)
  run: |
    cd subscriber
    mvn clean test
```

### CI Configuration With Provider

```yaml
# Example GitHub Actions workflow
- name: Start Provider Service
  run: |
    cd provider
    mvn exec:java &
    sleep 10  # Wait for service to start

- name: Run Subscriber Tests (Integration)
  run: |
    cd subscriber
    mvn clean test
```

## Test Structure

```
SubscriberVerticleTest
├── Health Check Tests (Standalone)
├── Device Proxy Tests (Requires Provider)
├── Update Device Proxy Tests (Requires Provider)
├── Delete Device Proxy Tests (Requires Provider)
├── Telemetry Proxy Tests (Requires Provider)
├── Data Fetch Tests (Requires Provider)
└── Integration Tests (Standalone)
```

## Test Best Practices

1. **Graceful Degradation:** Tests should handle Provider unavailability gracefully
2. **Validation First:** Always validate input before proxying to Provider
3. **Error Handling:** Test both success and failure scenarios
4. **Status Codes:** Verify appropriate HTTP status codes
5. **Content Types:** Validate response content types

## Mock Testing (Advanced)

For unit testing without Provider dependency, consider using WireMock:

```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8</artifactId>
    <version>2.35.0</version>
    <scope>test</scope>
</dependency>
```

## Further Reading

- [Vert.x Testing Documentation](https://vertx.io/docs/vertx-junit5/java/)
- [Vert.x Web Client](https://vertx.io/docs/vertx-web-client/java/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [WireMock Documentation](http://wiremock.org/docs/)

## Quick Test Commands

```bash
# Run only health check tests
mvn test -Dtest=SubscriberVerticleTest#testHealthEndpoint,testStatusEndpoint

# Run only proxy tests
mvn test -Dtest=SubscriberVerticleTest#testCreateDeviceProxy*

# Skip tests during build
mvn package -DskipTests

# Run tests with coverage
mvn clean test jacoco:report
```
