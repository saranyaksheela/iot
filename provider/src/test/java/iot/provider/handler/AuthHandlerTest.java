package iot.provider.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthHandler.
 */
@ExtendWith({VertxExtension.class, MockitoExtension.class})
@DisplayName("AuthHandler Tests")
public class AuthHandlerTest {

    @Mock
    private RoutingContext routingContext;
    
    @Mock
    private HttpServerRequest request;
    
    @Mock
    private HttpServerResponse response;

    private AuthHandler authHandler;
    private JsonObject validAuthConfig;

    @BeforeEach
    void setUp() {
        // Setup valid auth configuration
        validAuthConfig = new JsonObject()
                .put("apiKeys", new JsonObject()
                        .put("test-key-1", new JsonObject()
                                .put("key", "valid-api-key-123")
                                .put("description", "Test API Key 1")
                                .put("enabled", true))
                        .put("test-key-2", new JsonObject()
                                .put("key", "valid-api-key-456")
                                .put("description", "Test API Key 2")
                                .put("enabled", false))
                        .put("test-key-3", new JsonObject()
                                .put("key", "expired-api-key-789")
                                .put("description", "Expired Test API Key")
                                .put("enabled", true)
                                .put("expiryTimestamp", System.currentTimeMillis() - 10000)) // Expired 10 seconds ago
                );

        authHandler = new AuthHandler(validAuthConfig);
        
        // Setup lenient mock behavior for common interactions
        lenient().when(routingContext.request()).thenReturn(request);
        lenient().when(routingContext.response()).thenReturn(response);
        lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
        lenient().when(response.putHeader(anyString(), anyString())).thenReturn(response);
        lenient().when(response.end(anyString())).thenReturn(null);
        lenient().when(request.remoteAddress()).thenReturn(null);
    }

  

   

   
    @Test
    @DisplayName("Should reject request with missing API key")
    void testMissingApiKey(VertxTestContext testContext) {
        when(request.getHeader("X-API-Key")).thenReturn(null);

        authHandler.authenticate(routingContext);

        verify(response).setStatusCode(401);
        verify(response).putHeader("content-type", "application/json");
        verify(response).putHeader("WWW-Authenticate", "API-Key");
        verify(response).end(any(String.class));
        verify(routingContext, never()).next();
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject request with empty API key")
    void testEmptyApiKey(VertxTestContext testContext) {
        when(request.getHeader("X-API-Key")).thenReturn("");

        authHandler.authenticate(routingContext);

        verify(response).setStatusCode(401);
        verify(response).putHeader("content-type", "application/json");
        verify(response).putHeader("WWW-Authenticate", "API-Key");
        verify(response).end(any(String.class));
        verify(routingContext, never()).next();
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject request with invalid API key")
    void testInvalidApiKey(VertxTestContext testContext) {
        when(request.getHeader("X-API-Key")).thenReturn("invalid-api-key");

        authHandler.authenticate(routingContext);

        verify(response).setStatusCode(401);
        verify(response).putHeader("content-type", "application/json");
        verify(response).putHeader("WWW-Authenticate", "API-Key");
        verify(response).end(any(String.class));
        verify(routingContext, never()).next();
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject request with disabled API key")
    void testDisabledApiKey(VertxTestContext testContext) {
        when(request.getHeader("X-API-Key")).thenReturn("valid-api-key-456"); // This key is disabled

        authHandler.authenticate(routingContext);

        verify(response).setStatusCode(401);
        verify(response).putHeader("content-type", "application/json");
        verify(response).putHeader("WWW-Authenticate", "API-Key");
        verify(response).end(any(String.class));
        verify(routingContext, never()).next();
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject request with expired API key")
    void testExpiredApiKey(VertxTestContext testContext) {
        when(request.getHeader("X-API-Key")).thenReturn("expired-api-key-789");

        authHandler.authenticate(routingContext);

        verify(response).setStatusCode(401);
        verify(response).putHeader("content-type", "application/json");
        verify(response).putHeader("WWW-Authenticate", "API-Key");
        verify(response).end(any(String.class));
        verify(routingContext, never()).next();
        
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should create proper error JSON for unauthorized requests")
    void testUnauthorizedErrorJson(VertxTestContext testContext) {
        when(request.getHeader("X-API-Key")).thenReturn("invalid-key");

        // Capture the JSON response
        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            // Verify error JSON structure
            assertTrue(responseObj.containsKey("error"), "Should contain error field");
            assertTrue(responseObj.containsKey("message"), "Should contain message field");
            assertTrue(responseObj.containsKey("statusCode"), "Should contain statusCode field");
            assertEquals("Unauthorized", responseObj.getString("error"));
            assertEquals(401, responseObj.getInteger("statusCode"));
            
            testContext.completeNow();
            return null;
        });

        authHandler.authenticate(routingContext);
    }

 

   
}