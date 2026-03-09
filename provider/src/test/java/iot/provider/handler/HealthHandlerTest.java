package iot.provider.handler;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iot.provider.config.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthHandler.
 */
@ExtendWith({VertxExtension.class, MockitoExtension.class})
@DisplayName("HealthHandler Tests")
public class HealthHandlerTest {

    @Mock
    private RoutingContext routingContext;
    
    @Mock
    private HttpServerResponse response;

    private HealthHandler healthHandler;

    @BeforeEach
    void setUp() {
        healthHandler = new HealthHandler();
        
        // Setup lenient mock behavior for common interactions
        lenient().when(routingContext.response()).thenReturn(response);
        lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
        lenient().when(response.putHeader(anyString(), anyString())).thenReturn(response);
        lenient().when(response.end(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("Should handle health check request successfully")
    void testHealthCheck(VertxTestContext testContext) {
        // Execute health check
        healthHandler.healthCheck(routingContext);

        // Verify response matches actual implementation
        verify(response).setStatusCode(Constants.STATUS_OK);
        verify(response).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        
        // Verify that end() was called with a JSON response
        verify(response).end(any(String.class));
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should return proper JSON structure for health check")
    void testHealthCheckJsonStructure(VertxTestContext testContext) {
        // Capture the JSON response
        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            // Verify JSON structure matches actual implementation
            assert responseObj.containsKey("status");
            assert "UP".equals(responseObj.getString("status"));
            // Note: HealthHandler only returns status, not service or timestamp
            
            testContext.completeNow();
            return null;
        });

        healthHandler.healthCheck(routingContext);
    }

    @Test
    @DisplayName("Should set correct headers")
    void testHealthCheckHeaders(VertxTestContext testContext) {
        healthHandler.healthCheck(routingContext);

        verify(response).setStatusCode(Constants.STATUS_OK);
        verify(response).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should handle multiple health check requests")
    void testMultipleHealthCheckRequests(VertxTestContext testContext) {
        // Execute multiple health checks
        healthHandler.healthCheck(routingContext);
        healthHandler.healthCheck(routingContext);
        healthHandler.healthCheck(routingContext);

        // Verify response was called multiple times
        verify(response, times(3)).setStatusCode(Constants.STATUS_OK);
        verify(response, times(3)).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        verify(response, times(3)).end(any(String.class));
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should use constants for status code and content type")
    void testConstantsUsage(VertxTestContext testContext) {
        healthHandler.healthCheck(routingContext);

        verify(response).setStatusCode(200); // STATUS_OK
        verify(response).putHeader("content-type", "application/json"); // HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should handle getData request successfully")
    void testGetData(VertxTestContext testContext) {
        // Execute getData
        healthHandler.getData(routingContext);

        // Verify response
        verify(response).setStatusCode(Constants.STATUS_OK);
        verify(response).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        verify(response).end(any(String.class));
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should return proper JSON structure for getData")
    void testGetDataJsonStructure(VertxTestContext testContext) {
        // Capture the JSON response
        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            // Verify JSON structure for getData
            assert responseObj.containsKey("message");
            assert responseObj.containsKey("timestamp");
            assert "Hello from Provider Service".equals(responseObj.getString("message"));
            assert responseObj.getLong("timestamp") != null;
            
            testContext.completeNow();
            return null;
        });

        healthHandler.getData(routingContext);
    }
}