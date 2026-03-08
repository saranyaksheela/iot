package iot.subscriber.config;

public class Constants {
  
  // Base URL
  public static final String BASE_URL = "/subscriber/api";
  
  // API Endpoints
  public static final String DEVICES_ENDPOINT = BASE_URL + "/devices";
  public static final String DEVICE_BY_ID_ENDPOINT = BASE_URL + "/devices/:id";
  public static final String TELEMETRY_BY_DEVICE_ENDPOINT = BASE_URL + "/telemetry/device/:deviceId";
  public static final String FETCH_ENDPOINT = BASE_URL + "/fetch";
  public static final String STATUS_ENDPOINT = BASE_URL + "/status";
  public static final String HEALTH_ENDPOINT = "/health";
  
  // Provider Service Configuration
  public static final String PROVIDER_HOST = "localhost";
  public static final int PROVIDER_PORT = 8080;
  public static final String PROVIDER_DEVICES_PATH = "/provider/api/devices";
  public static final String PROVIDER_DATA_PATH = "/provider/api/data";
  public static final String PROVIDER_TELEMETRY_PATH = "/provider/api/telemetry/device";
  
  // HTTP Headers
  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String HEADER_CONTENT_TYPE = "content-type";
  public static final String HEADER_API_KEY = "X-API-Key";
  public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
  
  // HTTP Status Codes
  public static final int STATUS_OK = 200;
  public static final int STATUS_UNAUTHORIZED = 401;
  public static final int STATUS_INTERNAL_SERVER_ERROR = 500;
  
  // Default Values
  public static final int DEFAULT_HTTP_PORT = 8081;
  
  // JSON Keys
  public static final String JSON_KEY_STATUS = "status";
  public static final String JSON_KEY_SERVICE = "service";
  public static final String JSON_KEY_MESSAGE = "message";
  public static final String JSON_KEY_TIMESTAMP = "timestamp";
  public static final String JSON_KEY_ERROR = "error";
  public static final String JSON_KEY_SOURCE = "source";
  public static final String JSON_KEY_DATA = "data";
  
  // Config Keys
  public static final String CONFIG_HTTP_PORT = "http.port";
  public static final String CONFIG_AUTH = "authentication";
  public static final String CONFIG_AUTH_ENABLED = "enabled";
  
  // Service Information
  public static final String SERVICE_NAME = "subscriber";
  public static final String SERVICE_STATUS_UP = "UP";
  public static final String SERVICE_GREETING = "Hello from Subscriber Service";
  
  private Constants() {
    // Private constructor to prevent instantiation
  }
}
