package iot.subscriber.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iot.subscriber.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for health check and status endpoints.
 */
public class HealthHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthHandler.class);

    public HealthHandler() {
        logger.info("HealthHandler initialized");
    }

    /**
     * Handle health check request.
     */
    public void healthCheck(RoutingContext ctx) {
        logger.debug("Health check requested");
        
        JsonObject response = new JsonObject()
            .put(Constants.JSON_KEY_STATUS, Constants.SERVICE_STATUS_UP)
            .put(Constants.JSON_KEY_SERVICE, Constants.SERVICE_NAME);
        
        ctx.response()
            .setStatusCode(Constants.STATUS_OK)
            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
            .putHeader("Access-Control-Allow-Origin", "*")
            .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            .putHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key, Authorization")
            .end(response.encode());
    }

    /**
     * Handle status endpoint request.
     */
    public void getStatus(RoutingContext ctx) {
        logger.debug("Status endpoint requested");
        
        JsonObject response = new JsonObject()
            .put(Constants.JSON_KEY_STATUS, Constants.SERVICE_STATUS_UP)
            .put(Constants.JSON_KEY_SERVICE, Constants.SERVICE_NAME)
            .put(Constants.JSON_KEY_TIMESTAMP, System.currentTimeMillis());
        
        ctx.response()
            .setStatusCode(Constants.STATUS_OK)
            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
            .putHeader("Access-Control-Allow-Origin", "*")
            .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            .putHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key, Authorization")
            .end(response.encode());
    }
}
