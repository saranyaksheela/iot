package iot.provider.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import iot.provider.config.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TelemetryHandler.
 */
@ExtendWith({VertxExtension.class, MockitoExtension.class})
@DisplayName("TelemetryHandler Tests")
public class TelemetryHandlerTest {

    @Mock
    private RoutingContext routingContext;
    
    @Mock
    private HttpServerRequest request;
    
    @Mock
    private HttpServerResponse response;
    
    @Mock
    private Pool pool;
    
    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;
    
    @Mock
    private RowSet<Row> rowSet;
    
    @Mock
    private RowIterator<Row> rowIterator;
    
    @Mock
    private Row row;

    private TelemetryHandler telemetryHandler;
    private TelemetryHandler testModeHandler;

    @BeforeEach
    void setUp() {
        telemetryHandler = new TelemetryHandler(pool);
        testModeHandler = new TelemetryHandler(null); // Test mode without database
        
        // Setup lenient mock behavior for common interactions
        lenient().when(routingContext.request()).thenReturn(request);
        lenient().when(routingContext.response()).thenReturn(response);
        lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
        lenient().when(response.putHeader(anyString(), anyString())).thenReturn(response);
        lenient().when(response.end(anyString())).thenReturn(null);
    }

    // ========== GET TELEMETRY BY DEVICE TESTS ==========

    @Test
    @DisplayName("Should return empty telemetry data in test mode")
    void testGetTelemetryByDeviceTestMode(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn("1");

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            // Verify response structure
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_DEVICE_ID));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_TELEMETRY));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_COUNT));
            assertEquals(1L, responseObj.getLong(Constants.JSON_KEY_DEVICE_ID));
            assertEquals(0, responseObj.getInteger(Constants.JSON_KEY_COUNT));
            assertTrue(responseObj.getJsonArray(Constants.JSON_KEY_TELEMETRY).isEmpty());
            
            testContext.completeNow();
            return null;
        });

        testModeHandler.getTelemetryByDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_OK);
        verify(response).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
    }

    @Test
    @DisplayName("Should reject request with missing device ID")
    void testGetTelemetryMissingDeviceId(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn(null);

        testModeHandler.getTelemetryByDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        verify(response).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject request with empty device ID")
    void testGetTelemetryEmptyDeviceId(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn("");

        testModeHandler.getTelemetryByDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject request with invalid device ID format")
    void testGetTelemetryInvalidDeviceId(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn("not-a-number");

        testModeHandler.getTelemetryByDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should handle successful telemetry data retrieval")
    void testGetTelemetryByDeviceSuccess(VertxTestContext testContext) {
        long deviceId = 123L;
        when(routingContext.pathParam("deviceId")).thenReturn(String.valueOf(deviceId));
        
        // Mock database response
        when(pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.next()).thenReturn(row);
        
        // Mock row data
        when(row.getLong(Constants.DB_COL_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_DEVICE_ID)).thenReturn(deviceId);
        when(row.getLong(Constants.DB_COL_TOPIC_ID)).thenReturn(100L);
        when(row.getLocalDateTime(Constants.DB_COL_RECEIVED_AT)).thenReturn(LocalDateTime.now());
        when(row.getValue(Constants.DB_COL_PAYLOAD)).thenReturn(new JsonObject().put("temperature", 25.5));

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            assertEquals(deviceId, responseObj.getLong(Constants.JSON_KEY_DEVICE_ID));
            JsonArray telemetryData = responseObj.getJsonArray(Constants.JSON_KEY_TELEMETRY);
            assertEquals(1, telemetryData.size());
            assertEquals(1, responseObj.getInteger(Constants.JSON_KEY_COUNT));
            
            // Verify telemetry structure
            JsonObject telemetry = telemetryData.getJsonObject(0);
            assertTrue(telemetry.containsKey(Constants.JSON_KEY_ID));
            assertTrue(telemetry.containsKey(Constants.JSON_KEY_DEVICE_ID));
            assertTrue(telemetry.containsKey(Constants.JSON_KEY_TOPIC_ID));
            assertTrue(telemetry.containsKey(Constants.JSON_KEY_PAYLOAD));
            assertTrue(telemetry.containsKey(Constants.JSON_KEY_RECEIVED_AT));
            
            testContext.completeNow();
            return null;
        });

        telemetryHandler.getTelemetryByDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_OK);
    }

    @Test
    @DisplayName("Should handle database error during telemetry retrieval")
    void testGetTelemetryByDeviceDbError(VertxTestContext testContext) {
        Exception dbError = new RuntimeException("Database connection failed");
        
        when(routingContext.pathParam("deviceId")).thenReturn("1");
        when(pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.failedFuture(dbError));

        telemetryHandler.getTelemetryByDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_INTERNAL_SERVER_ERROR);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should handle string payload correctly")
    void testGetTelemetryWithStringPayload(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn("1");
        
        // Mock database response with string payload
        when(pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.next()).thenReturn(row);
        
        // Mock row data with string payload
        when(row.getLong(Constants.DB_COL_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_DEVICE_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_TOPIC_ID)).thenReturn(100L);
        when(row.getLocalDateTime(Constants.DB_COL_RECEIVED_AT)).thenReturn(LocalDateTime.now());
        when(row.getValue(Constants.DB_COL_PAYLOAD)).thenReturn("{\"temperature\": 25.5}");

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            JsonArray telemetryData = responseObj.getJsonArray(Constants.JSON_KEY_TELEMETRY);
            JsonObject telemetry = telemetryData.getJsonObject(0);
            JsonObject payload = telemetry.getJsonObject(Constants.JSON_KEY_PAYLOAD);
            
            assertNotNull(payload);
            assertEquals(25.5, payload.getDouble("temperature"));
            
            testContext.completeNow();
            return null;
        });

        telemetryHandler.getTelemetryByDevice(routingContext);
    }

    @Test
    @DisplayName("Should handle invalid JSON payload as string")
    void testGetTelemetryWithInvalidJsonPayload(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn("1");
        
        // Mock database response with invalid JSON string payload
        when(pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.next()).thenReturn(row);
        
        // Mock row data with invalid JSON string
        when(row.getLong(Constants.DB_COL_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_DEVICE_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_TOPIC_ID)).thenReturn(100L);
        when(row.getLocalDateTime(Constants.DB_COL_RECEIVED_AT)).thenReturn(LocalDateTime.now());
        when(row.getValue(Constants.DB_COL_PAYLOAD)).thenReturn("invalid-json-string");

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            JsonArray telemetryData = responseObj.getJsonArray(Constants.JSON_KEY_TELEMETRY);
            JsonObject telemetry = telemetryData.getJsonObject(0);
            String payload = telemetry.getString(Constants.JSON_KEY_PAYLOAD);
            
            assertEquals("invalid-json-string", payload);
            
            testContext.completeNow();
            return null;
        });

        telemetryHandler.getTelemetryByDevice(routingContext);
    }

    @Test
    @DisplayName("Should handle null payload")
    void testGetTelemetryWithNullPayload(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn("1");
        
        // Mock database response with null payload
        when(pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.next()).thenReturn(row);
        
        // Mock row data with null payload
        when(row.getLong(Constants.DB_COL_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_DEVICE_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_TOPIC_ID)).thenReturn(100L);
        when(row.getLocalDateTime(Constants.DB_COL_RECEIVED_AT)).thenReturn(LocalDateTime.now());
        when(row.getValue(Constants.DB_COL_PAYLOAD)).thenReturn(null);

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            JsonArray telemetryData = responseObj.getJsonArray(Constants.JSON_KEY_TELEMETRY);
            JsonObject telemetry = telemetryData.getJsonObject(0);
            
            // Should not have payload field when null
            assertFalse(telemetry.containsKey(Constants.JSON_KEY_PAYLOAD));
            
            testContext.completeNow();
            return null;
        });

        telemetryHandler.getTelemetryByDevice(routingContext);
    }

    @Test
    @DisplayName("Should handle multiple telemetry records")
    void testGetTelemetryMultipleRecords(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn("1");
        
        // Mock database response with multiple records
        Row row2 = mock(Row.class);
        when(pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, true, false);
        when(rowIterator.next()).thenReturn(row, row2);
        
        // Mock first row
        when(row.getLong(Constants.DB_COL_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_DEVICE_ID)).thenReturn(1L);
        when(row.getLong(Constants.DB_COL_TOPIC_ID)).thenReturn(100L);
        when(row.getLocalDateTime(Constants.DB_COL_RECEIVED_AT)).thenReturn(LocalDateTime.now());
        when(row.getValue(Constants.DB_COL_PAYLOAD)).thenReturn(new JsonObject().put("temperature", 25.5));
        
        // Mock second row
        when(row2.getLong(Constants.DB_COL_ID)).thenReturn(2L);
        when(row2.getLong(Constants.DB_COL_DEVICE_ID)).thenReturn(1L);
        when(row2.getLong(Constants.DB_COL_TOPIC_ID)).thenReturn(101L);
        when(row2.getLocalDateTime(Constants.DB_COL_RECEIVED_AT)).thenReturn(LocalDateTime.now());
        when(row2.getValue(Constants.DB_COL_PAYLOAD)).thenReturn(new JsonObject().put("humidity", 60.0));

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            JsonArray telemetryData = responseObj.getJsonArray(Constants.JSON_KEY_TELEMETRY);
            assertEquals(2, telemetryData.size());
            assertEquals(2, responseObj.getInteger(Constants.JSON_KEY_COUNT));
            
            testContext.completeNow();
            return null;
        });

        telemetryHandler.getTelemetryByDevice(routingContext);
    }

    @Test
    @DisplayName("Should create proper error JSON for bad requests")
    void testBadRequestErrorJson(VertxTestContext testContext) {
        when(routingContext.pathParam("deviceId")).thenReturn(null);

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            // Verify error JSON structure
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_ERROR));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_MESSAGE));
            assertEquals("Bad Request", responseObj.getString(Constants.JSON_KEY_ERROR));
            
            testContext.completeNow();
            return null;
        });

        testModeHandler.getTelemetryByDevice(routingContext);
    }

    @Test
    @DisplayName("Should create proper error JSON for internal server errors")
    void testInternalServerErrorJson(VertxTestContext testContext) {
        Exception dbError = new RuntimeException("Database connection failed");
        
        when(routingContext.pathParam("deviceId")).thenReturn("1");
        when(pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.failedFuture(dbError));

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            // Verify error JSON structure
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_ERROR));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_MESSAGE));
            assertEquals("Failed to fetch telemetry data", responseObj.getString(Constants.JSON_KEY_ERROR));
            
            testContext.completeNow();
            return null;
        });

        telemetryHandler.getTelemetryByDevice(routingContext);
    }
}