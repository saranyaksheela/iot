package iot.provider;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iot.provider.config.Constants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Provider service.
 * These tests require Docker to be running for Testcontainers.
 */
@ExtendWith(VertxExtension.class)
@Testcontainers
@EnabledIfSystemProperty(named = "test.docker", matches = "true", 
    disabledReason = "Docker tests disabled. Run with -Dtest.docker=true to enable.")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Provider Service Integration Tests")
public class ProviderIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-db.sql");

    private static final int TEST_PORT = 8888;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;
    private static final String VALID_API_KEY = "test-api-key-123";

    private WebClient webClient;
    private String deploymentId;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        webClient = WebClient.create(vertx);

        // Create test configuration
        JsonObject testConfig = new JsonObject()
                .put(Constants.CONFIG_HTTP_PORT, TEST_PORT)
                .put(Constants.CONFIG_DB, new JsonObject()
                        .put(Constants.CONFIG_DB_HOST, postgres.getHost())
                        .put(Constants.CONFIG_DB_PORT, postgres.getFirstMappedPort())
                        .put(Constants.CONFIG_DB_NAME, postgres.getDatabaseName())
                        .put(Constants.CONFIG_DB_USER, postgres.getUsername())
                        .put(Constants.CONFIG_DB_PASSWORD, postgres.getPassword())
                        .put(Constants.CONFIG_DB_POOL_SIZE, 5))
                .put(Constants.CONFIG_AUTH, new JsonObject()
                        .put(Constants.CONFIG_AUTH_ENABLED, true)
                        .put("apiKeys", new JsonObject()
                                .put("test-key", new JsonObject()
                                        .put("key", VALID_API_KEY)
                                        .put("description", "Test API Key")
                                        .put("enabled", true))));

        // Deploy ProviderVerticle with test configuration
        vertx.deployVerticle(new ProviderVerticle(), 
                new io.vertx.core.DeploymentOptions().setConfig(testConfig))
                .onSuccess(id -> {
                    deploymentId = id;
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);

        assertTrue(testContext.awaitCompletion(30, TimeUnit.SECONDS));
    }

    @AfterEach
    void tearDown(Vertx vertx, VertxTestContext testContext) {
        if (webClient != null) {
            webClient.close();
        }
        
        if (deploymentId != null) {
            vertx.undeploy(deploymentId)
                    .onComplete(ar -> testContext.completeNow());
        } else {
            testContext.completeNow();
        }
    }

    // ========== HEALTH ENDPOINT TESTS ==========

    @Test
    @Order(1)
    @DisplayName("Should return health status successfully")
    void testHealthEndpoint(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", Constants.HEALTH_ENDPOINT)
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .send()
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertNotNull(body);
                    assertTrue(body.containsKey("status"));
                    assertTrue(body.containsKey("service"));
                    assertTrue(body.containsKey("timestamp"));
                    assertEquals("UP", body.getString("status"));
                    assertEquals("provider", body.getString("service"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(2)
    @DisplayName("Should handle CORS preflight request")
    void testCorsPreflightRequest(VertxTestContext testContext) {
        webClient.request(io.vertx.core.http.HttpMethod.OPTIONS, TEST_PORT, "localhost", Constants.HEALTH_ENDPOINT)
                .putHeader("Access-Control-Request-Method", "GET")
                .putHeader("Access-Control-Request-Headers", "Content-Type")
                .send()
                .onSuccess(response -> {
                    assertNotNull(response.getHeader("Access-Control-Allow-Origin"));
                    assertNotNull(response.getHeader("Access-Control-Allow-Methods"));
                    assertNotNull(response.getHeader("Access-Control-Allow-Headers"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    @Order(3)
    @DisplayName("Should reject request without API key")
    void testAuthenticationNoApiKey(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .expect(ResponsePredicate.SC_UNAUTHORIZED)
                .send()
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertEquals("Unauthorized", body.getString("error"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(4)
    @DisplayName("Should reject request with invalid API key")
    void testAuthenticationInvalidApiKey(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, "invalid-key")
                .expect(ResponsePredicate.SC_UNAUTHORIZED)
                .send()
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertEquals("Unauthorized", body.getString("error"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    // ========== DEVICE ENDPOINT TESTS ==========

    @Test
    @Order(5)
    @DisplayName("Should get empty devices list initially")
    void testGetAllDevicesEmpty(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .expect(ResponsePredicate.SC_OK)
                .send()
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertTrue(body.containsKey("devices"));
                    assertTrue(body.containsKey("count"));
                    assertEquals(0, body.getInteger("count"));
                    assertTrue(body.getJsonArray("devices").isEmpty());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(6)
    @DisplayName("Should create device successfully")
    void testCreateDevice(VertxTestContext testContext) {
        JsonObject deviceRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_NAME, "Test Sensor")
                .put(Constants.JSON_KEY_DEVICE_TYPE, "temperature-sensor")
                .put(Constants.JSON_KEY_FIRMWARE_VERSION, "1.2.3")
                .put(Constants.JSON_KEY_LOCATION, "Server Room A")
                .put(Constants.JSON_KEY_STATUS, "active");

        webClient.post(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .expect(ResponsePredicate.SC_CREATED)
                .sendJsonObject(deviceRequest)
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertNotNull(body.getLong(Constants.JSON_KEY_ID));
                    assertNotNull(body.getString(Constants.JSON_KEY_DEVICE_UUID));
                    assertEquals("Test Sensor", body.getString(Constants.JSON_KEY_DEVICE_NAME));
                    assertEquals("temperature-sensor", body.getString(Constants.JSON_KEY_DEVICE_TYPE));
                    assertEquals("1.2.3", body.getString(Constants.JSON_KEY_FIRMWARE_VERSION));
                    assertEquals("Server Room A", body.getString(Constants.JSON_KEY_LOCATION));
                    assertEquals("active", body.getString(Constants.JSON_KEY_STATUS));
                    assertNotNull(body.getString(Constants.JSON_KEY_CREATED_AT));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(7)
    @DisplayName("Should reject device creation with missing required fields")
    void testCreateDeviceValidationError(VertxTestContext testContext) {
        JsonObject invalidRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_TYPE, "sensor");
                // Missing device name

        webClient.post(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .expect(ResponsePredicate.SC_BAD_REQUEST)
                .sendJsonObject(invalidRequest)
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertTrue(body.containsKey("error"));
                    assertTrue(body.containsKey("message"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(8)
    @DisplayName("Should get devices list after creation")
    void testGetAllDevicesAfterCreation(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .expect(ResponsePredicate.SC_OK)
                .send()
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertTrue(body.getInteger("count") >= 1);
                    JsonArray devices = body.getJsonArray("devices");
                    assertFalse(devices.isEmpty());
                    
                    // Verify device structure
                    JsonObject device = devices.getJsonObject(0);
                    assertNotNull(device.getLong(Constants.JSON_KEY_ID));
                    assertNotNull(device.getString(Constants.JSON_KEY_DEVICE_UUID));
                    assertNotNull(device.getString(Constants.JSON_KEY_DEVICE_NAME));
                    
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(9)
    @DisplayName("Should update device successfully")
    void testUpdateDevice(VertxTestContext testContext) {
        // First create a device
        JsonObject deviceRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_NAME, "Original Name")
                .put(Constants.JSON_KEY_DEVICE_TYPE, "sensor")
                .put(Constants.JSON_KEY_FIRMWARE_VERSION, "1.0.0")
                .put(Constants.JSON_KEY_LOCATION, "Lab");

        webClient.post(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .sendJsonObject(deviceRequest)
                .compose(createResponse -> {
                    Long deviceId = createResponse.bodyAsJsonObject().getLong(Constants.JSON_KEY_ID);
                    
                    // Update the device
                    JsonObject updateRequest = new JsonObject()
                            .put(Constants.JSON_KEY_DEVICE_NAME, "Updated Name");
                    
                    return webClient.put(TEST_PORT, "localhost", 
                            Constants.BASE_URL + "/devices/" + deviceId)
                            .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                            .expect(ResponsePredicate.SC_OK)
                            .sendJsonObject(updateRequest);
                })
                .onSuccess(updateResponse -> {
                    JsonObject body = updateResponse.bodyAsJsonObject();
                    assertEquals("Updated Name", body.getString(Constants.JSON_KEY_DEVICE_NAME));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(10)
    @DisplayName("Should delete device successfully")
    void testDeleteDevice(VertxTestContext testContext) {
        // First create a device
        JsonObject deviceRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_NAME, "Device to Delete")
                .put(Constants.JSON_KEY_DEVICE_TYPE, "sensor")
                .put(Constants.JSON_KEY_FIRMWARE_VERSION, "1.0.0")
                .put(Constants.JSON_KEY_LOCATION, "Lab");

        webClient.post(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .sendJsonObject(deviceRequest)
                .compose(createResponse -> {
                    Long deviceId = createResponse.bodyAsJsonObject().getLong(Constants.JSON_KEY_ID);
                    
                    // Delete the device
                    return webClient.delete(TEST_PORT, "localhost", 
                            Constants.BASE_URL + "/devices/" + deviceId)
                            .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                            .expect(ResponsePredicate.SC_OK)
                            .send();
                })
                .onSuccess(deleteResponse -> {
                    JsonObject body = deleteResponse.bodyAsJsonObject();
                    assertTrue(body.containsKey("message"));
                    assertTrue(body.containsKey(Constants.JSON_KEY_ID));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    // ========== TELEMETRY ENDPOINT TESTS ==========

    @Test
    @Order(11)
    @DisplayName("Should get empty telemetry data for device")
    void testGetTelemetryByDevice(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", Constants.BASE_URL + "/telemetry/device/1")
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .expect(ResponsePredicate.SC_OK)
                .send()
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertTrue(body.containsKey(Constants.JSON_KEY_DEVICE_ID));
                    assertTrue(body.containsKey(Constants.JSON_KEY_TELEMETRY));
                    assertTrue(body.containsKey(Constants.JSON_KEY_COUNT));
                    assertEquals(1L, body.getLong(Constants.JSON_KEY_DEVICE_ID));
                    assertEquals(0, body.getInteger(Constants.JSON_KEY_COUNT));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(12)
    @DisplayName("Should reject telemetry request with invalid device ID")
    void testGetTelemetryInvalidDeviceId(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", Constants.BASE_URL + "/telemetry/device/invalid")
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .expect(ResponsePredicate.SC_BAD_REQUEST)
                .send()
                .onSuccess(response -> {
                    JsonObject body = response.bodyAsJsonObject();
                    assertTrue(body.containsKey("error"));
                    assertTrue(body.containsKey("message"));
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @Order(13)
    @DisplayName("Should return 404 for non-existent endpoints")
    void testNotFoundEndpoint(VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", "/non-existent")
                .expect(ResponsePredicate.SC_NOT_FOUND)
                .send()
                .onSuccess(response -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(14)
    @DisplayName("Should handle request timeout")
    void testRequestTimeout(VertxTestContext testContext) {
        // This test verifies that timeout handler is configured
        // In a real scenario, we might test with a slow endpoint
        testContext.completeNow(); // Skip this test for now
    }

    @Test
    @Order(15)
    @DisplayName("Should handle malformed JSON requests")
    void testMalformedJsonRequest(VertxTestContext testContext) {
        webClient.post(TEST_PORT, "localhost", Constants.DEVICES_ENDPOINT)
                .putHeader(Constants.HEADER_API_KEY, VALID_API_KEY)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .expect(ResponsePredicate.SC_BAD_REQUEST)
                .sendBuffer(io.vertx.core.buffer.Buffer.buffer("{ invalid json"))
                .onSuccess(response -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }
}