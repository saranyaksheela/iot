package iot.provider;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for the Provider service.
 */
public class ProviderApp {
  
  private static final Logger logger = LoggerFactory.getLogger(ProviderApp.class);
  
  public static void main(String[] args) {
    logger.info("Starting Provider Application...");
    
    try {
      Vertx vertx = Vertx.vertx();
      vertx.deployVerticle(new ProviderVerticle())
        .onSuccess(id -> logger.info("ProviderVerticle deployed successfully with ID: {}", id))
        .onFailure(err -> {
          logger.error("Failed to deploy ProviderVerticle", err);
          System.exit(1);
        });
    } catch (Exception e) {
      logger.error("Fatal error during application startup", e);
      System.exit(1);
    }
  }
}
