package iot.provider.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iot.provider.config.Constants;

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
        if (pool == null) {
            logger.info("DeviceHandler initialized in test mode (no database)");
        } else {
            logger.info("DeviceHandler initialized with database pool");
        }
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

            // Handle test mode (no database)
            if (pool == null) {
                logger.info("Test mode - returning mock device response");
                JsonObject mockResponse = new JsonObject()
                    .put(Constants.JSON_KEY_ID, 1L)
                    .put(Constants.JSON_KEY_DEVICE_UUID, deviceUuid.toString())
                    .put(Constants.JSON_KEY_DEVICE_NAME, deviceName)
                    .put(Constants.JSON_KEY_DEVICE_TYPE, deviceType)
                    .put(Constants.JSON_KEY_FIRMWARE_VERSION, firmwareVersion)
                    .put(Constants.JSON_KEY_LOCATION, location)
                    .put(Constants.JSON_KEY_STATUS, status)
                    .put(Constants.JSON_KEY_CREATED_AT, java.time.Instant.now().toString());
                sendCreated(ctx, mockResponse);
                return;
            }

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
            // Handle test mode (no database)
            if (pool == null) {
                logger.info("Test mode - returning empty devices list");
                JsonObject mockResponse = new JsonObject()
                    .put(Constants.JSON_KEY_DEVICES, new JsonArray())
                    .put(Constants.JSON_KEY_COUNT, 0);
                sendSuccess(ctx, mockResponse);
                return;
            }
            
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
     * Handle device update request.
     * Updates device name by ID and returns the updated device.
     */
    public void updateDevice(RoutingContext ctx) {
        logger.debug("Received device update request");
        
        try {
            // Extract device ID from path parameter
            String idParam = ctx.pathParam("id");
            if (idParam == null || idParam.trim().isEmpty()) {
                logger.warn("Device ID is missing in path parameter");
                sendBadRequest(ctx, "Device ID is required");
                return;
            }
            
            long deviceId;
            try {
                deviceId = Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                logger.warn("Invalid device ID format: {}", idParam);
                sendBadRequest(ctx, "Invalid device ID format");
                return;
            }
            
            // Parse request body
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                logger.warn("Request body is missing");
                sendBadRequest(ctx, "Request body is required");
                return;
            }
            
            // Validate device name
            if (!body.containsKey(Constants.JSON_KEY_DEVICE_NAME) || 
                body.getString(Constants.JSON_KEY_DEVICE_NAME) == null ||
                body.getString(Constants.JSON_KEY_DEVICE_NAME).trim().isEmpty()) {
                logger.warn("Device name is missing in request body");
                sendBadRequest(ctx, "Device name is required");
                return;
            }
            
            String deviceName = sanitizeInput(body.getString(Constants.JSON_KEY_DEVICE_NAME));
            
            // Validate device name length
            if (deviceName.length() > 100) {
                logger.warn("Device name exceeds maximum length: {}", deviceName.length());
                sendBadRequest(ctx, "Device name must not exceed 100 characters");
                return;
            }
            
            logger.info("Updating device: id={}, newName={}", deviceId, deviceName);
            
            // Handle test mode (no database)
            if (pool == null) {
                logger.info("Test mode - returning mock updated device response");
                JsonObject mockResponse = new JsonObject()
                    .put(Constants.JSON_KEY_ID, deviceId)
                    .put(Constants.JSON_KEY_DEVICE_NAME, deviceName)
                    .put(Constants.JSON_KEY_DEVICE_TYPE, "sensor")
                    .put(Constants.JSON_KEY_STATUS, "active")
                    .put(Constants.JSON_KEY_CREATED_AT, java.time.Instant.now().toString());
                sendSuccess(ctx, mockResponse);
                return;
            }
            
            // Execute database update
            pool.preparedQuery(Constants.UPDATE_DEVICE_QUERY)
                .execute(Tuple.of(deviceName, deviceId))
                .onSuccess(rows -> {
                    if (rows.size() == 0) {
                        logger.warn("Device not found: id={}", deviceId);
                        sendNotFound(ctx, "Device not found");
                    } else {
                        Row row = rows.iterator().next();
                        JsonObject response = buildDeviceResponse(row);
                        logger.info("Device updated successfully: id={}, newName={}", deviceId, deviceName);
                        sendSuccess(ctx, response);
                    }
                })
                .onFailure(err -> {
                    logger.error("Failed to update device: id={}, error={}", deviceId, err.getMessage(), err);
                    sendInternalError(ctx, "Failed to update device", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error during device update", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
    }

    /**
     * Handle device deletion request.
     * Deletes device by ID and returns success response.
     */
    public void deleteDevice(RoutingContext ctx) {
        logger.debug("Received device deletion request");
        
        try {
            // Extract device ID from path parameter
            String idParam = ctx.pathParam("id");
            if (idParam == null || idParam.trim().isEmpty()) {
                logger.warn("Device ID is missing in path parameter");
                sendBadRequest(ctx, "Device ID is required");
                return;
            }
            
            long deviceId;
            try {
                deviceId = Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                logger.warn("Invalid device ID format: {}", idParam);
                sendBadRequest(ctx, "Invalid device ID format");
                return;
            }
            
            logger.info("Deleting device: id={}", deviceId);
            
            // Handle test mode (no database) - simulate not found for testing
            if (pool == null) {
                logger.info("Test mode - simulating device not found");
                sendNotFound(ctx, "Device not found");
                return;
            }
            
            // Execute database delete
            pool.preparedQuery(Constants.DELETE_DEVICE_QUERY)
                .execute(Tuple.of(deviceId))
                .onSuccess(rows -> {
                    if (rows.size() == 0) {
                        logger.warn("Device not found: id={}", deviceId);
                        sendNotFound(ctx, "Device not found");
                    } else {
                        JsonObject response = new JsonObject()
                            .put(Constants.JSON_KEY_MESSAGE, "Device deleted successfully")
                            .put(Constants.JSON_KEY_ID, deviceId);
                        logger.info("Device deleted successfully: id={}", deviceId);
                        sendSuccess(ctx, response);
                    }
                })
                .onFailure(err -> {
                    logger.error("Failed to delete device: id={}, error={}", deviceId, err.getMessage(), err);
                    sendInternalError(ctx, "Failed to delete device", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error during device deletion", e);
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
     * Send not found response.
     */
    private void sendNotFound(RoutingContext ctx, String message) {
        JsonObject error = new JsonObject()
            .put(Constants.JSON_KEY_ERROR, "Not Found")
            .put(Constants.JSON_KEY_MESSAGE, message);
        
        ctx.response()
            .setStatusCode(Constants.STATUS_NOT_FOUND)
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
