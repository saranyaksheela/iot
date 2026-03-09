package iot.provider.handler;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iot.provider.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for telemetry data operations.
 * This class encapsulates telemetry-related operations including
 * fetching telemetry data by device ID.
 */
public class TelemetryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TelemetryHandler.class);
    private final Pool pool;

    public TelemetryHandler(Pool pool) {
        this.pool = pool;
        if (pool == null) {
            logger.info("TelemetryHandler initialized in test mode (no database)");
        } else {
            logger.info("TelemetryHandler initialized with database pool");
        }
    }

    /**
     * Handle get telemetry by device ID request.
     * Retrieves all telemetry data for a specific device from database.
     */
    public void getTelemetryByDevice(RoutingContext ctx) {
        logger.debug("Received get telemetry by device request");
        
        try {
            // Extract device ID from path parameter
            String deviceIdParam = ctx.pathParam("deviceId");
            if (deviceIdParam == null || deviceIdParam.trim().isEmpty()) {
                logger.warn("Device ID is missing in path parameter");
                sendBadRequest(ctx, "Device ID is required");
                return;
            }
            
            long deviceId;
            try {
                deviceId = Long.parseLong(deviceIdParam);
            } catch (NumberFormatException e) {
                logger.warn("Invalid device ID format: {}", deviceIdParam);
                sendBadRequest(ctx, "Invalid device ID format");
                return;
            }
            
            logger.info("Fetching telemetry data for device: deviceId={}", deviceId);
            
            // Handle test mode (no database)
            if (pool == null) {
                logger.info("Test mode - returning empty telemetry data");
                JsonObject mockResponse = new JsonObject()
                    .put(Constants.JSON_KEY_DEVICE_ID, deviceId)
                    .put(Constants.JSON_KEY_TELEMETRY, new JsonArray())
                    .put(Constants.JSON_KEY_COUNT, 0);
                sendSuccess(ctx, mockResponse);
                return;
            }
            
            pool.preparedQuery(Constants.SELECT_TELEMETRY_BY_DEVICE_QUERY)
                .execute(Tuple.of(deviceId))
                .onSuccess(rows -> {
                    JsonArray telemetryData = new JsonArray();
                    
                    for (Row row : rows) {
                        telemetryData.add(buildTelemetryResponse(row));
                    }
                    
                    JsonObject response = new JsonObject()
                        .put(Constants.JSON_KEY_DEVICE_ID, deviceId)
                        .put(Constants.JSON_KEY_TELEMETRY, telemetryData)
                        .put(Constants.JSON_KEY_COUNT, telemetryData.size());
                    
                    logger.info("Retrieved {} telemetry records for device: deviceId={}", telemetryData.size(), deviceId);
                    sendSuccess(ctx, response);
                })
                .onFailure(err -> {
                    logger.error("Failed to fetch telemetry data: deviceId={}, error={}", deviceId, err.getMessage(), err);
                    sendInternalError(ctx, "Failed to fetch telemetry data", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error while fetching telemetry data", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
    }

    /**
     * Build telemetry response from database row.
     */
    private JsonObject buildTelemetryResponse(Row row) {
        JsonObject telemetry = new JsonObject()
            .put(Constants.JSON_KEY_ID, row.getLong(Constants.DB_COL_ID))
            .put(Constants.JSON_KEY_DEVICE_ID, row.getLong(Constants.DB_COL_DEVICE_ID))
            .put(Constants.JSON_KEY_TOPIC_ID, row.getLong(Constants.DB_COL_TOPIC_ID))
            .put(Constants.JSON_KEY_RECEIVED_AT, row.getLocalDateTime(Constants.DB_COL_RECEIVED_AT).toString());
        
        // Handle JSONB payload
        Object payload = row.getValue(Constants.DB_COL_PAYLOAD);
        if (payload != null) {
            if (payload instanceof JsonObject) {
                telemetry.put(Constants.JSON_KEY_PAYLOAD, (JsonObject) payload);
            } else if (payload instanceof String) {
                try {
                    telemetry.put(Constants.JSON_KEY_PAYLOAD, new JsonObject((String) payload));
                } catch (Exception e) {
                    logger.warn("Failed to parse payload as JSON, storing as string: {}", payload);
                    telemetry.put(Constants.JSON_KEY_PAYLOAD, payload.toString());
                }
            } else {
                telemetry.put(Constants.JSON_KEY_PAYLOAD, payload.toString());
            }
        }
        
        return telemetry;
    }

    /**
     * Send successful response.
     */
    private void sendSuccess(RoutingContext ctx, JsonObject response) {
        ctx.response()
            .setStatusCode(Constants.STATUS_OK)
            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
            .end(response.encode());
    }

    /**
     * Send bad request response.
     */
    private void sendBadRequest(RoutingContext ctx, String message) {
        JsonObject error = new JsonObject()
            .put(Constants.JSON_KEY_ERROR, "Bad Request")
            .put(Constants.JSON_KEY_MESSAGE, message);
        
        ctx.response()
            .setStatusCode(Constants.STATUS_BAD_REQUEST)
            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
            .end(error.encode());
    }

    /**
     * Send internal error response.
     */
    private void sendInternalError(RoutingContext ctx, String error, String message) {
        JsonObject errorResponse = new JsonObject()
            .put(Constants.JSON_KEY_ERROR, error)
            .put(Constants.JSON_KEY_MESSAGE, message);
        
        ctx.response()
            .setStatusCode(Constants.STATUS_INTERNAL_SERVER_ERROR)
            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
            .end(errorResponse.encode());
    }
}
