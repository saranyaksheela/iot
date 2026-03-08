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
 * Test suite for authentication functionality in Subscriber service.
 */
@ExtendWith(VertxExtension.class)
class SubscriberAuthenticationTest {

  private Vertx vertx;
  private WebClient client;
  private static final int PORT = 8081;
  private static final String HOST = "localhost";
  
  // Valid API keys from auth-config.json
  private static final String CLIENT_APP_KEY = "sk_live_client_app_key_12345";
  private static final String MOBILE_APP_KEY = "sk_live_mobile_app_key_67890";
  private static final String DASHBOARD_KEY = "sk_live_dashboard_key_abcdef";
  private static final String TEST_CLIENT_KEY = "sk_test_development_client_key";
  private static final String DISABLED_CLIENT_KEY = "sk_disabled_old_client_key";
  
  // Invalid keys
  private static final String INVALID_API_KEY = "sk_invalid_key_12345";

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

  // ==================== Public Endpoints (No Auth Required) ====================
  
  @Test
  @DisplayName("Health endpoint should be accessible without API key")
  void testHealthEndpointNoAuth(VertxTestContext testContext) {
    client.get(PORT, HOST, "/health")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertNotNull(response.bodyAsJsonObject());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Status endpoint should be accessible without API key")
  void testStatusEndpointNoAuth(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/status")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("UP", body.getString("status"));
        assertEquals("subscriber", body.getString("service"));
        testContext.completeNow();
      })));
  }

  // ==================== Valid API Key Tests ====================
  
  @Test
  @DisplayName("Should access protected endpoint with valid client app key")
  void testValidClientAppKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", CLIENT_APP_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // If provider is not running, we expect 500, otherwise 201 or 400
        assertTrue(response.statusCode() == 201 || response.statusCode() == 400 || response.statusCode() == 500);
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should accept mobile app API key")
  void testMobileAppKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Mobile Test")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", MOBILE_APP_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should not return 401 - authentication passed
        assertNotEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should accept dashboard API key")
  void testDashboardKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Dashboard Test")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", DASHBOARD_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertNotEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should accept test client API key")
  void testTestClientKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Client")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", TEST_CLIENT_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertNotEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Missing API Key Tests ====================
  
  @Test
  @DisplayName("Should return 401 when API key is missing for devices endpoint")
  void testMissingApiKeyDevices(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Unauthorized", body.getString("error"));
        assertTrue(body.getString("message").contains("Invalid or missing API key"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for PUT request without API key")
  void testPutWithoutApiKey(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject()
      .put("deviceName", "Updated Name");

    client.put(PORT, HOST, "/subscriber/api/devices/1")
      .putHeader("content-type", "application/json")
      .sendJsonObject(updateData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for DELETE request without API key")
  void testDeleteWithoutApiKey(VertxTestContext testContext) {
    client.delete(PORT, HOST, "/subscriber/api/devices/1")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for telemetry endpoint without API key")
  void testTelemetryWithoutApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/telemetry/device/1")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for fetch endpoint without API key")
  void testFetchWithoutApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/fetch")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Invalid API Key Tests ====================
  
  @Test
  @DisplayName("Should return 401 with invalid API key")
  void testInvalidApiKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", INVALID_API_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Unauthorized", body.getString("error"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 with empty API key")
  void testEmptyApiKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", "")
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 with whitespace-only API key")
  void testWhitespaceApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/telemetry/device/1")
      .putHeader("X-API-Key", "   ")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Disabled API Key Tests ====================
  
  @Test
  @DisplayName("Should return 401 with disabled API key")
  void testDisabledApiKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", DISABLED_CLIENT_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== WWW-Authenticate Header Tests ====================
  
  @Test
  @DisplayName("Should include WWW-Authenticate header in 401 response")
  void testWWWAuthenticateHeader(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        assertNotNull(response.getHeader("WWW-Authenticate"));
        assertEquals("API-Key", response.getHeader("WWW-Authenticate"));
        testContext.completeNow();
      })));
  }

  // ==================== Multiple Endpoints with Same Key ====================
  
  @Test
  @DisplayName("Should use same API key for multiple endpoints")
  void testSameKeyMultipleEndpoints(VertxTestContext testContext) {
    // Test DELETE with valid key
    client.delete(PORT, HOST, "/subscriber/api/devices/1")
      .putHeader("X-API-Key", CLIENT_APP_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should not return 401 - authentication passed
        assertNotEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should authenticate telemetry request with valid key")
  void testTelemetryWithAuth(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/telemetry/device/1")
      .putHeader("X-API-Key", CLIENT_APP_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should not return 401 - authentication passed
        assertNotEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should authenticate fetch request with valid key")
  void testFetchWithAuth(VertxTestContext testContext) {
    client.get(PORT, HOST, "/subscriber/api/fetch")
      .putHeader("X-API-Key", CLIENT_APP_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should not return 401 - authentication passed
        assertNotEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Case Sensitivity Tests ====================
  
  @Test
  @DisplayName("API key should be case sensitive")
  void testApiKeyCaseSensitive(VertxTestContext testContext) {
    String uppercaseKey = CLIENT_APP_KEY.toUpperCase();
    
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", uppercaseKey)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Integration Tests ====================
  
  @Test
  @DisplayName("Should handle authentication failure before validation errors")
  void testAuthBeforeValidation(VertxTestContext testContext) {
    // Send invalid data but without API key
    JsonObject invalidData = new JsonObject(); // Missing required fields

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .sendJsonObject(invalidData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should return 401 (auth error) before checking validation
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should validate data after successful authentication")
  void testValidationAfterAuth(VertxTestContext testContext) {
    // Send invalid data with valid API key
    JsonObject invalidData = new JsonObject(); // Missing required fields

    client.post(PORT, HOST, "/subscriber/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", CLIENT_APP_KEY)
      .sendJsonObject(invalidData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Should pass authentication and return validation error (400)
        assertEquals(400, response.statusCode());
        testContext.completeNow();
      })));
  }
}
