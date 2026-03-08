package iot.subscriber;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import iot.subscriber.config.Constants;
import iot.subscriber.handler.DataFetchHandler;
import iot.subscriber.handler.DeviceProxyHandler;
import iot.subscriber.handler.HealthHandler;
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
  private DeviceProxyHandler deviceProxyHandler;
  private DataFetchHandler dataFetchHandler;
  private HealthHandler healthHandler;

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting Subscriber service...");
    
    try {
      // Initialize WebClient
      initWebClient();
      
      // Initialize handlers
      initHandlers();
      
      // Start HTTP server
      startHttpServer(startPromise);
      
    } catch (Exception e) {
      logger.error("Failed to start Subscriber service", e);
      startPromise.fail(e);
    }
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
   * Initialize business logic handlers.
   */
  private void initHandlers() {
    logger.info("Initializing handlers...");
    deviceProxyHandler = new DeviceProxyHandler(webClient);
    dataFetchHandler = new DataFetchHandler(webClient);
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
    router.post(Constants.DEVICES_ENDPOINT).handler(deviceProxyHandler::createDevice);
    router.get(Constants.FETCH_ENDPOINT).handler(dataFetchHandler::fetchFromProvider);
    router.get(Constants.STATUS_ENDPOINT).handler(healthHandler::getStatus);
    
    // Health check endpoint (no base URL for health)
    router.get(Constants.HEALTH_ENDPOINT).handler(healthHandler::healthCheck);
    
    logger.info("Routes configured: POST {}, GET {}, GET {}, GET {}", 
      Constants.DEVICES_ENDPOINT,
      Constants.FETCH_ENDPOINT,
      Constants.STATUS_ENDPOINT,
      Constants.HEALTH_ENDPOINT);
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
