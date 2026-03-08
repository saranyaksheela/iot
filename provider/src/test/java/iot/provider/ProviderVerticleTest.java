package iot.provider;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
class ProviderVerticleTest {

  private Vertx vertx;

  @BeforeEach
  void setup(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new ProviderVerticle(), testContext.succeedingThenComplete());
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    vertx.close(testContext.succeedingThenComplete());
  }

  @Test
  void testHealthEndpoint(VertxTestContext testContext) {
    WebClient client = WebClient.create(vertx);
    client.get(8080, "localhost", "/health")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        testContext.completeNow();
      })));
  }

  @Test
  void testDataEndpoint(VertxTestContext testContext) {
    WebClient client = WebClient.create(vertx);
    client.get(8080, "localhost", "/api/data")
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.getHeader("content-type"));
        testContext.completeNow();
      })));
  }
}
