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
 * Test suite for authentication functionality in Provider service.
 */
@ExtendWith(VertxExtension.class)
class ProviderAuthenticationTest {

  private Vertx vertx;
  private WebClient client;
  private static final int PORT = 8080;
  private static final String HOST = "localhost";
  
  // Valid API keys from auth-config.json
  private static final String VALID_API_KEY = "pk_live_12345abcdef67890provider";
  private static final String SUBSCRIBER_API_KEY = "pk_live_subscriber987654321xyz";
  private static final String ADMIN_API_KEY = "pk_live_admin_secret_key_2026";
  private static final String TEST_API_KEY = "pk_test_development_only_key";
  private static final String DISABLED_API_KEY = "pk_disabled_old_key_example";
  
  // Invalid keys
  private static final String INVALID_API_KEY = "pk_invalid_key_12345";

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

  // ==================== Health Check Tests (No Auth Required) ====================
  
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

  // ==================== Valid API Key Tests ====================
  
  @Test
  @DisplayName("Should access protected endpoint with valid API key")
  void testValidApiKeyAccess(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should accept subscriber API key")
  void testSubscriberApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", SUBSCRIBER_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should accept admin API key")
  void testAdminApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", ADMIN_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should accept test API key")
  void testTestApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", TEST_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  // ==================== Missing API Key Tests ====================
  
  @Test
  @DisplayName("Should return 401 when API key is missing")
  void testMissingApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Unauthorized", body.getString("error"));
        assertTrue(body.getString("message").contains("Invalid or missing API key"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for POST request without API key")
  void testPostWithoutApiKey(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Test Device")
      .put("deviceType", "sensor");

    client.post(PORT, HOST, "/provider/api/devices")
      .putHeader("content-type", "application/json")
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Unauthorized", body.getString("error"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for PUT request without API key")
  void testPutWithoutApiKey(VertxTestContext testContext) {
    JsonObject updateData = new JsonObject()
      .put("deviceName", "Updated Name");

    client.put(PORT, HOST, "/provider/api/devices/1")
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
    client.delete(PORT, HOST, "/provider/api/devices/1")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for telemetry endpoint without API key")
  void testTelemetryWithoutApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/telemetry/device/1")
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
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", INVALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Unauthorized", body.getString("error"));
        assertTrue(body.getString("message").contains("Invalid or missing API key"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 with empty API key")
  void testEmptyApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", "")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 with whitespace-only API key")
  void testWhitespaceApiKey(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
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
    client.get(PORT, HOST, "/provider/api/devices")
      .putHeader("X-API-Key", DISABLED_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Unauthorized", body.getString("error"));
        testContext.completeNow();
      })));
  }

  // ==================== WWW-Authenticate Header Tests ====================
  
  @Test
  @DisplayName("Should include WWW-Authenticate header in 401 response")
  void testWWWAuthenticateHeader(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/devices")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        assertNotNull(response.getHeader("WWW-Authenticate"));
        assertEquals("API-Key", response.getHeader("WWW-Authenticate"));
        testContext.completeNow();
      })));
  }

  // ==================== Successful Authenticated Operations ====================
  
  @Test
  @DisplayName("Should create device with valid API key")
  void testCreateDeviceWithAuth(VertxTestContext testContext) {
    JsonObject deviceData = new JsonObject()
      .put("deviceName", "Auth Test Sensor")
      .put("deviceType", "temperature")
      .put("firmwareVersion", "1.0.0")
      .put("location", "Lab 1");

    client.post(PORT, HOST, "/provider/api/devices")
      .putHeader("content-type", "application/json")
      .putHeader("X-API-Key", VALID_API_KEY)
      .sendJsonObject(deviceData)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(201, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertEquals("Auth Test Sensor", body.getString("deviceName"));
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should get telemetry with valid API key")
  void testGetTelemetryWithAuth(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/telemetry/device/1")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
        assertNotNull(body.getJsonArray("telemetry"));
        assertNotNull(body.getInteger("count"));
        testContext.completeNow();
      })));
  }

  // ==================== Data Endpoint Tests ====================
  
  @Test
  @DisplayName("Should access data endpoint with valid API key")
  void testDataEndpointWithAuth(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/data")
      .putHeader("X-API-Key", VALID_API_KEY)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  @DisplayName("Should return 401 for data endpoint without API key")
  void testDataEndpointWithoutAuth(VertxTestContext testContext) {
    client.get(PORT, HOST, "/provider/api/data")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(401, response.statusCode());
        testContext.completeNow();
      })));
  }
}
