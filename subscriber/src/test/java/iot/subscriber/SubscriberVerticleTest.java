package iot.subscriber;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
class SubscriberVerticleTest {

  private Vertx vertx;

  @BeforeEach
  void setup(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new SubscriberVerticle(), testContext.succeedingThenComplete());
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    vertx.close(testContext.succeedingThenComplete());
  }

  @Test
  void testHealthEndpoint(VertxTestContext testContext) {
    WebClient client = WebClient.create(vertx);
    client.get(8081, "localhost", "/health")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  void testStatusEndpoint(VertxTestContext testContext) {
    WebClient client = WebClient.create(vertx);
    client.get(8081, "localhost", "/api/status")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.getHeader("content-type"));
        testContext.completeNow();
      })));
  }
}
