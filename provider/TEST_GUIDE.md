# Provider Service Test Guide

## Overview
This guide explains how to run the comprehensive test suite for the Provider service.

## Test Coverage

The test suite covers all Provider API endpoints:

### 1. Health Check Tests
- ✅ Health endpoint (`/health`)
- ✅ Data endpoint (`/provider/api/data`)

### 2. Device Creation Tests
- ✅ Create device with valid data
- ✅ Missing device name validation
- ✅ Missing device type validation
- ✅ Device name length validation (max 100 chars)
- ✅ Empty request body validation

### 3. Device Retrieval Tests
- ✅ Get all devices

### 4. Device Update Tests
- ✅ Missing device name validation
- ✅ Invalid device ID format validation
- ✅ Non-existent device (404) validation
- ✅ Device name length validation on update

### 5. Device Deletion Tests
- ✅ Invalid device ID format validation
- ✅ Non-existent device (404) validation

### 6. Telemetry Data Tests
- ✅ Invalid device ID format validation
- ✅ Retrieve telemetry by device ID
- ✅ Empty telemetry array for device with no data

## Prerequisites

1. **Java 17** or higher installed
2. **Maven** installed
3. **PostgreSQL database** running with required tables:
   - `devices` table
   - `telemetry_data` table
4. **Database configuration** properly set in `src/main/resources/db-config.json`

## Running Tests

### Run All Tests
```bash
cd provider
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=ProviderVerticleTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=ProviderVerticleTest#testCreateDeviceSuccess
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

## Test Database Setup

Before running tests, ensure your test database has the following schema:

```sql
-- Devices table
CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_uuid UUID UNIQUE NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    firmware_version VARCHAR(50),
    location VARCHAR(100),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Topics table (for telemetry reference)
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    topic_name VARCHAR(255) NOT NULL
);

-- Telemetry data table
CREATE TABLE telemetry_data (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id),
    topic_id BIGINT REFERENCES topics(id),
    payload JSONB,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Test Configuration

The tests use the following default configuration:
- **Port:** 8080
- **Host:** localhost

These can be overridden in the test setup if needed.

## Important Notes

1. **Database State:** Some tests may fail if the database already contains data. Consider using a dedicated test database or cleaning data between test runs.

2. **Test Isolation:** Tests are designed to be independent but may interact with the same database. For production testing, consider implementing test data cleanup in `@AfterEach`.

3. **Async Testing:** All tests use Vert.x's async testing framework with `VertxTestContext` for proper async handling.

4. **Timeout:** Default test timeout is 30 seconds per test. Adjust if needed for slower systems.

## Common Test Failures

### Connection Refused
```
Error: Connection refused
```
**Solution:** Ensure PostgreSQL is running and accessible with correct credentials.

### Table Not Found
```
Error: relation "devices" does not exist
```
**Solution:** Run the database schema creation script above.

### Port Already in Use
```
Error: Address already in use
```
**Solution:** Stop any running instance of the Provider service or change the test port.

## Continuous Integration

To integrate with CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Run Provider Tests
  run: |
    cd provider
    mvn clean test
```

## Test Best Practices

1. Always clean up test data after tests
2. Use meaningful test names with `@DisplayName`
3. Test both success and failure scenarios
4. Validate response status codes and body content
5. Mock external dependencies when possible

## Further Reading

- [Vert.x Testing Documentation](https://vertx.io/docs/vertx-junit5/java/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
