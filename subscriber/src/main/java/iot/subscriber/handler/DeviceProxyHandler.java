package iot.subscriber.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import iot.subscriber.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for proxying device operations to the Provider service.
 * This class handles forwarding device-related requests to the Provider
 * and managing the responses.
 */
public class DeviceProxyHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceProxyHandler.class);
    private final WebClient webClient;

    public DeviceProxyHandler(WebClient webClient) {
        this.webClient = webClient;
        logger.info("DeviceProxyHandler initialized");
    }

    /**
     * Handle device creation by forwarding to Provider service.
     */
    public void createDevice(RoutingContext ctx) {
        logger.debug("Received device creation request, forwarding to Provider");
        
        try {
            JsonObject deviceData = ctx.body().asJsonObject();
            
            if (deviceData == null || deviceData.isEmpty()) {
                logger.warn("Empty request body received for device creation");
                sendBadRequest(ctx, "Request body is required");
                return;
            }
            
            logger.info("Forwarding device creation to Provider: deviceName={}, deviceType={}", 
                deviceData.getString("deviceName"), 
                deviceData.getString("deviceType"));
            
            webClient.post(Constants.PROVIDER_PORT, Constants.PROVIDER_HOST, Constants.PROVIDER_DEVICES_PATH)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .sendJsonObject(deviceData)
                .onSuccess(response -> {
                    logger.info("Device created successfully via Provider: statusCode={}", response.statusCode());
                    ctx.response()
                        .setStatusCode(response.statusCode())
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                        .end(response.bodyAsString());
                })
                .onFailure(err -> {
                    logger.error("Failed to create device in Provider: error={}", err.getMessage(), err);
                    sendInternalError(ctx, "Failed to create device in provider", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error during device creation proxy", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
    }

    /**
     * Handle device update by forwarding to Provider service.
     */
    public void updateDevice(RoutingContext ctx) {
        logger.debug("Received device update request, forwarding to Provider");
        
        try {
            // Extract device ID from path parameter
            String deviceId = ctx.pathParam("id");
            
            if (deviceId == null || deviceId.trim().isEmpty()) {
                logger.warn("Device ID is missing in path parameter");
                sendBadRequest(ctx, "Device ID is required");
                return;
            }
            
            JsonObject deviceData = ctx.body().asJsonObject();
            
            if (deviceData == null || deviceData.isEmpty()) {
                logger.warn("Empty request body received for device update");
                sendBadRequest(ctx, "Request body is required");
                return;
            }
            
            logger.info("Forwarding device update to Provider: deviceId={}, deviceName={}", 
                deviceId, 
                deviceData.getString("deviceName"));
            
            // Build the path with device ID
            String updatePath = Constants.PROVIDER_DEVICES_PATH + "/" + deviceId;
            
            webClient.put(Constants.PROVIDER_PORT, Constants.PROVIDER_HOST, updatePath)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .sendJsonObject(deviceData)
                .onSuccess(response -> {
                    logger.info("Device updated successfully via Provider: deviceId={}, statusCode={}", 
                        deviceId, response.statusCode());
                    ctx.response()
                        .setStatusCode(response.statusCode())
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                        .end(response.bodyAsString());
                })
                .onFailure(err -> {
                    logger.error("Failed to update device in Provider: deviceId={}, error={}", 
                        deviceId, err.getMessage(), err);
                    sendInternalError(ctx, "Failed to update device in provider", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error during device update proxy", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
    }

    /**
     * Handle device deletion by forwarding to Provider service.
     */
    public void deleteDevice(RoutingContext ctx) {
        logger.debug("Received device deletion request, forwarding to Provider");
        
        try {
            // Extract device ID from path parameter
            String deviceId = ctx.pathParam("id");
            
            if (deviceId == null || deviceId.trim().isEmpty()) {
                logger.warn("Device ID is missing in path parameter");
                sendBadRequest(ctx, "Device ID is required");
                return;
            }
            
            logger.info("Forwarding device deletion to Provider: deviceId={}", deviceId);
            
            // Build the path with device ID
            String deletePath = Constants.PROVIDER_DEVICES_PATH + "/" + deviceId;
            
            webClient.delete(Constants.PROVIDER_PORT, Constants.PROVIDER_HOST, deletePath)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .send()
                .onSuccess(response -> {
                    logger.info("Device deleted successfully via Provider: deviceId={}, statusCode={}", 
                        deviceId, response.statusCode());
                    ctx.response()
                        .setStatusCode(response.statusCode())
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                        .end(response.bodyAsString());
                })
                .onFailure(err -> {
                    logger.error("Failed to delete device in Provider: deviceId={}, error={}", 
                        deviceId, err.getMessage(), err);
                    sendInternalError(ctx, "Failed to delete device in provider", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error during device deletion proxy", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
    }

    /**
     * Send bad request response.
     */
    private void sendBadRequest(RoutingContext ctx, String message) {
        JsonObject error = new JsonObject()
            .put(Constants.JSON_KEY_ERROR, "Bad Request")
            .put(Constants.JSON_KEY_MESSAGE, message);
        
        ctx.response()
            .setStatusCode(400)
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
