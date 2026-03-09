package iot.provider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.core.http.HttpMethod;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import iot.provider.config.Constants;
import iot.provider.handler.AuthHandler;
import iot.provider.handler.DeviceHandler;
import iot.provider.handler.HealthHandler;
import iot.provider.handler.TelemetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main verticle for the Provider service.
 * This verticle is responsible for:
 * - Loading configuration
 * - Initializing database connection pool
 * - Setting up HTTP server and routes
 * - Delegating business logic to handlers
 */
public class ProviderVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(ProviderVerticle.class);
  
  private Pool pool;
  private JsonObject dbConfig;
  private JsonObject authConfig;
  private AuthHandler authHandler;
  private DeviceHandler deviceHandler;
  private TelemetryHandler telemetryHandler;
  private HealthHandler healthHandler;

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting Provider service...");
    
    // Create ConfigRetriever with multiple configuration sources
    ConfigRetrieverOptions options = new ConfigRetrieverOptions();
    
    // Add database configuration from file (lowest priority)
    ConfigStoreOptions dbConfigStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", "db-config.json"));
    options.addStore(dbConfigStore);
    
    // Add authentication configuration from file
    ConfigStoreOptions authConfigStore = new ConfigStoreOptions()
      .setType("file") 
      .setFormat("json")
      .setConfig(new JsonObject().put("path", "auth-config.json"));
    options.addStore(authConfigStore);
    
    // Add environment variables (higher priority - will override file values)
    ConfigStoreOptions envStore = new ConfigStoreOptions()
      .setType("env");
    options.addStore(envStore);
    
    // Add system properties (highest priority)
    ConfigStoreOptions sysPropsStore = new ConfigStoreOptions()
      .setType("sys");
    options.addStore(sysPropsStore);
    
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    
    // Retrieve configuration
    retriever.getConfig()
      .onSuccess(config -> {
        try {
          logger.info("Configuration loaded successfully");
          logger.debug("Full config: {}", config.encodePrettily());
          
          // Extract database configuration - handle both nested JSON and environment variables
          dbConfig = config.getJsonObject(Constants.CONFIG_DB);
          
          // If nested config doesn't exist, build from environment variables or flat config
          if (dbConfig == null) {
            logger.info("No nested database config found, building from environment variables/flat config");
            dbConfig = new JsonObject()
              .put(Constants.CONFIG_DB_HOST, config.getString("DB_HOST", config.getString("host", "postgres")))
              .put(Constants.CONFIG_DB_PORT, config.getInteger("DB_PORT", config.getInteger("port", 5432)))
              .put(Constants.CONFIG_DB_NAME, config.getString("DB_NAME", config.getString("database", "postgres")))
              .put(Constants.CONFIG_DB_USER, config.getString("DB_USER", config.getString("user", "postgres")))
              .put(Constants.CONFIG_DB_PASSWORD, config.getString("DB_PASSWORD", config.getString("password", "123")))
              .put(Constants.CONFIG_DB_POOL_SIZE, config.getInteger("DB_POOL_SIZE", config.getInteger("pool_size", 5)));
          } else {
            // Allow environment variables to override nested config values
            String envHost = config.getString("DB_HOST");
            if (envHost != null) dbConfig.put(Constants.CONFIG_DB_HOST, envHost);
            
            Integer envPort = config.getInteger("DB_PORT");
            if (envPort != null) dbConfig.put(Constants.CONFIG_DB_PORT, envPort);
            
            String envName = config.getString("DB_NAME");
            if (envName != null) dbConfig.put(Constants.CONFIG_DB_NAME, envName);
            
            String envUser = config.getString("DB_USER");
            if (envUser != null) dbConfig.put(Constants.CONFIG_DB_USER, envUser);
            
            String envPassword = config.getString("DB_PASSWORD");
            if (envPassword != null) dbConfig.put(Constants.CONFIG_DB_PASSWORD, envPassword);
            
            Integer envPoolSize = config.getInteger("DB_POOL_SIZE");
            if (envPoolSize != null) dbConfig.put(Constants.CONFIG_DB_POOL_SIZE, envPoolSize);
          }
          
          // Set auth config - use entire config object for auth
          authConfig = config;
          
          logger.debug("Database configuration: host={}, port={}, database={}", 
            dbConfig.getString(Constants.CONFIG_DB_HOST),
            dbConfig.getInteger(Constants.CONFIG_DB_PORT),
            dbConfig.getString(Constants.CONFIG_DB_NAME));
          
          // Initialize database connection
          initDatabase();
          
          // Initialize authentication
          initAuthentication();
          
          // Initialize handlers
          initHandlers();
          
          // Start HTTP server
          JsonObject httpConfig = config.getJsonObject("http", new JsonObject());
          startHttpServer(httpConfig, startPromise);
          
        } catch (Exception e) {
          logger.error("Failed to process configuration", e);
          startPromise.fail(e);
        }
      })
      .onFailure(err -> {
        logger.error("Failed to load configuration: {}", err.getMessage(), err);
        startPromise.fail(err);
      });
  }

  /**
   * Initialize database connection pool.
   */
  private void initDatabase() {
    logger.info("Initializing database connection pool...");
    
    try {
      // Check if we're in test mode and skip database initialization
      if (System.getProperty("test.mode") != null) {
        logger.info("Test mode detected - skipping database initialization");
        pool = null;
        return;
      }
      
      // Configure PostgreSQL connection from config file
      PgConnectOptions connectOptions = new PgConnectOptions()
        .setHost(dbConfig.getString(Constants.CONFIG_DB_HOST))
        .setPort(dbConfig.getInteger(Constants.CONFIG_DB_PORT))
        .setDatabase(dbConfig.getString(Constants.CONFIG_DB_NAME))
        .setUser(dbConfig.getString(Constants.CONFIG_DB_USER))
        .setPassword(dbConfig.getString(Constants.CONFIG_DB_PASSWORD));

      // Pool options
      PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(dbConfig.getInteger(Constants.CONFIG_DB_POOL_SIZE, 5));

      // Create the PostgreSQL pool using Pool interface (non-deprecated)
      pool = Pool.pool(vertx, connectOptions, poolOptions);
      
      logger.info("Database connection pool initialized: host={}, port={}, database={}, poolSize={}", 
        connectOptions.getHost(), 
        connectOptions.getPort(),
        connectOptions.getDatabase(),
        poolOptions.getMaxSize());
        
    } catch (Exception e) {
      logger.error("Failed to initialize database connection pool", e);
      throw e;
    }
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
    deviceHandler = new DeviceHandler(pool);
    telemetryHandler = new TelemetryHandler(pool);
    healthHandler = new HealthHandler();
    logger.info("Handlers initialized successfully");
  }

  /**
   * Start HTTP server with configured routes.
   */
  private void startHttpServer(JsonObject httpConfig, Promise<Void> startPromise) {
    logger.info("Starting HTTP server...");
    
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    
    // Configure router
    configureRouter(router);
    
    // Setup routes
    setupRoutes(router);
    
    // Start the HTTP server
    int port = httpConfig.getInteger("port", Constants.DEFAULT_HTTP_PORT);
    server.requestHandler(router).listen(port)
      .onSuccess(s -> {
        logger.info("Provider service started successfully on port {}", port);
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
      
      // Apply authentication to all /provider/api/* routes
      router.route(Constants.BASE_URL + "/*").handler(authHandler::authenticate);
      
      // REST API endpoints - protected by authentication
      router.post(Constants.DEVICES_ENDPOINT).handler(deviceHandler::createDevice);
      router.get(Constants.DEVICES_ENDPOINT).handler(deviceHandler::getAllDevices);
      router.put(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceHandler::updateDevice);
      router.delete(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceHandler::deleteDevice);
      router.get(Constants.TELEMETRY_BY_DEVICE_ENDPOINT).handler(telemetryHandler::getTelemetryByDevice);
      router.get(Constants.DATA_ENDPOINT).handler(healthHandler::getData);
      
      logger.info("Routes configured with authentication: POST {}, GET {}, PUT {}, DELETE {}, GET {}, GET {}, GET {}", 
        Constants.DEVICES_ENDPOINT,
        Constants.DEVICES_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.TELEMETRY_BY_DEVICE_ENDPOINT,
        Constants.DATA_ENDPOINT,
        Constants.HEALTH_ENDPOINT);
    } else {
      logger.warn("Authentication disabled or not configured - routes are publicly accessible!");
      
      // REST API endpoints without authentication (NOT RECOMMENDED FOR PRODUCTION)
      router.post(Constants.DEVICES_ENDPOINT).handler(deviceHandler::createDevice);
      router.get(Constants.DEVICES_ENDPOINT).handler(deviceHandler::getAllDevices);
      router.put(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceHandler::updateDevice);
      router.delete(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceHandler::deleteDevice);
      router.get(Constants.TELEMETRY_BY_DEVICE_ENDPOINT).handler(telemetryHandler::getTelemetryByDevice);
      router.get(Constants.DATA_ENDPOINT).handler(healthHandler::getData);
      
      logger.info("Routes configured WITHOUT authentication: POST {}, GET {}, PUT {}, DELETE {}, GET {}, GET {}, GET {}", 
        Constants.DEVICES_ENDPOINT,
        Constants.DEVICES_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.DEVICE_BY_ID_ENDPOINT,
        Constants.TELEMETRY_BY_DEVICE_ENDPOINT,
        Constants.DATA_ENDPOINT,
        Constants.HEALTH_ENDPOINT);
    }
  }

  @Override
  public void stop() {
    logger.info("Stopping Provider service...");
    
    if (pool != null) {
      pool.close();
      logger.info("Database connection pool closed");
    }
    
    logger.info("Provider service stopped");
  }
}
