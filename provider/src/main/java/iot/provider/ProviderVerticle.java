package iot.provider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
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
  private DeviceHandler deviceHandler;
  private TelemetryHandler telemetryHandler;
  private HealthHandler healthHandler;

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting Provider service...");
    
    // Load database configuration from file
    vertx.fileSystem().readFile("src/main/resources/db-config.json")
      .onSuccess(buffer -> {
        try {
          JsonObject config = buffer.toJsonObject();
          dbConfig = config.getJsonObject(Constants.CONFIG_DB);
          
          logger.debug("Configuration loaded successfully");
          
          // Initialize database connection
          initDatabase();
          
          // Initialize handlers
          initHandlers();
          
          // Start HTTP server
          startHttpServer(config, startPromise);
          
        } catch (Exception e) {
          logger.error("Failed to parse configuration", e);
          startPromise.fail(e);
        }
      })
      .onFailure(err -> {
        logger.error("Failed to load config file: {}", err.getMessage(), err);
        startPromise.fail(err);
      });
  }

  /**
   * Initialize database connection pool.
   */
  private void initDatabase() {
    logger.info("Initializing database connection pool...");
    
    try {
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
  private void startHttpServer(JsonObject config, Promise<Void> startPromise) {
    logger.info("Starting HTTP server...");
    
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    
    // Configure router
    configureRouter(router);
    
    // Setup routes
    setupRoutes(router);
    
    // Start the HTTP server
    int port = config.getJsonObject("http").getInteger("port", Constants.DEFAULT_HTTP_PORT);
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
    
    // REST API endpoints with base URL - delegate to handlers
    router.post(Constants.DEVICES_ENDPOINT).handler(deviceHandler::createDevice);
    router.get(Constants.DEVICES_ENDPOINT).handler(deviceHandler::getAllDevices);
    router.put(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceHandler::updateDevice);
    router.delete(Constants.DEVICE_BY_ID_ENDPOINT).handler(deviceHandler::deleteDevice);
    router.get(Constants.TELEMETRY_BY_DEVICE_ENDPOINT).handler(telemetryHandler::getTelemetryByDevice);
    router.get(Constants.DATA_ENDPOINT).handler(healthHandler::getData);
    
    // Health check endpoint (no base URL for health)
    router.get(Constants.HEALTH_ENDPOINT).handler(healthHandler::healthCheck);
    
    logger.info("Routes configured: POST {}, GET {}, PUT {}, DELETE {}, GET {}, GET {}, GET {}", 
      Constants.DEVICES_ENDPOINT,
      Constants.DEVICES_ENDPOINT,
      Constants.DEVICE_BY_ID_ENDPOINT,
      Constants.DEVICE_BY_ID_ENDPOINT,
      Constants.TELEMETRY_BY_DEVICE_ENDPOINT,
      Constants.DATA_ENDPOINT,
      Constants.HEALTH_ENDPOINT);
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
