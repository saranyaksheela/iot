package iot.provider;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iot.provider.config.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database connection and pool management unit tests.
 * These tests require Docker to be running for Testcontainers.
 */
@ExtendWith(VertxExtension.class)
@Testcontainers
@EnabledIfSystemProperty(named = "test.docker", matches = "true", 
    disabledReason = "Docker tests disabled. Run with -Dtest.docker=true to enable.")
@DisplayName("Database Connection Tests")
public class DatabaseConnectionTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-db.sql");

    private Pool pool;
    private JsonObject dbConfig;

    @BeforeEach
    void setUp(Vertx vertx) {
        dbConfig = new JsonObject()
                .put(Constants.CONFIG_DB_HOST, postgres.getHost())
                .put(Constants.CONFIG_DB_PORT, postgres.getFirstMappedPort())
                .put(Constants.CONFIG_DB_NAME, postgres.getDatabaseName())
                .put(Constants.CONFIG_DB_USER, postgres.getUsername())
                .put(Constants.CONFIG_DB_PASSWORD, postgres.getPassword())
                .put(Constants.CONFIG_DB_POOL_SIZE, 5);

        // Create connection pool
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setHost(dbConfig.getString(Constants.CONFIG_DB_HOST))
                .setPort(dbConfig.getInteger(Constants.CONFIG_DB_PORT))
                .setDatabase(dbConfig.getString(Constants.CONFIG_DB_NAME))
                .setUser(dbConfig.getString(Constants.CONFIG_DB_USER))
                .setPassword(dbConfig.getString(Constants.CONFIG_DB_PASSWORD));

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(dbConfig.getInteger(Constants.CONFIG_DB_POOL_SIZE));

        pool = Pool.pool(vertx, connectOptions, poolOptions);
    }

    @Test
    @DisplayName("Should successfully create database connection pool")
    void testConnectionPoolCreation(VertxTestContext testContext) {
        assertNotNull(pool, "Database pool should be created");
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should successfully connect to database")
    void testDatabaseConnection(VertxTestContext testContext) {
        pool.getConnection()
                .onSuccess(connection -> {
                    assertNotNull(connection, "Database connection should be established");
                    connection.close();
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @DisplayName("Should execute simple query successfully")
    void testSimpleQuery(VertxTestContext testContext) {
        pool.query("SELECT 1 as test_value")
                .execute()
                .onSuccess(rowSet -> {
                    assertNotNull(rowSet, "Query result should not be null");
                    assertTrue(rowSet.size() > 0, "Query should return at least one row");
                    
                    Row row = rowSet.iterator().next();
                    assertEquals(1, row.getInteger("test_value"), "Query should return correct value");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @DisplayName("Should handle invalid connection configuration gracefully")
    void testInvalidConnectionConfig(Vertx vertx, VertxTestContext testContext) {
        // Create invalid connection configuration
        PgConnectOptions invalidOptions = new PgConnectOptions()
                .setHost("invalid-host")
                .setPort(9999)
                .setDatabase("invalid-db")
                .setUser("invalid-user")
                .setPassword("invalid-password");

        PoolOptions poolOptions = new PoolOptions().setMaxSize(1);
        Pool invalidPool = Pool.pool(vertx, invalidOptions, poolOptions);

        invalidPool.getConnection()
                .onSuccess(connection -> {
                    connection.close();
                    testContext.failNow("Should not be able to connect with invalid configuration");
                })
                .onFailure(error -> {
                    assertNotNull(error, "Should receive connection error");
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("Should verify devices table exists")
    void testDevicesTableExists(VertxTestContext testContext) {
        pool.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'devices'")
                .execute()
                .onSuccess(rowSet -> {
                    Row row = rowSet.iterator().next();
                    long tableCount = row.getLong(0);
                    assertEquals(1L, tableCount, "Devices table should exist");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @DisplayName("Should verify telemetry_data table exists")
    void testTelemetryDataTableExists(VertxTestContext testContext) {
        pool.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'telemetry_data'")
                .execute()
                .onSuccess(rowSet -> {
                    Row row = rowSet.iterator().next();
                    long tableCount = row.getLong(0);
                    assertEquals(1L, tableCount, "Telemetry_data table should exist");
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @DisplayName("Should test connection pool limits")
    void testConnectionPoolLimits(Vertx vertx, VertxTestContext testContext) {
        PoolOptions limitedPoolOptions = new PoolOptions().setMaxSize(2);
        Pool limitedPool = Pool.pool(vertx, 
                new PgConnectOptions()
                        .setHost(dbConfig.getString(Constants.CONFIG_DB_HOST))
                        .setPort(dbConfig.getInteger(Constants.CONFIG_DB_PORT))
                        .setDatabase(dbConfig.getString(Constants.CONFIG_DB_NAME))
                        .setUser(dbConfig.getString(Constants.CONFIG_DB_USER))
                        .setPassword(dbConfig.getString(Constants.CONFIG_DB_PASSWORD)),
                limitedPoolOptions);

        // Try to get multiple connections
        limitedPool.getConnection()
                .compose(conn1 -> {
                    return limitedPool.getConnection()
                            .onSuccess(conn2 -> {
                                conn1.close();
                                conn2.close();
                                testContext.completeNow();
                            });
                })
                .onFailure(testContext::failNow);
    }
}