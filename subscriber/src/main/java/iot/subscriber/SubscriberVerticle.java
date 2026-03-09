package iot.subscriber;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.core.http.HttpMethod;
import iot.subscriber.config.Constants;
import iot.subscriber.handler.AuthHandler;
import iot.subscriber.handler.DataFetchHandler;
import iot.subscriber.handler.DeviceProxyHandler;
import iot.subscriber.handler.HealthHandler;
import iot.subscriber.handler.TelemetryProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main verticle for the Subscriber service.
 * This verticle is responsible for:
 * - Initializing WebClient for HTTP communication
 * - Setting up HTTP server and routes
 * - Delegating business logic to handlers
 * - Acting as a proxy to the Provider service
 */
public class SubscriberVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(SubscriberVerticle.class);
  
  private WebClient webClient;
  private JsonObject authConfig;
  private AuthHandler authHandler;
  private DeviceProxyHandler deviceProxyHandler;
  private TelemetryProxyHandler telemetryProxyHandler;
  private DataFetchHandler dataFetchHandler;
  private HealthHandler healthHandler;

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting Subscriber service...");
    
    // Load authentication configuration
    vertx.fileSystem().readFile("src/main/resources/auth-config.json")
      .onSuccess(authBuffer -> {
        try {
          authConfig = authBuffer.toJsonObject();
          logger.debug("Authentication configuration loaded successfully");
          
          // Initialize WebClient
          initWebClient();
          
          // Initialize authentication
          initAuthentication();
          
          // Initialize handlers
          initHandlers();
          
          // Start HTTP server
          startHttpServer(startPromise);
          
        } catch (Exception e) {
          logger.error("Failed to initialize Subscriber service", e);
          startPromise.fail(e);
        }
      })
      .onFailure(err -> {
        logger.error("Failed to load auth config file: {}", err.getMessage(), err);
        startPromise.fail(err);
      });
  }

  /**
   * Initialize WebClient for HTTP communication with Provider.
   */
  private void initWebClient() {
    logger.info("Initializing WebClient...");
    webClient = WebClient.create(vertx);
    logger.info("WebClient initialized successfully");
  }

  /**
   * Initialize authentication handler.
   */
  private void initAuthentication() {
    logger.info("Initializing authentication...");
    authHandler = new AuthHandler(authConfig);
    
    boolean authEnabled = authConfig.getBoolean(Constants.CONFIG_AUTH_ENABLED, true);
    if (authEnabled && authHandler.isConfigured()) {
      logger.info("Authentication enabled with {} API keys", authHandler.getApiKeyCount());
      logger.info("Provider API key configured: {}", authHandler.getProviderApiKey() != null && !authHandler.getProviderApiKey().isEmpty());
    } else if (!authEnabled) {
      logger.warn("Authentication is DISABLED in configuration. All routes are publicly accessible!");
    } else {
      logger.warn("Authentication is enabled but no API keys configured. All requests will be rejected!");
    }
  }

  /**
   * Initialize business logic handlers.
   */
  private void initHandlers() {
    logger.info("Initializing handlers...");
    deviceProxyHandler = new DeviceProxyHandler(webClient, authHandler);
    telemetryProxyHandler = new TelemetryProxyHandler(webClient, authHandler);
    dataFetchHandler = new DataFetchHandler(webClient, authHandler);
    healthHandler = new HealthHandler();
    logger.info("Handlers initialized successfully");
  }

  /**
   * Start HTTP server with configured routes.
   */
  private void startHttpServer(Promise<Void> startPromise) {
    logger.info("Starting HTTP server...");
    
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    
    // Configure router
    configureRouter(router);
    
    // Setup routes
    setupRoutes(router);
    
    // Start the HTTP server
    int port = config().getInteger(Constants.CONFIG_HTTP_PORT, Constants.DEFAULT_HTTP_PORT);
    server.requestHandler(router).listen(port)
      .onSuccess(s -> {
        logger.info("Subscriber service started successfully on port {}", port);
        logger.info("Base URL: {}", Constants.BASE_URL);
        startPromise.complete();
      })
      .onFailure(err -> {
        logger.error("Failed to start HTTP server on port {}: {}", port, err.getMessage(), err);
        startPromise.fail(err);
      });
  }

  /**
   * Configure router with middleware.
   */
  private void configureRouter(Router router) {
    logger.debug("Configuring router middleware...");
    
    // CORS handler - must be first to handle preflight requests
    CorsHandler corsHandler = CorsHandler.create()
      .addOrigin("*")  // Allow all origins for development (restrict in production)
      .allowedMethod(HttpMethod.GET)
      .allowedMethod(HttpMethod.POST)
      .allowedMethod(HttpMethod.PUT)
      .allowedMethod(HttpMethod.DELETE)
      .allowedMethod(HttpMethod.OPTIONS)
      .allowedHeader("Content-Type")
      .allowedHeader("X-API-Key")
      .allowedHeader("Authorization")
      .allowCredentials(false);
    
    router.route().handler(corsHandler);
    logger.info("CORS enabled for all origins with methods: GET, POST, PUT, DELETE, OPTIONS");
    
    // Body handler for processing request bodies
    router.route().handler(BodyHandler.create());
    
    // Timeout handler - 30 seconds
    router.route().handler(TimeoutHandler.create(30000));
    
    // Request logging
    router.route().handler(ctx -> {
      logger.info("Incoming request: method={}, path={}, remoteAddress={}", 
        ctx.request().method(), 
        ctx.request().path(),
        ctx.request().remoteAddress());
      ctx.next();
    });
    
    // Error handler
    router.route().failureHandler(ctx -> {
      int statusCode = ctx.statusCode();
      Throwable failure = ctx.failure();
      
      if (failure != null) {
        logger.error("Request failed: path={}, statusCode={}, error={}", 
          ctx.request().path(), statusCode, failure.getMessage(), failure);
      } else {
        logger.warn("Request failed: path={}, statusCode={}", ctx.request().path(), statusCode);
      }
      
      JsonObject error = new JsonObject()
        .put(Constants.JSON_KEY_ERROR, "Request failed")
        .put(Constants.JSON_KEY_MESSAGE, failure != null ? failure.getMessage() : "Unknown error");
      
      ctx.response()
        .setStatusCode(statusCode > 0 ? statusCode : 500)
        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
        .end(error.encode());
    });
    
    logger.debug("Router middleware configured");
  }

  /**
   * Setup API routes and delegate to handlers.
   */
  private void setupRoutes(Router router) {
    logger.debug("Setting up routes...");
    
    // Check if authentication is enabled
    boolean authEnabled = authConfig.getBoolean(Constants.CONFIG_AUTH_ENABLED, true);
    
    // Health check endpoint (no authentication required)
    router.get(Constants.HEALTH_ENDPOINT).handler(healthHandler::healthCheck);
    
    // Protected REST API endpoints with authentication
    if (authEnabled && authHandler.isConfigured()) {
      logger.info("Authentication middleware enabled for protected routes");
      
      // Apply authentication to all /subscriber/api/* routes except status
      router.route(Constants.BASE_URL + "/*").handler(ctx -> {
        // Allow status endpoint without auth
        if (ctx.request().path().equals(Constants.STATUS_ENDPOINT)) {
          ctx.next();
        } else {
          authHandler.authenticate(ctx);
        }
      });
      
      // REST API endpoints - protected by authentication
      router.post(Constants.DEVICES_ENDPOINT).handler(deviceProxyHandler::createDevice);
      router.put(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceProxyHandler::updateDevice);
      router.delete(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceProxyHandler::deleteDevice);
      router.get(Constants.TELEMETRY_BY_DEVICE_ENDPOINT).handler(telemetryProxyHandler::getTelemetryByDevice);
      router.get(Constants.FETCH_ENDPOINT).handler(dataFetchHandler::fetchFromProvider);
      router.get(Constants.STATUS_ENDPOINT).handler(healthHandler::getStatus);
      
      logger.info("Routes configured with authentication: POST {}, PUT {}, DELETE {}, GET {}, GET {}, GET {}, GET {}", 
        Constants.DEVICES_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.TELEMETRY_BY_DEVICE_ENDPOINT,
        Constants.FETCH_ENDPOINT,
        Constants.STATUS_ENDPOINT,
        Constants.HEALTH_ENDPOINT);
    } else {
      logger.warn("Authentication disabled or not configured - routes are publicly accessible!");
      
      // REST API endpoints without authentication (NOT RECOMMENDED FOR PRODUCTION)
      router.post(Constants.DEVICES_ENDPOINT).handler(deviceProxyHandler::createDevice);
      router.put(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceProxyHandler::updateDevice);
      router.delete(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceProxyHandler::deleteDevice);
      router.get(Constants.TELEMETRY_BY_DEVICE_ENDPOINT).handler(telemetryProxyHandler::getTelemetryByDevice);
      router.get(Constants.FETCH_ENDPOINT).handler(dataFetchHandler::fetchFromProvider);
      router.get(Constants.STATUS_ENDPOINT).handler(healthHandler::getStatus);
      
      logger.info("Routes configured WITHOUT authentication: POST {}, PUT {}, DELETE {}, GET {}, GET {}, GET {}, GET {}", 
        Constants.DEVICES_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.TELEMETRY_BY_DEVICE_ENDPOINT,
        Constants.FETCH_ENDPOINT,
        Constants.STATUS_ENDPOINT,
        Constants.HEALTH_ENDPOINT);
    }
  }
  
  @Override
  public void stop() {
    logger.info("Stopping Subscriber service...");
    
    if (webClient != null) {
      webClient.close();
      logger.info("WebClient closed");
    }
    
    logger.info("Subscriber service stopped");
  }
}
