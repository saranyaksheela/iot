package iot.subscriber.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import iot.subscriber.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for data fetching operations from the Provider service.
 */
public class DataFetchHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DataFetchHandler.class);
    private final WebClient webClient;
    private final AuthHandler authHandler;

    public DataFetchHandler(WebClient webClient, AuthHandler authHandler) {
        this.webClient = webClient;
        this.authHandler = authHandler;
        logger.info("DataFetchHandler initialized with authentication support");
    }

    /**
     * Fetch data from Provider service.
     */
    public void fetchFromProvider(RoutingContext ctx) {
        logger.debug("Fetching data from Provider service");
        
        try {
            webClient.get(Constants.PROVIDER_PORT, Constants.PROVIDER_HOST, Constants.PROVIDER_DATA_PATH)
                .putHeader(Constants.HEADER_API_KEY, authHandler.getProviderApiKey())
                .send()
                .onSuccess(response -> {
                    JsonObject result = new JsonObject()
                        .put(Constants.JSON_KEY_SOURCE, "provider")
                        .put(Constants.JSON_KEY_DATA, response.bodyAsJsonObject());
                    
                    logger.info("Data fetched successfully from Provider");
                    logger.debug("Fetched data: {}", result.encode());
                    
                    ctx.response()
                        .setStatusCode(Constants.STATUS_OK)
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                        .end(result.encode());
                })
                .onFailure(err -> {
                    logger.error("Failed to fetch data from Provider: error={}", err.getMessage(), err);
                    sendInternalError(ctx, "Failed to fetch from provider", err.getMessage());
                });
                
        } catch (Exception e) {
            logger.error("Unexpected error while fetching data", e);
            sendInternalError(ctx, "Unexpected error occurred", e.getMessage());
        }
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
