package iot.provider.config;

public class Constants {
  
  // Base URL
  public static final String BASE_URL = "/provider/api";
  
  // API Endpoints
  public static final String DEVICES_ENDPOINT = BASE_URL + "/devices";
  public static final String DEVICE_BY_ID_ENDPOINT = BASE_URL + "/devices/:id";
  public static final String TELEMETRY_ENDPOINT = BASE_URL + "/telemetry";
  public static final String TELEMETRY_BY_DEVICE_ENDPOINT = BASE_URL + "/telemetry/device/:deviceId";
  public static final String DATA_ENDPOINT = BASE_URL + "/data";
  public static final String HEALTH_ENDPOINT = "/health";
  
  // HTTP Headers
  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String HEADER_CONTENT_TYPE = "content-type";
  
  // HTTP Status Codes
  public static final int STATUS_OK = 200;
  public static final int STATUS_CREATED = 201;
  public static final int STATUS_BAD_REQUEST = 400;
  public static final int STATUS_NOT_FOUND = 404;
  public static final int STATUS_INTERNAL_SERVER_ERROR = 500;
  
  // Default Values
  public static final String DEFAULT_STATUS = "active";
  public static final int DEFAULT_HTTP_PORT = 8080;
  
  // Database Queries
  public static final String INSERT_DEVICE_QUERY = 
    "INSERT INTO devices (device_uuid, device_name, device_type, firmware_version, location, status) " +
    "VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, device_uuid, device_name, device_type, firmware_version, location, status, created_at";
  
  public static final String SELECT_ALL_DEVICES_QUERY = 
    "SELECT id, device_uuid, device_name, device_type, firmware_version, location, status, created_at " +
    "FROM devices ORDER BY created_at DESC";
  
  public static final String UPDATE_DEVICE_QUERY = 
    "UPDATE devices SET device_name = $1 WHERE id = $2 " +
    "RETURNING id, device_uuid, device_name, device_type, firmware_version, location, status, created_at";
  
  public static final String DELETE_DEVICE_QUERY = 
    "DELETE FROM devices WHERE id = $1 RETURNING id";
  
  public static final String SELECT_TELEMETRY_BY_DEVICE_QUERY = 
    "SELECT id, device_id, topic_id, payload, received_at " +
    "FROM telemetry_data WHERE device_id = $1 ORDER BY received_at DESC";
  
  // JSON Keys
  public static final String JSON_KEY_ID = "id";
  public static final String JSON_KEY_DEVICE_UUID = "deviceUuid";
  public static final String JSON_KEY_DEVICE_NAME = "deviceName";
  public static final String JSON_KEY_DEVICE_TYPE = "deviceType";
  public static final String JSON_KEY_FIRMWARE_VERSION = "firmwareVersion";
  public static final String JSON_KEY_LOCATION = "location";
  public static final String JSON_KEY_STATUS = "status";
  public static final String JSON_KEY_CREATED_AT = "createdAt";
  public static final String JSON_KEY_DEVICES = "devices";
  public static final String JSON_KEY_COUNT = "count";
  public static final String JSON_KEY_MESSAGE = "message";
  public static final String JSON_KEY_ERROR = "error";
  public static final String JSON_KEY_TIMESTAMP = "timestamp";
  public static final String JSON_KEY_DEVICE_ID = "deviceId";
  public static final String JSON_KEY_TOPIC_ID = "topicId";
  public static final String JSON_KEY_PAYLOAD = "payload";
  public static final String JSON_KEY_RECEIVED_AT = "receivedAt";
  public static final String JSON_KEY_TELEMETRY = "telemetry";
  
  // Database Column Names
  public static final String DB_COL_ID = "id";
  public static final String DB_COL_DEVICE_UUID = "device_uuid";
  public static final String DB_COL_DEVICE_NAME = "device_name";
  public static final String DB_COL_DEVICE_TYPE = "device_type";
  public static final String DB_COL_FIRMWARE_VERSION = "firmware_version";
  public static final String DB_COL_LOCATION = "location";
  public static final String DB_COL_STATUS = "status";
  public static final String DB_COL_CREATED_AT = "created_at";
  public static final String DB_COL_DEVICE_ID = "device_id";
  public static final String DB_COL_TOPIC_ID = "topic_id";
  public static final String DB_COL_PAYLOAD = "payload";
  public static final String DB_COL_RECEIVED_AT = "received_at";
  
  // Config Keys
  public static final String CONFIG_HTTP_PORT = "http.port";
  public static final String CONFIG_DB = "database";
  public static final String CONFIG_DB_HOST = "host";
  public static final String CONFIG_DB_PORT = "port";
  public static final String CONFIG_DB_NAME = "database";
  public static final String CONFIG_DB_USER = "user";
  public static final String CONFIG_DB_PASSWORD = "password";
  public static final String CONFIG_DB_POOL_SIZE = "pool_size";
  
  private Constants() {
    // Private constructor to prevent instantiation
  }
}
