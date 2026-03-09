# Docker Architecture Diagram

```
╔════════════════════════════════════════════════════════════════════════════╗
║                      IOT SERVICES DOCKER ARCHITECTURE                      ║
╚════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────┐
│                          HOST MACHINE (Windows)                          │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                    Docker Network: iot-network                     │ │
│  │                          (Bridge Mode)                             │ │
│  │                                                                    │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────────┐ │ │
│  │  │   PostgreSQL     │  │   Provider       │  │   Subscriber    │ │ │
│  │  │   Container      │  │   Service        │  │   Service       │ │ │
│  │  │                  │  │                  │  │                 │ │ │
│  │  │  iot-postgres    │  │  iot-provider    │  │ iot-subscriber  │ │ │
│  │  ├──────────────────┤  ├──────────────────┤  ├─────────────────┤ │ │
│  │  │ Image:           │  │ Built from:      │  │ Built from:     │ │ │
│  │  │ postgres:15      │  │ provider/        │  │ subscriber/     │ │ │
│  │  │ -alpine          │  │ Dockerfile       │  │ Dockerfile      │ │ │
│  │  ├──────────────────┤  ├──────────────────┤  ├─────────────────┤ │ │
│  │  │ Database:        │  │ Java 17 JRE      │  │ Java 17 JRE     │ │ │
│  │  │ - postgres       │  │ Vert.x 4.5.6     │  │ Vert.x 4.5.6    │ │ │
│  │  │                  │  │                  │  │                 │ │ │
│  │  │ Tables:          │  │ Endpoints:       │  │ Endpoints:      │ │ │
│  │  │ - devices        │  │ /health          │  │ /health         │ │ │
│  │  │ - telemetry_data │  │ /provider/api/*  │  │ /subscriber/api/│ │ │
│  │  ├──────────────────┤  ├──────────────────┤  ├─────────────────┤ │ │
│  │  │ Memory:          │  │ Memory:          │  │ Memory:         │ │ │
│  │  │ ~100MB           │  │ 256MB-512MB      │  │ 256MB-512MB     │ │ │
│  │  ├──────────────────┤  ├──────────────────┤  ├─────────────────┤ │ │
│  │  │ Volume:          │  │ Volume:          │  │ Volume:         │ │ │
│  │  │ postgres_data    │  │ provider_logs    │  │ subscriber_logs │ │ │
│  │  ├──────────────────┤  ├──────────────────┤  ├─────────────────┤ │ │
│  │  │ Port Mapping:    │  │ Port Mapping:    │  │ Port Mapping:   │ │ │
│  │  │ 5432:5432        │  │ 8080:8080        │  │ 8081:8081       │ │ │
│  │  └──────────────────┘  └──────────────────┘  └─────────────────┘ │ │
│  │         │                       │                      │          │ │
│  │         │ SQL Queries           │ HTTP Requests        │          │ │
│  │         │ Connection Pool       │ WebClient            │          │ │
│  │         └───────────────────────┘                      │          │ │
│  │                                  └──────────────────────┘          │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                          Port Mappings                             │ │
│  ├────────────────────────────────────────────────────────────────────┤ │
│  │  localhost:5432   ───────► PostgreSQL (iot-postgres)              │ │
│  │  localhost:8080   ───────► Provider    (iot-provider)             │ │
│  │  localhost:8081   ───────► Subscriber  (iot-subscriber)           │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                          Named Volumes                             │ │
│  ├────────────────────────────────────────────────────────────────────┤ │
│  │  iot-postgres-data    ──► PostgreSQL data persistence             │ │
│  │  iot-provider-logs    ──► Provider application logs               │ │
│  │  iot-subscriber-logs  ──► Subscriber application logs             │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════════════════╗
║                           REQUEST FLOW DIAGRAM                             ║
╚════════════════════════════════════════════════════════════════════════════╝

Client Request Flow:
───────────────────

1. Direct to Provider:
   
   [Client]
      │
      │ HTTP POST /provider/api/devices
      │ Header: X-API-Key: pk_live_12345abcdef67890provider
      │ Body: {"deviceName": "Sensor1", "deviceType": "temp"}
      ▼
   [Provider:8080]
      │
      ├─► [AuthHandler] ──► Validate API Key
      │                     └─► 401 Unauthorized (if invalid)
      │                     └─► Continue (if valid)
      ▼
   [DeviceHandler]
      │
      ├─► Validate Input
      ├─► Generate UUID
      ├─► Sanitize Data
      ▼
   [PostgreSQL:5432]
      │
      ├─► INSERT INTO devices...
      ├─► Return created device
      ▼
   [Response: 201 Created]
      │
      └─► {"id":1,"deviceUuid":"...","deviceName":"Sensor1",...}


2. Via Subscriber Proxy:
   
   [Client]
      │
      │ HTTP POST /subscriber/api/devices
      │ Header: X-API-Key: sk_live_client_app_key_12345
      │ Body: {"deviceName": "Sensor2", "deviceType": "humidity"}
      ▼
   [Subscriber:8081]
      │
      ├─► [AuthHandler] ──► Validate Client API Key
      │                     └─► 401 Unauthorized (if invalid)
      ▼
   [DeviceProxyHandler]
      │
      ├─► Validate Request
      ├─► Forward to Provider
      │   HTTP POST http://provider:8080/provider/api/devices
      │   Header: X-API-Key: pk_live_subscriber987654321xyz
      ▼
   [Provider:8080]
      │
      ├─► [AuthHandler] ──► Validate Subscriber API Key
      ├─► [DeviceHandler] ──► Process Request
      ├─► [PostgreSQL:5432] ──► Save to DB
      ▼
   [Response to Subscriber]
      │
      ▼
   [Forward Response to Client]
      │
      └─► {"id":2,"deviceUuid":"...","deviceName":"Sensor2",...}


╔════════════════════════════════════════════════════════════════════════════╗
║                        DATABASE SCHEMA DIAGRAM                             ║
╚════════════════════════════════════════════════════════════════════════════╝

PostgreSQL Database: postgres
─────────────────────────────

┌─────────────────────────────────────────────────────────────────┐
│                           devices                               │
├──────────────────┬─────────────────────────┬──────────────────┤
│ Column           │ Type                    │ Constraints      │
├──────────────────┼─────────────────────────┼──────────────────┤
│ id               │ BIGSERIAL               │ PRIMARY KEY      │
│ device_uuid      │ UUID                    │ UNIQUE, NOT NULL │
│ device_name      │ VARCHAR(100)            │ NOT NULL         │
│ device_type      │ VARCHAR(50)             │ NOT NULL         │
│ firmware_version │ VARCHAR(50)             │                  │
│ location         │ VARCHAR(100)            │                  │
│ status           │ VARCHAR(20)             │ DEFAULT 'active' │
│ created_at       │ TIMESTAMP               │ DEFAULT NOW()    │
└──────────────────┴─────────────────────────┴──────────────────┘
                              │
                              │ Foreign Key
                              │ (device_id)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        telemetry_data                           │
├──────────────────┬─────────────────────────┬──────────────────┤
│ Column           │ Type                    │ Constraints      │
├──────────────────┼─────────────────────────┼──────────────────┤
│ id               │ BIGSERIAL               │ PRIMARY KEY      │
│ device_id        │ BIGINT                  │ FOREIGN KEY      │
│ topic_id         │ BIGINT                  │ NOT NULL         │
│ payload          │ JSONB                   │                  │
│ received_at      │ TIMESTAMP               │ DEFAULT NOW()    │
└──────────────────┴─────────────────────────┴──────────────────┘

Indexes:
────────
• idx_devices_uuid          ON devices(device_uuid)
• idx_devices_type          ON devices(device_type)
• idx_devices_status        ON devices(status)
• idx_telemetry_device_id   ON telemetry_data(device_id)
• idx_telemetry_received_at ON telemetry_data(received_at DESC)
• idx_telemetry_payload     ON telemetry_data USING GIN (payload)


╔════════════════════════════════════════════════════════════════════════════╗
║                      AUTHENTICATION ARCHITECTURE                           ║
╚════════════════════════════════════════════════════════════════════════════╝

Two-Layer Authentication:
─────────────────────────

Layer 1: Client to Subscriber
┌──────────┐
│  Client  │
│          │
│ API Key: │
│ sk_live_ │
│ client_  │
│ app_key_ │
│ 12345    │
└────┬─────┘
     │
     │ X-API-Key Header
     ▼
┌────────────────┐
│  Subscriber    │
│  AuthHandler   │
│                │
│ Validates:     │
│ - Key exists   │
│ - Key enabled  │
│ - Not expired  │
└────┬───────────┘
     │
     │ If Valid
     ▼
┌────────────────┐
│ Forward to     │
│ Provider       │
└────────────────┘


Layer 2: Subscriber to Provider
┌──────────────┐
│ Subscriber   │
│              │
│ Uses:        │
│ pk_live_     │
│ subscriber   │
│ 987654321xyz │
└──────┬───────┘
       │
       │ X-API-Key Header
       ▼
┌────────────────┐
│   Provider     │
│   AuthHandler  │
│                │
│ Validates:     │
│ - Key exists   │
│ - Key enabled  │
│ - Not expired  │
└────┬───────────┘
     │
     │ If Valid
     ▼
┌────────────────┐
│ Process        │
│ Request        │
└────────────────┘


API Keys Configuration:
───────────────────────

Provider (auth-config.json):
├── provider-service      : pk_live_12345abcdef67890provider
├── subscriber-service    : pk_live_subscriber987654321xyz (used by Subscriber)
├── admin-client          : pk_live_admin_secret_key_2026
├── test-client           : pk_test_development_only_key
└── disabled-client       : [DISABLED]

Subscriber (auth-config.json):
├── providerApiKey        : pk_live_subscriber987654321xyz (for Provider access)
├── client-app            : sk_live_client_app_key_12345
├── mobile-app            : sk_live_mobile_app_key_67890
├── web-dashboard         : sk_live_dashboard_key_abcdef
└── test-client           : sk_test_development_client_key


╔════════════════════════════════════════════════════════════════════════════╗
║                       DEPLOYMENT FILE STRUCTURE                            ║
╚════════════════════════════════════════════════════════════════════════════╝

provider/
├── Dockerfile                     Multi-stage build (Maven + JRE)
├── docker-compose.yml             Orchestrates all 3 services
├── init-db.sql                    Database initialization
├── deploy.bat                     Interactive deployment manager
├── test-services.bat              Automated test suite
├── .dockerignore                  Build optimization
├── DOCKER_DEPLOYMENT.md           Comprehensive guide
├── DOCKER_QUICK_REF.md            Quick reference
├── DOCKER_SETUP_COMPLETE.md       Setup summary
└── DOCKER_ARCHITECTURE.md         This file

subscriber/
├── Dockerfile                     Multi-stage build (Maven + JRE)
└── .dockerignore                  Build optimization


╔════════════════════════════════════════════════════════════════════════════╗
║                         CONTAINER LIFECYCLE                                ║
╚════════════════════════════════════════════════════════════════════════════╝

Startup Sequence:
─────────────────

1. PostgreSQL Container
   ├─► Start container
   ├─► Initialize database
   ├─► Run init-db.sql
   ├─► Create tables and indexes
   ├─► Insert sample data
   └─► Health check: pg_isready
        └─► Ready in ~10 seconds

2. Provider Container (depends on PostgreSQL)
   ├─► Wait for PostgreSQL health
   ├─► Start Java application
   ├─► Load configurations
   ├─► Initialize database pool
   ├─► Start HTTP server on 8080
   └─► Health check: wget /health
        └─► Ready in ~30-40 seconds

3. Subscriber Container (depends on Provider)
   ├─► Wait for Provider health
   ├─► Start Java application
   ├─► Load configurations
   ├─► Initialize WebClient
   ├─► Start HTTP server on 8081
   └─► Health check: wget /health
        └─► Ready in ~20-30 seconds

Total Startup Time: ~60-90 seconds


Shutdown Sequence:
──────────────────

1. docker-compose down
   ├─► Stop Subscriber (graceful)
   │    └─► Close WebClient
   ├─► Stop Provider (graceful)
   │    └─► Close database pool
   ├─► Stop PostgreSQL
   └─► Remove network (optional)


Health Check Mechanism:
───────────────────────

Each container has:
├─► Interval: 30 seconds
├─► Timeout: 3-5 seconds
├─► Retries: 3-5 attempts
├─► Start Period: 10-40 seconds
└─► Command: Service-specific check

PostgreSQL: pg_isready -U postgres
Provider:   wget /health
Subscriber: wget /health


╔════════════════════════════════════════════════════════════════════════════╗
║                           BUILD PROCESS                                    ║
╚════════════════════════════════════════════════════════════════════════════╝

Multi-Stage Docker Build:
─────────────────────────

Stage 1: Build Stage (maven:3.9-eclipse-temurin-17)
┌────────────────────────────────────────┐
│ 1. Copy pom.xml                        │
│ 2. Download dependencies (cached)      │
│ 3. Copy source code                    │
│ 4. mvn clean package -DskipTests       │
│ 5. Generate JAR file                   │
└────────────────────────────────────────┘
                  │
                  │ Copy artifacts
                  ▼
Stage 2: Runtime Stage (eclipse-temurin:17-jre-alpine)
┌────────────────────────────────────────┐
│ 1. Create non-root user                │
│ 2. Copy JAR from build stage           │
│ 3. Copy configuration files            │
│ 4. Create log directories              │
│ 5. Set permissions                     │
│ 6. Configure health check              │
│ 7. Set entry point                     │
└────────────────────────────────────────┘
                  │
                  ▼
            Final Image
        (~150-200 MB)

Benefits:
├─► Smaller image size (no Maven, no source)
├─► Faster deployment
├─► Better security (minimal attack surface)
└─► Layer caching for fast rebuilds


═══════════════════════════════════════════════════════════════════════════════

End of Architecture Documentation
Generated: March 9, 2026
Docker Compose Version: 3.8
Container Runtime: Docker Desktop for Windows

═══════════════════════════════════════════════════════════════════════════════
```
