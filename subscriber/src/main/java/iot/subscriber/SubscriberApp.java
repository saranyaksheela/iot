package iot.subscriber;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for the Subscriber service.
 */
public class SubscriberApp {
  
  private static final Logger logger = LoggerFactory.getLogger(SubscriberApp.class);
  
  public static void main(String[] args) {
    logger.info("Starting Subscriber Application...");
    
    try {
      Vertx vertx = Vertx.vertx();
      vertx.deployVerticle(new SubscriberVerticle())
        .onSuccess(id -> logger.info("SubscriberVerticle deployed successfully with ID: {}", id))
        .onFailure(err -> {
          logger.error("Failed to deploy SubscriberVerticle", err);
          System.exit(1);
        });
    } catch (Exception e) {
      logger.error("Fatal error during application startup", e);
      System.exit(1);
    }
  }
}
