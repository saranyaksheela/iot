package iot.subscriber.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import iot.subscriber.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for proxying telemetry operations to the Provider service.
 * This class handles forwarding telemetry-related requests to the Provider
 * and managing the responses.
 */
public class TelemetryProxyHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TelemetryProxyHandler.class);
    private final WebClient webClient;

    public TelemetryProxyHandler(WebClient webClient) {
        this.webClient = webClient;
        logger.info("TelemetryProxyHandler initialized");
    }

    /**
     * Handle get telemetry by device ID by forwarding to Provider service.
     */
    public void getTelemetryByDevice(RoutingContext ctx) {
        logger.debug("Received telemetry fetch request, forwarding to Provider");
        
        try {
            // Extract device ID from path parameter
            String deviceId = ctx.pathParam("deviceId");
            
            if (deviceId == null || deviceId.trim().isEmpty()) {
                logger.warn("Device ID is missing in path parameter");
                sendBadRequest(ctx, "Device ID is required");
                return;
            }
            
            logger.info("Forwarding telemetry fetch to Provider: deviceId={}", deviceId);
            
            // Build the path with device ID
            String telemetryPath = Constants.PROVIDER_TELEMETRY_PATH + "/" + deviceId;
            
            webClient.get(Constants.PROVIDER_PORT, Constants.PROVIDER_HOST, telemetryPath)
                .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                .send()
                .onSuccess(response -> {
                    logger.info("Telemetry data fetched successfully via Provider: deviceId={}, statusCode={}", 
                        deviceId, response.statusCode());
                    ctx.response()
                        .setStatusCode(response.statusCode())
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                        .end(response.bodyAsString());
                })
                .onFailure(err -> {
                    logger.error("Failed to fetch telemetry data from Provider: deviceId={}, error={}", 
                        deviceId, err.getMessage(), err);
                    sendInternalError(ctx, "Failed to fetch telemetry data from provider", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error during telemetry fetch proxy", e);
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
