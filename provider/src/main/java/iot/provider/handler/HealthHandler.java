package iot.provider.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iot.provider.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for health and general information endpoints.
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
            .put(Constants.JSON_KEY_STATUS, "UP");
        
        ctx.response()
            .setStatusCode(Constants.STATUS_OK)
            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
            .end(response.encode());
    }

    /**
     * Handle data endpoint request.
     */
    public void getData(RoutingContext ctx) {
        logger.debug("Data endpoint requested");
        
        JsonObject response = new JsonObject()
            .put(Constants.JSON_KEY_MESSAGE, "Hello from Provider Service")
            .put(Constants.JSON_KEY_TIMESTAMP, System.currentTimeMillis());
        
        ctx.response()
            .setStatusCode(Constants.STATUS_OK)
            .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
            .end(response.encode());
    }
}
