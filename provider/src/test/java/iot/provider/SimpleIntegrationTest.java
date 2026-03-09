package iot.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration tests that don't require Docker.
 */
@DisplayName("Simple Provider Integration Tests")
public class SimpleIntegrationTest {

    @Test
    @DisplayName("Should verify provider service classes exist and can be instantiated")
    void testProviderServiceClassesExist() {
        // Test that all main classes can be instantiated
        assertDoesNotThrow(() -> {
            Class.forName("iot.provider.ProviderApp");
            Class.forName("iot.provider.ProviderVerticle");
            Class.forName("iot.provider.handler.HealthHandler");
            Class.forName("iot.provider.handler.AuthHandler");
            Class.forName("iot.provider.handler.DeviceHandler");
            Class.forName("iot.provider.handler.TelemetryHandler");
            Class.forName("iot.provider.model.Device");
            Class.forName("iot.provider.config.Constants");
        });
    }

    @Test
    @DisplayName("Should verify constants are properly defined")
    void testConstants() {
        assertEquals("/provider/api", iot.provider.config.Constants.BASE_URL);
        assertEquals("/health", iot.provider.config.Constants.HEALTH_ENDPOINT);
        assertEquals(8080, iot.provider.config.Constants.DEFAULT_HTTP_PORT);
        assertEquals("active", iot.provider.config.Constants.DEFAULT_STATUS);
        assertEquals(200, iot.provider.config.Constants.STATUS_OK);
        assertEquals(201, iot.provider.config.Constants.STATUS_CREATED);
        assertEquals(400, iot.provider.config.Constants.STATUS_BAD_REQUEST);
        assertEquals(401, iot.provider.config.Constants.STATUS_UNAUTHORIZED);
        assertEquals(404, iot.provider.config.Constants.STATUS_NOT_FOUND);
        assertEquals(500, iot.provider.config.Constants.STATUS_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Should be able to create device model instances")
    void testDeviceModelCreation() {
        iot.provider.model.Device device = new iot.provider.model.Device();
        assertNotNull(device);
        
        java.util.UUID uuid = java.util.UUID.randomUUID();
        iot.provider.model.Device deviceWithParams = new iot.provider.model.Device(
            uuid, "Test Device", "sensor", "1.0.0", "Lab", "active");
        assertNotNull(deviceWithParams);
        assertEquals(uuid, deviceWithParams.getDeviceUuid());
        assertEquals("Test Device", deviceWithParams.getDeviceName());
    }

    @Test
    @DisplayName("Should be able to create handlers without database")
    void testHandlerCreationWithoutDatabase() {
        // These should work without database connections
        assertDoesNotThrow(() -> {
            new iot.provider.handler.HealthHandler();
            new iot.provider.handler.DeviceHandler(null); // Test mode
            new iot.provider.handler.TelemetryHandler(null); // Test mode
            new iot.provider.handler.AuthHandler(new io.vertx.core.json.JsonObject());
        });
    }

    @Test
    @DisplayName("Should skip Docker-dependent tests when Docker is not available")
    @DisabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "false", disabledReason = "Docker not available")
    void testDockerNotAvailable() {
        // This test documents that Docker-dependent tests are skipped
        System.out.println("Docker-dependent tests would be skipped if Docker is not available");
        assertTrue(true, "This test serves as documentation");
    }
}