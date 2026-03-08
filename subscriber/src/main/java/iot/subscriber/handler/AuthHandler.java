package iot.subscriber.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication handler for Subscriber service.
 * This handler validates API keys for subscriber clients and manages
 * API key forwarding to the Provider service.
 */
public class AuthHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String UNAUTHORIZED_ERROR = "Unauthorized";
    private static final String INVALID_API_KEY_MESSAGE = "Invalid or missing API key";
    private static final String API_KEY_EXPIRED_MESSAGE = "API key has expired";
    
    private final Map<String, ApiKeyInfo> validApiKeys;
    private final String providerApiKey;

    public AuthHandler(JsonObject authConfig) {
        this.validApiKeys = new ConcurrentHashMap<>();
        this.providerApiKey = authConfig.getString("providerApiKey", "");
        loadApiKeys(authConfig);
        logger.info("AuthHandler initialized with {} API keys", validApiKeys.size());
    }

    /**
     * Load API keys from configuration.
     */
    private void loadApiKeys(JsonObject authConfig) {
        if (authConfig == null || !authConfig.containsKey("apiKeys")) {
            logger.warn("No API keys configured. Authentication will reject all requests.");
            return;
        }

        JsonObject apiKeysConfig = authConfig.getJsonObject("apiKeys");
        for (String key : apiKeysConfig.fieldNames()) {
            JsonObject keyInfo = apiKeysConfig.getJsonObject(key);
            String apiKey = keyInfo.getString("key");
            String description = keyInfo.getString("description", "No description");
            boolean enabled = keyInfo.getBoolean("enabled", true);
            Long expiryTimestamp = keyInfo.getLong("expiryTimestamp", null);
            
            validApiKeys.put(apiKey, new ApiKeyInfo(key, description, enabled, expiryTimestamp));
            logger.debug("Loaded API key: {} - {} (enabled: {})", key, description, enabled);
        }
    }

    /**
     * Authentication middleware handler for subscriber clients.
     */
    public void authenticate(RoutingContext ctx) {
        String apiKey = ctx.request().getHeader(API_KEY_HEADER);

        // Check if API key is present
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Authentication failed: Missing API key from {}", ctx.request().remoteAddress());
            sendUnauthorized(ctx, INVALID_API_KEY_MESSAGE);
            return;
        }

        // Validate API key
        ApiKeyInfo keyInfo = validApiKeys.get(apiKey);
        if (keyInfo == null) {
            logger.warn("Authentication failed: Invalid API key from {}", ctx.request().remoteAddress());
            sendUnauthorized(ctx, INVALID_API_KEY_MESSAGE);
            return;
        }

        // Check if API key is enabled
        if (!keyInfo.isEnabled()) {
            logger.warn("Authentication failed: Disabled API key '{}' from {}", 
                keyInfo.getName(), ctx.request().remoteAddress());
            sendUnauthorized(ctx, INVALID_API_KEY_MESSAGE);
            return;
        }

        // Check if API key is expired
        if (keyInfo.isExpired()) {
            logger.warn("Authentication failed: Expired API key '{}' from {}", 
                keyInfo.getName(), ctx.request().remoteAddress());
            sendUnauthorized(ctx, API_KEY_EXPIRED_MESSAGE);
            return;
        }

        // Authentication successful
        logger.debug("Authentication successful: API key '{}' from {}", 
            keyInfo.getName(), ctx.request().remoteAddress());
        
        // Store API key info in context
        ctx.put("apiKeyName", keyInfo.getName());
        ctx.put("apiKeyDescription", keyInfo.getDescription());
        
        // Continue to next handler
        ctx.next();
    }

    /**
     * Get the API key to use when calling Provider service.
     */
    public String getProviderApiKey() {
        return providerApiKey;
    }

    /**
     * Send unauthorized response.
     */
    private void sendUnauthorized(RoutingContext ctx, String message) {
        JsonObject error = new JsonObject()
            .put("error", UNAUTHORIZED_ERROR)
            .put("message", message)
            .put("statusCode", 401);

        ctx.response()
            .setStatusCode(401)
            .putHeader("content-type", "application/json")
            .putHeader("WWW-Authenticate", "API-Key")
            .end(error.encode());
    }

    /**
     * Check if authentication is properly configured.
     */
    public boolean isConfigured() {
        return !validApiKeys.isEmpty();
    }

    /**
     * Get count of configured API keys.
     */
    public int getApiKeyCount() {
        return validApiKeys.size();
    }

    /**
     * API Key information holder.
     */
    private static class ApiKeyInfo {
        private final String name;
        private final String description;
        private final boolean enabled;
        private final Long expiryTimestamp;

        public ApiKeyInfo(String name, String description, boolean enabled, Long expiryTimestamp) {
            this.name = name;
            this.description = description;
            this.enabled = enabled;
            this.expiryTimestamp = expiryTimestamp;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isExpired() {
            if (expiryTimestamp == null) {
                return false;
            }
            return System.currentTimeMillis() > expiryTimestamp;
        }
    }
}
