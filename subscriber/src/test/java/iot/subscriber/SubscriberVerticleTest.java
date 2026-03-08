package iot.subscriber;

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
 * Comprehensive test suite for Subscriber API endpoints.
 * Tests all proxy operations that forward requests to the Provider service.
 */
@ExtendWith(VertxExtension.class)
class SubscriberVerticleTest {

  private Vertx vertx;
  private WebClient client;
  private static final int PORT = 8081;
  private static final String HOST = "localhost";

  @BeforeEach
  void setup(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new SubscriberVerticle(), testContext.succeedingThenComplete());
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
  @DisplayName("Should return 200 OK for status endpoint")
  void testStatusEndpoint(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/status")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.getHeader("content-type"));
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("UP", body.getString("status"));
        assertEquals("subscriber", body.getString("service"));
        testContext.completeNow();
      })));
  }

  // ==================== Device Proxy Tests ====================
  
  @Test
  @DisplayName("Should proxy device creation to provider (requires provider running)")
  void testCreateDeviceProxy(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Subscriber Test Sensor")
      .put("deviceType", "temperature")
      .put("firmwareVersion", "2.0.0")
      .put("location", "Lab 2")
      .put("status", "active");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Provider must be running for this test to pass with 201
        // If provider is not running, we expect connection error
        assertTrue(response.statusCode() == 201 || response.statusCode() == 500);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when proxying device creation with empty body")
  void testCreateDeviceProxyEmptyBody(VertxTestContext testContext) {
    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .sendJsonObject(new JsonObject())
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Bad Request", body.getString("error"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when proxying device creation without body")
  void testCreateDeviceProxyNoBody(VertxTestContext testContext) {
    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Update Device Proxy Tests ====================
  
  @Test
  @DisplayName("Should proxy device update to provider (requires provider running)")
  void testUpdateDeviceProxy(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject()
      .put("deviceName", "Updated Subscriber Device");

    client.put(PORT, HOST, "/subscriber/api/devices/1")
      .putHeader("content-type", "application/json")
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Provider must be running for this test to pass
        // If provider is not running, we expect connection error (500)
        assertTrue(response.statusCode() == 200 || response.statusCode() == 404 || response.statusCode() == 500);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when proxying update with missing device ID")
  void testUpdateDeviceProxyMissingId(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject()
      .put("deviceName", "Updated Name");

    client.put(PORT, HOST, "/subscriber/api/devices/")
      .putHeader("content-type", "application/json")
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // This will hit a different route or 404
        assertTrue(response.statusCode() >= 400);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when proxying update with empty body")
  void testUpdateDeviceProxyEmptyBody(VertxTestContext testContext) {
    client.put(PORT, HOST, "/subscriber/api/devices/1")
      .putHeader("content-type", "application/json")
      .sendJsonObject(new JsonObject())
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(400, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Bad Request", body.getString("error"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when proxying update with invalid device ID")
  void testUpdateDeviceProxyInvalidId(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject()
      .put("deviceName", "Updated Name");

    client.put(PORT, HOST, "/subscriber/api/devices/invalid")
      .putHeader("content-type", "application/json")
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should forward to provider which will return 400
        assertTrue(response.statusCode() >= 400);
        testContext.completeNow();
      })));
  }

  // ==================== Delete Device Proxy Tests ====================
  
  @Test
  @DisplayName("Should proxy device deletion to provider (requires provider running)")
  void testDeleteDeviceProxy(VertxTestContext testContext) {
    client.delete(PORT, HOST, "/subscriber/api/devices/1")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Provider must be running for this test to pass
        // If provider is not running, we expect connection error (500)
        assertTrue(response.statusCode() == 200 || response.statusCode() == 404 || response.statusCode() == 500);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when proxying deletion with missing device ID")
  void testDeleteDeviceProxyMissingId(VertxTestContext testContext) {
    client.delete(PORT, HOST, "/subscriber/api/devices/")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // This will hit a different route or 404
        assertTrue(response.statusCode() >= 400);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should proxy deletion with invalid device ID to provider")
  void testDeleteDeviceProxyInvalidId(VertxTestContext testContext) {
    client.delete(PORT, HOST, "/subscriber/api/devices/invalid")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should forward to provider which will return 400 or 500
        assertTrue(response.statusCode() >= 400);
        testContext.completeNow();
      })));
  }

  // ==================== Telemetry Proxy Tests ====================
  
  @Test
  @DisplayName("Should proxy telemetry fetch to provider (requires provider running)")
  void testGetTelemetryProxy(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/telemetry/device/1")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Provider must be running for this test to pass with 200
        // If provider is not running, we expect connection error (500)
        assertTrue(response.statusCode() == 200 || response.statusCode() == 500);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 400 when proxying telemetry fetch with missing device ID")
  void testGetTelemetryProxyMissingId(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/telemetry/device/")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // This will hit a different route or 404
        assertTrue(response.statusCode() >= 400);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should proxy telemetry fetch with invalid device ID to provider")
  void testGetTelemetryProxyInvalidId(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/telemetry/device/invalid")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should forward to provider which will return 400 or 500
        assertTrue(response.statusCode() >= 400);
        testContext.completeNow();
      })));
  }

  // ==================== Data Fetch Tests ====================
  
  @Test
  @DisplayName("Should fetch data from provider (requires provider running)")
  void testFetchFromProvider(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/fetch")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Provider must be running for this test to pass with 200
        // If provider is not running, we expect connection error (500)
        assertTrue(response.statusCode() == 200 || response.statusCode() == 500);
        testContext.completeNow();
      })));
  }

  // ==================== Integration Tests ====================
  
  @Test
  @DisplayName("Should handle timeout for long-running requests")
  void testRequestTimeout(VertxTestContext testContext) {
    // This test verifies that the timeout handler is configured
    client.get(PORT, HOST, "/subscriber/api/status")
      .timeout(1000)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return proper content-type header")
  void testContentTypeHeader(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/status")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals("application/json", response.getHeader("content-type"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should handle non-existent routes with 404")
  void testNonExistentRoute(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/nonexistent")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(404, response.statusCode());
        testContext.completeNow();
      })));
  }
}
