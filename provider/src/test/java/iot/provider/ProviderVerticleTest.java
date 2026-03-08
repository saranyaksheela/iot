package iot.provider;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Provider API endpoints.
 * Tests all CRUD operations for devices and telemetry data retrieval.
 */
@ExtendWith(VertxExtension.class)
class ProviderVerticleTest {

  private Vertx vertx;
  private WebClient client;
  private static final int PORT = 8080;
  private static final String HOST = "localhost";
  
  // Valid API key for testing (from auth-config.json)
  private static final String VALID_API_KEY = "pk_live_12345abcdef67890provider";

  @BeforeEach
  void setup(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new ProviderVerticle(), testContext.succeedingThenComplete());
    client = WebClient.create(vertx);
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    if (client != null) {
      client.close();
    }
    vertx.close(testContext.succeedingThenComplete());
  }

  // ==================== Health Check Tests ====================
  
  @Test
  @DisplayName("Should return 200 OK for health check endpoint")
  void testHealthEndpoint(VertxTestContext testContext) {
    client.get(PORT, HOST, "/health")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertNotNull(response.bodyAsJsonObject());
        assertEquals("UP", response.bodyAsJsonObject().getString("status"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 200 OK for data endpoint")
  void testDataEndpoint(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/data")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.getHeader("content-type"));
        testContext.completeNow();
      })));
  }

  // ==================== Device Creation Tests ====================
  
  @Test
  @DisplayName("Should create device with valid data")
  void testCreateDeviceSuccess(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Sensor")
      .put("deviceType", "temperature")
      .put("firmwareVersion", "1.0.0")
      .put("location", "Lab 1")
      .put("status", "active");

    client.post(PORT, HOST, "/provider/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(201, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertNotNull(body.getLong("id"));
        assertNotNull(body.getString("deviceUuid"));
        assertEquals("Test Sensor", body.getString("deviceName"));
        assertEquals("temperature", body.getString("deviceType"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when device name is missing")
  void testCreateDeviceMissingName(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceType", "sensor")
      .put("firmwareVersion", "1.0.0");

    client.post(PORT, HOST, "/provider/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Bad Request", body.getString("error"));
        assertTrue(body.getString("message").contains("Device name"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when device type is missing")
  void testCreateDeviceMissingType(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("firmwareVersion", "1.0.0");

    client.post(PORT, HOST, "/provider/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Bad Request", body.getString("error"));
        assertTrue(body.getString("message").contains("Device type"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when device name exceeds max length")
  void testCreateDeviceNameTooLong(VertxTestContext testContext) {
    String longName = "a".repeat(101); // 101 characters
    JsonObject deviceData = new JsonObject()
      .put("deviceName", longName)
      .put("deviceType", "sensor")
      .put("firmwareVersion", "1.0.0");

    client.post(PORT, HOST, "/provider/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertTrue(body.getString("message").contains("100 characters"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when request body is empty")
  void testCreateDeviceEmptyBody(VertxTestContext testContext) {
    client.post(PORT, HOST, "/provider/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(new JsonObject())
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Get All Devices Tests ====================
  
  @Test
  @DisplayName("Should retrieve all devices")
  void testGetAllDevices(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertNotNull(body.getJsonArray("devices"));
        assertNotNull(body.getInteger("count"));
        testContext.completeNow();
      })));
  }

  // ==================== Update Device Tests ====================
  
  @Test
  @DisplayName("Should return 400 when updating with missing device name")
  void testUpdateDeviceMissingName(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject();

    client.put(PORT, HOST, "/provider/api/devices/1")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertTrue(body.getString("message").contains("Device name"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when updating with invalid device ID")
  void testUpdateDeviceInvalidId(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject()
      .put("deviceName", "Updated Name");

    client.put(PORT, HOST, "/provider/api/devices/invalid")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertTrue(body.getString("message").contains("Invalid device ID"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent device")
  void testUpdateDeviceNotFound(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject()
      .put("deviceName", "Updated Name");

    client.put(PORT, HOST, "/provider/api/devices/999999")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(404, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Not Found", body.getString("error"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when updating with name exceeding max length")
  void testUpdateDeviceNameTooLong(VertxTestContext testContext) {
    String longName = "a".repeat(101);
    JsonObject updateData = new JsonObject()
      .put("deviceName", longName);

    client.put(PORT, HOST, "/provider/api/devices/1")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertTrue(body.getString("message").contains("100 characters"));
        testContext.completeNow();
      })));
  }

  // ==================== Delete Device Tests ====================
  
  @Test
  @DisplayName("Should return 400 when deleting with invalid device ID")
  void testDeleteDeviceInvalidId(VertxTestContext testContext) {
    client.delete(PORT, HOST, "/provider/api/devices/invalid")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertTrue(body.getString("message").contains("Invalid device ID"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent device")
  void testDeleteDeviceNotFound(VertxTestContext testContext) {
    client.delete(PORT, HOST, "/provider/api/devices/999999")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(404, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Not Found", body.getString("error"));
        testContext.completeNow();
      })));
  }

  // ==================== Telemetry Data Tests ====================
  
  @Test
  @DisplayName("Should return 400 when fetching telemetry with invalid device ID")
  void testGetTelemetryInvalidDeviceId(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/telemetry/device/invalid")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertTrue(body.getString("message").contains("Invalid device ID"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should retrieve telemetry data for valid device ID")
  void testGetTelemetryByDeviceId(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/telemetry/device/1")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertNotNull(body.getLong("deviceId"));
        assertNotNull(body.getJsonArray("telemetry"));
        assertNotNull(body.getInteger("count"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return empty array for device with no telemetry data")
  void testGetTelemetryNoData(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/telemetry/device/999999")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals(999999, body.getLong("deviceId"));
        assertEquals(0, body.getJsonArray("telemetry").size());
        assertEquals(0, body.getInteger("count"));
        testContext.completeNow();
      })));
  }
}
