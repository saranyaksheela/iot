package iot.provider.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceHandler.
 */
@ExtendWith({VertxExtension.class, MockitoExtension.class})
@DisplayName("DeviceHandler Tests")
public class DeviceHandlerTest {

    @Mock
    private RoutingContext routingContext;
    
    @Mock
    private HttpServerRequest request;
    
    @Mock
    private HttpServerResponse response;
    
    @Mock
    private RequestBody requestBody;
    
    @Mock
    private Pool pool;
    
    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;
    
    @Mock
    private Query<RowSet<Row>> query;
    
    @Mock
    private RowSet<Row> rowSet;
    
    @Mock
    private RowIterator<Row> rowIterator;
    
    @Mock
    private Row row;

    private DeviceHandler deviceHandler;
    private DeviceHandler testModeHandler;

    @BeforeEach
    void setUp() {
        deviceHandler = new DeviceHandler(pool);
        testModeHandler = new DeviceHandler(null); // Test mode without database
        
        // Setup lenient mock behavior for common interactions
        lenient().when(routingContext.request()).thenReturn(request);
        lenient().when(routingContext.response()).thenReturn(response);
        lenient().when(routingContext.body()).thenReturn(requestBody);
        lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
        lenient().when(response.putHeader(anyString(), anyString())).thenReturn(response);
        lenient().when(response.end(anyString())).thenReturn(null);
    }

    // ========== CREATE DEVICE TESTS ==========

    @Test
    @DisplayName("Should create device successfully in test mode")
    void testCreateDeviceTestMode(VertxTestContext testContext) {
        JsonObject validDeviceRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_NAME, "Test Device")
                .put(Constants.JSON_KEY_DEVICE_TYPE, "sensor")
                .put(Constants.JSON_KEY_FIRMWARE_VERSION, "1.0.0")
                .put(Constants.JSON_KEY_LOCATION, "Lab 1");

        when(requestBody.asJsonObject()).thenReturn(validDeviceRequest);

        // Capture the JSON response to verify structure
        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            // Verify response structure
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_ID));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_DEVICE_UUID));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_DEVICE_NAME));
            assertEquals("Test Device", responseObj.getString(Constants.JSON_KEY_DEVICE_NAME));
            assertEquals("sensor", responseObj.getString(Constants.JSON_KEY_DEVICE_TYPE));
            
            testContext.completeNow();
            return null;
        });

        testModeHandler.createDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_CREATED);
        verify(response).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
    }

    @Test
    @DisplayName("Should reject device creation with missing device name")
    void testCreateDeviceValidationMissingName(VertxTestContext testContext) {
        JsonObject invalidRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_TYPE, "sensor");

        when(requestBody.asJsonObject()).thenReturn(invalidRequest);

        testModeHandler.createDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        verify(response).putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject device creation with null request body")
    void testCreateDeviceNullBody(VertxTestContext testContext) {
        when(requestBody.asJsonObject()).thenReturn(null);

        testModeHandler.createDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should handle device creation with default status")
    void testCreateDeviceDefaultStatus(VertxTestContext testContext) {
        JsonObject deviceRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_NAME, "Test Device")
                .put(Constants.JSON_KEY_DEVICE_TYPE, "sensor")
                .put(Constants.JSON_KEY_FIRMWARE_VERSION, "1.0.0")
                .put(Constants.JSON_KEY_LOCATION, "Lab 1");
                // No status provided, should use default

        when(requestBody.asJsonObject()).thenReturn(deviceRequest);

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            assertEquals(Constants.DEFAULT_STATUS, responseObj.getString(Constants.JSON_KEY_STATUS));
            testContext.completeNow();
            return null;
        });

        testModeHandler.createDevice(routingContext);
    }

    // ========== GET ALL DEVICES TESTS ==========

    @Test
    @DisplayName("Should return empty devices list in test mode")
    void testGetAllDevicesTestMode(VertxTestContext testContext) {
        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_DEVICES));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_COUNT));
            assertEquals(0, responseObj.getInteger(Constants.JSON_KEY_COUNT));
            assertTrue(responseObj.getJsonArray(Constants.JSON_KEY_DEVICES).isEmpty());
            
            testContext.completeNow();
            return null;
        });

        testModeHandler.getAllDevices(routingContext);

        verify(response).setStatusCode(Constants.STATUS_OK);
    }

    @Test
    @DisplayName("Should handle database query success for getAllDevices")
    void testGetAllDevicesSuccess(VertxTestContext testContext) {
        // Mock database response
        when(pool.query(Constants.SELECT_ALL_DEVICES_QUERY)).thenReturn(query);
        when(query.execute()).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.next()).thenReturn(row);
        
        // Mock row data
        UUID testUuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        java.time.LocalDateTime testTime = java.time.LocalDateTime.now();
        
        when(row.getLong(Constants.DB_COL_ID)).thenReturn(1L);
        when(row.getUUID(Constants.DB_COL_DEVICE_UUID)).thenReturn(testUuid);
        when(row.getString(Constants.DB_COL_DEVICE_NAME)).thenReturn("Test Device");
        when(row.getString(Constants.DB_COL_DEVICE_TYPE)).thenReturn("sensor");
        when(row.getString(Constants.DB_COL_FIRMWARE_VERSION)).thenReturn("1.0.0");
        when(row.getString(Constants.DB_COL_LOCATION)).thenReturn("Lab 1");
        when(row.getString(Constants.DB_COL_STATUS)).thenReturn("active");
        when(row.getLocalDateTime(Constants.DB_COL_CREATED_AT)).thenReturn(testTime);

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            assertEquals(1, responseObj.getInteger(Constants.JSON_KEY_COUNT));
            JsonArray devices = responseObj.getJsonArray(Constants.JSON_KEY_DEVICES);
            assertEquals(1, devices.size());
            
            testContext.completeNow();
            return null;
        });

        deviceHandler.getAllDevices(routingContext);

        verify(response).setStatusCode(Constants.STATUS_OK);
    }

    @Test
    @DisplayName("Should handle database error for getAllDevices")
    void testGetAllDevicesDbError(VertxTestContext testContext) {
        Exception dbError = new RuntimeException("Database connection failed");
        
        when(pool.query(Constants.SELECT_ALL_DEVICES_QUERY)).thenReturn(query);
        when(query.execute()).thenReturn(io.vertx.core.Future.failedFuture(dbError));

        deviceHandler.getAllDevices(routingContext);

        verify(response).setStatusCode(Constants.STATUS_INTERNAL_SERVER_ERROR);
        testContext.completeNow();
    }

    // ========== UPDATE DEVICE TESTS ==========

    @Test
    @DisplayName("Should reject update with missing device ID")
    void testUpdateDeviceMissingId(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn(null);

        testModeHandler.updateDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject update with invalid device ID format")
    void testUpdateDeviceInvalidId(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn("invalid-id");

        testModeHandler.updateDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject update with missing request body")
    void testUpdateDeviceMissingBody(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn("1");
        when(requestBody.asJsonObject()).thenReturn(null);

        testModeHandler.updateDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject update with missing device name")
    void testUpdateDeviceMissingName(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn("1");
        when(requestBody.asJsonObject()).thenReturn(new JsonObject());

        testModeHandler.updateDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject update with device name exceeding maximum length")
    void testUpdateDeviceNameTooLong(VertxTestContext testContext) {
        String longName = "a".repeat(101); // 101 characters
        JsonObject updateRequest = new JsonObject()
                .put(Constants.JSON_KEY_DEVICE_NAME, longName);

        when(routingContext.pathParam("id")).thenReturn("1");
        when(requestBody.asJsonObject()).thenReturn(updateRequest);

        testModeHandler.updateDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    // ========== DELETE DEVICE TESTS ==========

    @Test
    @DisplayName("Should reject delete with missing device ID")
    void testDeleteDeviceMissingId(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn(null);

        testModeHandler.deleteDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should reject delete with invalid device ID format")
    void testDeleteDeviceInvalidId(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn("not-a-number");

        testModeHandler.deleteDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_BAD_REQUEST);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should simulate device not found in test mode")
    void testDeleteDeviceTestModeNotFound(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn("1");

        testModeHandler.deleteDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_NOT_FOUND);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should handle successful device deletion")
    void testDeleteDeviceSuccess(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn("1");
        when(pool.preparedQuery(Constants.DELETE_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.size()).thenReturn(1);

        when(response.end(anyString())).thenAnswer(invocation -> {
            String jsonResponse = invocation.getArgument(0);
            JsonObject responseObj = new JsonObject(jsonResponse);
            
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_MESSAGE));
            assertTrue(responseObj.containsKey(Constants.JSON_KEY_ID));
            assertEquals(1L, responseObj.getLong(Constants.JSON_KEY_ID));
            
            testContext.completeNow();
            return null;
        });

        deviceHandler.deleteDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_OK);
    }

    @Test
    @DisplayName("Should handle device not found during deletion")
    void testDeleteDeviceNotFound(VertxTestContext testContext) {
        when(routingContext.pathParam("id")).thenReturn("999");
        when(pool.preparedQuery(Constants.DELETE_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.succeededFuture(rowSet));
        when(rowSet.size()).thenReturn(0);

        deviceHandler.deleteDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_NOT_FOUND);
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should handle database error during deletion")
    void testDeleteDeviceDbError(VertxTestContext testContext) {
        Exception dbError = new RuntimeException("Database error");
        
        when(routingContext.pathParam("id")).thenReturn("1");
        when(pool.preparedQuery(Constants.DELETE_DEVICE_QUERY)).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(io.vertx.core.Future.failedFuture(dbError));

        deviceHandler.deleteDevice(routingContext);

        verify(response).setStatusCode(Constants.STATUS_INTERNAL_SERVER_ERROR);
        testContext.completeNow();
    }
}