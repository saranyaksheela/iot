package iot.provider.handler;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iot.provider.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handler for device-related business logic.
 * This class encapsulates all device operations including validation,
 * database interactions, and response formatting.
 */
public class DeviceHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceHandler.class);
    private final Pool pool;

    public DeviceHandler(Pool pool) {
        this.pool = pool;
        logger.info("DeviceHandler initialized");
    }

    /**
     * Handle device creation request.
     * Validates input, creates device in database, and returns formatted response.
     */
    public void createDevice(RoutingContext ctx) {
        logger.debug("Received device creation request");
        
        try {
            JsonObject body = ctx.body().asJsonObject();
            
            // Validate input
            ValidationResult validation = validateDeviceInput(body);
            if (!validation.isValid()) {
                logger.warn("Invalid device input: {}", validation.getErrorMessage());
                sendBadRequest(ctx, validation.getErrorMessage());
                return;
            }
            
            // Extract and sanitize input
            String deviceName = sanitizeInput(body.getString(Constants.JSON_KEY_DEVICE_NAME));
            String deviceType = sanitizeInput(body.getString(Constants.JSON_KEY_DEVICE_TYPE));
            String firmwareVersion = sanitizeInput(body.getString(Constants.JSON_KEY_FIRMWARE_VERSION));
            String location = sanitizeInput(body.getString(Constants.JSON_KEY_LOCATION));
            String status = body.getString(Constants.JSON_KEY_STATUS, Constants.DEFAULT_STATUS);
            UUID deviceUuid = UUID.randomUUID();

            logger.info("Creating device: name={}, type={}, uuid={}", deviceName, deviceType, deviceUuid);

            // Execute database insert
            pool.preparedQuery(Constants.INSERT_DEVICE_QUERY)
                .execute(Tuple.of(deviceUuid, deviceName, deviceType, firmwareVersion, location, status))
                .onSuccess(rows -> {
                    Row row = rows.iterator().next();
                    JsonObject response = buildDeviceResponse(row);
                    logger.info("Device created successfully: id={}, uuid={}", response.getLong(Constants.JSON_KEY_ID), deviceUuid);
                    sendCreated(ctx, response);
                })
                .onFailure(err -> {
                    logger.error("Failed to create device: name={}, error={}", deviceName, err.getMessage(), err);
                    sendInternalError(ctx, "Failed to create device", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error during device creation", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
    }

    /**
     * Handle get all devices request.
     * Retrieves all devices from database and returns formatted response.
     */
    public void getAllDevices(RoutingContext ctx) {
        logger.debug("Received get all devices request");
        
        try {
            pool.query(Constants.SELECT_ALL_DEVICES_QUERY)
                .execute()
                .onSuccess(rows -> {
                    JsonArray devices = new JsonArray();
                    
                    for (Row row : rows) {
                        devices.add(buildDeviceResponse(row));
                    }
                    
                    JsonObject response = new JsonObject()
                        .put(Constants.JSON_KEY_DEVICES, devices)
                        .put(Constants.JSON_KEY_COUNT, devices.size());
                    
                    logger.info("Retrieved {} devices", devices.size());
                    sendSuccess(ctx, response);
                })
                .onFailure(err -> {
                    logger.error("Failed to fetch devices: error={}", err.getMessage(), err);
                    sendInternalError(ctx, "Failed to fetch devices", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error while fetching devices", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
    }

    /**
     * Validate device input data.
     */
    private ValidationResult validateDeviceInput(JsonObject body) {
        if (body == null) {
            return ValidationResult.invalid("Request body is required");
        }
        
        if (!body.containsKey(Constants.JSON_KEY_DEVICE_NAME) || 
            body.getString(Constants.JSON_KEY_DEVICE_NAME) == null ||
            body.getString(Constants.JSON_KEY_DEVICE_NAME).trim().isEmpty()) {
            return ValidationResult.invalid("Device name is required");
        }
        
        if (!body.containsKey(Constants.JSON_KEY_DEVICE_TYPE) || 
            body.getString(Constants.JSON_KEY_DEVICE_TYPE) == null ||
            body.getString(Constants.JSON_KEY_DEVICE_TYPE).trim().isEmpty()) {
            return ValidationResult.invalid("Device type is required");
        }
        
        // Validate device name length
        String deviceName = body.getString(Constants.JSON_KEY_DEVICE_NAME);
        if (deviceName.length() > 100) {
            return ValidationResult.invalid("Device name must not exceed 100 characters");
        }
        
        // Validate device type length
        String deviceType = body.getString(Constants.JSON_KEY_DEVICE_TYPE);
        if (deviceType.length() > 50) {
            return ValidationResult.invalid("Device type must not exceed 50 characters");
        }
        
        // Validate firmware version if provided
        if (body.containsKey(Constants.JSON_KEY_FIRMWARE_VERSION)) {
            String firmwareVersion = body.getString(Constants.JSON_KEY_FIRMWARE_VERSION);
            if (firmwareVersion != null && firmwareVersion.length() > 50) {
                return ValidationResult.invalid("Firmware version must not exceed 50 characters");
            }
        }
        
        // Validate location if provided
        if (body.containsKey(Constants.JSON_KEY_LOCATION)) {
            String location = body.getString(Constants.JSON_KEY_LOCATION);
            if (location != null && location.length() > 100) {
                return ValidationResult.invalid("Location must not exceed 100 characters");
            }
        }
        
        // Validate status if provided
        if (body.containsKey(Constants.JSON_KEY_STATUS)) {
            String status = body.getString(Constants.JSON_KEY_STATUS);
            if (status != null && status.length() > 20) {
                return ValidationResult.invalid("Status must not exceed 20 characters");
            }
        }
        
        return ValidationResult.valid();
    }

    /**
     * Sanitize input to prevent injection attacks.
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim();
    }

    /**
     * Build device response from database row.
     */
    private JsonObject buildDeviceResponse(Row row) {
        return new JsonObject()
            .put(Constants.JSON_KEY_ID, row.getLong(Constants.DB_COL_ID))
            .put(Constants.JSON_KEY_DEVICE_UUID, row.getUUID(Constants.DB_COL_DEVICE_UUID).toString())
            .put(Constants.JSON_KEY_DEVICE_NAME, row.getString(Constants.DB_COL_DEVICE_NAME))
            .put(Constants.JSON_KEY_DEVICE_TYPE, row.getString(Constants.DB_COL_DEVICE_TYPE))
            .put(Constants.JSON_KEY_FIRMWARE_VERSION, row.getString(Constants.DB_COL_FIRMWARE_VERSION))
            .put(Constants.JSON_KEY_LOCATION, row.getString(Constants.DB_COL_LOCATION))
            .put(Constants.JSON_KEY_STATUS, row.getString(Constants.DB_COL_STATUS))
            .put(Constants.JSON_KEY_CREATED_AT, row.getLocalDateTime(Constants.DB_COL_CREATED_AT).toString());
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
     * Send created response.
     */
    private void sendCreated(RoutingContext ctx, JsonObject response) {
        ctx.response()
            .setStatusCode(Constants.STATUS_CREATED)
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

    /**
     * Validation result holder.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
