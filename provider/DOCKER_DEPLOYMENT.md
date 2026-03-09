# Docker Deployment Guide
## IoT Provider and Subscriber Services

This guide provides comprehensive instructions for deploying the IoT services using Docker.

---

## 📋 Prerequisites

- **Docker Desktop** (Windows): Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher (included with Docker Desktop)
- **curl**: For testing API endpoints (included with Windows 10+)
- **At least 2GB free RAM** for Docker containers
- **Ports Available**: 5432 (PostgreSQL), 8080 (Provider), 8081 (Subscriber)

---

## 🚀 Quick Start

### Option 1: Using the Deployment Script (Recommended)

1. **Open Command Prompt** in the `provider` directory
2. **Run the deployment script**:
   ```cmd
   deploy.bat
   ```
3. **Select option 10** for Full Deployment (Build + Start + Test)
4. **Wait** for services to start (approximately 2-3 minutes)

### Option 2: Manual Deployment

1. **Build and start services**:
   ```cmd
   cd provider
   docker-compose up -d --build
   ```

2. **Check service status**:
   ```cmd
   docker-compose ps
   ```

3. **Run tests**:
   ```cmd
   test-services.bat
   ```

---

## 📁 File Structure

```
provider/
├── Dockerfile                 # Provider service Docker image
├── docker-compose.yml         # Multi-container orchestration
├── init-db.sql               # Database initialization script
├── deploy.bat                # Comprehensive deployment manager
├── test-services.bat         # Automated test suite
├── .dockerignore             # Docker build exclusions
└── DOCKER_DEPLOYMENT.md      # This file

subscriber/
├── Dockerfile                # Subscriber service Docker image
└── .dockerignore            # Docker build exclusions
```

---

## 🐳 Docker Architecture

### Services Overview

```
┌─────────────────────────────────────────────────────┐
│                 Docker Network                      │
│                   (iot-network)                     │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  PostgreSQL  │  │   Provider   │  │Subscriber│ │
│  │   Port 5432  │◄─┤   Port 8080  │◄─┤Port 8081 │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│         │                  │                │       │
└─────────┼──────────────────┼────────────────┼───────┘
          │                  │                │
          ▼                  ▼                ▼
    localhost:5432    localhost:8080  localhost:8081
```

### Container Details

**PostgreSQL Container (`iot-postgres`)**
- Image: `postgres:15-alpine`
- Port: `5432:5432`
- Database: `postgres`
- User: `postgres`
- Password: `123`
- Volume: `iot-postgres-data`

**Provider Container (`iot-provider`)**
- Built from: `provider/Dockerfile`
- Port: `8080:8080`
- JVM Memory: 256MB-512MB
- Health Check: `/health` endpoint
- Volume: `iot-provider-logs`

**Subscriber Container (`iot-subscriber`)**
- Built from: `subscriber/Dockerfile`
- Port: `8081:8081`
- JVM Memory: 256MB-512MB
- Health Check: `/health` endpoint
- Volume: `iot-subscriber-logs`

---

## 🎮 Using deploy.bat

The `deploy.bat` script provides a comprehensive menu-driven interface:

### Main Menu Options

1. **Build Docker Images** - Build/rebuild container images
2. **Start Services (with build)** - Build and start all services
3. **Start Services (without build)** - Start existing containers
4. **Stop Services** - Stop all running containers
5. **Restart Services** - Restart all containers
6. **View Logs** - View container logs (real-time)
7. **Run Tests** - Execute automated test suite
8. **Health Check** - Check service health status
9. **Clean Up** - Remove containers, volumes, and images
10. **Full Deployment** - Complete build, start, and test cycle
11. **Show Service Status** - Display container status and stats
12. **View Service URLs** - Show all endpoints and credentials
13. **Backup Database** - Create PostgreSQL backup
14. **Exit** - Exit the script

### Example Usage

```cmd
C:\Users\USER\Gokul\provider> deploy.bat

============================================================================
 IoT Services Docker Deployment Manager
============================================================================
 1. Build Docker Images
 2. Start Services (with build)
 ...
 10. Full Deployment (Build + Start + Test)
 ...
============================================================================
Enter your choice (1-14): 10
```

---

## 🧪 Testing

### Automated Testing

Run the automated test suite:

```cmd
test-services.bat
```

The test suite includes:
- ✅ Health checks (Provider, Subscriber, PostgreSQL)
- ✅ API endpoint tests
- ✅ Authentication tests
- ✅ Database connectivity tests
- ✅ Error handling tests
- ✅ Performance tests

### Test Output

```
============================================================================
 Test Results Summary
============================================================================
 Total Tests:  13
 Passed:       13
 Failed:       0
 Pass Rate:    100%
 Status: ALL TESTS PASSED ✓
============================================================================
```

### Manual Testing

**Health Checks:**
```cmd
curl http://localhost:8080/health
curl http://localhost:8081/health
```

**Get All Devices:**
```cmd
curl -H "X-API-Key: pk_live_12345abcdef67890provider" ^
     http://localhost:8080/provider/api/devices
```

**Create Device:**
```cmd
curl -X POST ^
     -H "Content-Type: application/json" ^
     -H "X-API-Key: pk_live_12345abcdef67890provider" ^
     -d "{\"deviceName\":\"Test Sensor\",\"deviceType\":\"temperature\",\"firmwareVersion\":\"1.0.0\",\"location\":\"Lab 1\",\"status\":\"active\"}" ^
     http://localhost:8080/provider/api/devices
```

**Create Device via Subscriber:**
```cmd
curl -X POST ^
     -H "Content-Type: application/json" ^
     -H "X-API-Key: sk_live_client_app_key_12345" ^
     -d "{\"deviceName\":\"Proxy Sensor\",\"deviceType\":\"humidity\",\"firmwareVersion\":\"2.0.0\"}" ^
     http://localhost:8081/subscriber/api/devices
```

---

## 🔑 API Keys

### Provider API Keys
```
Service Key:      pk_live_12345abcdef67890provider
Subscriber Key:   pk_live_subscriber987654321xyz
Admin Key:        pk_live_admin_secret_key_2026
Test Key:         pk_test_development_only_key
```

### Subscriber API Keys
```
Client App:       sk_live_client_app_key_12345
Mobile App:       sk_live_mobile_app_key_67890
Web Dashboard:    sk_live_dashboard_key_abcdef
Test Client:      sk_test_development_client_key
```

---

## 📊 Monitoring

### View Logs

**All services:**
```cmd
cd provider
docker-compose logs -f
```

**Specific service:**
```cmd
docker-compose logs -f provider
docker-compose logs -f subscriber
docker-compose logs -f postgres
```

### Check Container Status

```cmd
docker-compose ps
```

### View Resource Usage

```cmd
docker stats iot-postgres iot-provider iot-subscriber
```

### Access Container Shell

```cmd
docker exec -it iot-provider sh
docker exec -it iot-subscriber sh
docker exec -it iot-postgres psql -U postgres
```

---

## 🗄️ Database Management

### Connect to Database

```cmd
docker exec -it iot-postgres psql -U postgres
```

### Run SQL Queries

```sql
-- View all devices
SELECT * FROM devices;

-- View telemetry data
SELECT * FROM telemetry_data ORDER BY received_at DESC LIMIT 10;

-- Count devices by type
SELECT device_type, COUNT(*) FROM devices GROUP BY device_type;
```

### Backup Database

**Using deploy.bat:**
- Select option 13 from the menu

**Manual backup:**
```cmd
docker exec iot-postgres pg_dump -U postgres postgres > backup.sql
```

### Restore Database

```cmd
docker exec -i iot-postgres psql -U postgres postgres < backup.sql
```

---

## 🛠️ Troubleshooting

### Services Not Starting

**Problem:** Containers fail to start

**Solution:**
```cmd
# Check logs
docker-compose logs

# Remove old containers and try again
docker-compose down -v
docker-compose up -d --build
```

### Port Already in Use

**Problem:** Port 8080, 8081, or 5432 already in use

**Solution:**
```cmd
# Find process using port (PowerShell)
Get-NetTCPConnection -LocalPort 8080

# Kill the process or change port in docker-compose.yml
```

### Health Checks Failing

**Problem:** Health checks show as unhealthy

**Solution:**
```cmd
# Wait longer (services need 30-60 seconds to fully start)
timeout /t 30

# Check service logs
docker-compose logs provider
docker-compose logs subscriber

# Verify database is ready
docker exec iot-postgres pg_isready -U postgres
```

### Out of Memory

**Problem:** Containers crash with OOM errors

**Solution:**
```cmd
# Increase Docker Desktop memory allocation
# Settings → Resources → Memory → Set to at least 4GB

# Or reduce JVM memory in docker-compose.yml:
JAVA_OPTS: "-Xms128m -Xmx256m"
```

### Network Issues

**Problem:** Services can't communicate

**Solution:**
```cmd
# Recreate network
docker-compose down
docker network rm iot-network
docker-compose up -d
```

---

## 🔄 Common Operations

### Update Service Code

1. Make code changes
2. Rebuild and restart:
   ```cmd
   docker-compose up -d --build provider
   # or
   docker-compose up -d --build subscriber
   ```

### View Environment Variables

```cmd
docker exec iot-provider env
docker exec iot-subscriber env
```

### Scale Services (for testing)

```cmd
# Not recommended for this setup, but possible:
docker-compose up -d --scale subscriber=2
```

### Clean Everything

```cmd
# Stop and remove everything
docker-compose down -v

# Remove images
docker rmi iot-provider:latest iot-subscriber:latest

# Clean dangling images
docker image prune -f

# Or use deploy.bat option 9
```

---

## 📈 Performance Tuning

### JVM Memory Settings

Edit `docker-compose.yml`:

```yaml
environment:
  JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
```

### Database Connection Pool

Edit `docker-compose.yml`:

```yaml
environment:
  DB_POOL_SIZE: 10  # Increase for higher load
```

### Docker Resource Limits

Add resource limits to `docker-compose.yml`:

```yaml
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 1G
    reservations:
      cpus: '0.5'
      memory: 512M
```

---

## 🔒 Security Considerations

### For Production Deployment

1. **Change Default Passwords**
   - Update PostgreSQL password in `docker-compose.yml`
   - Regenerate API keys in `auth-config.json`

2. **Enable HTTPS/TLS**
   - Add reverse proxy (nginx/traefik)
   - Configure SSL certificates
   - Update Dockerfile for HTTPS support

3. **Use Secrets Management**
   - Docker secrets or environment files
   - Never commit passwords to git

4. **Network Isolation**
   - Use internal networks
   - Expose only necessary ports

5. **Regular Updates**
   - Update base images regularly
   - Monitor for security vulnerabilities

---

## 📝 Environment Variables

### Provider Service

| Variable | Default | Description |
|----------|---------|-------------|
| JAVA_OPTS | -Xms256m -Xmx512m | JVM memory settings |
| DB_HOST | postgres | Database hostname |
| DB_PORT | 5432 | Database port |
| DB_NAME | postgres | Database name |
| DB_USER | postgres | Database username |
| DB_PASSWORD | 123 | Database password |
| DB_POOL_SIZE | 5 | Connection pool size |

### Subscriber Service

| Variable | Default | Description |
|----------|---------|-------------|
| JAVA_OPTS | -Xms256m -Xmx512m | JVM memory settings |
| PROVIDER_HOST | provider | Provider hostname |
| PROVIDER_PORT | 8080 | Provider port |

---

## 🎯 Next Steps

After successful deployment:

1. ✅ **Run Tests**: Execute `test-services.bat` to verify all functionality
2. ✅ **Test APIs**: Use Postman collection or curl commands
3. ✅ **Monitor Logs**: Check logs for any errors or warnings
4. ✅ **Set Up Monitoring**: Consider adding Prometheus/Grafana
5. ✅ **Enable HTTPS**: Configure SSL for production
6. ✅ **Implement Backups**: Set up automated database backups
7. ✅ **Load Testing**: Use JMeter or similar tools
8. ✅ **Documentation**: Keep this guide updated with changes

---

## 📞 Support

For issues or questions:
- Check logs: `docker-compose logs`
- Review troubleshooting section above
- Refer to main README files in each service directory

---

## 📄 Additional Resources

- **Provider README**: `provider/README.md`
- **Subscriber README**: `subscriber/README.md`
- **Production Quality Report**: `provider/PRODUCTION_QUALITY_ANALYSIS_REPORT.md`
- **Docker Compose Documentation**: https://docs.docker.com/compose/
- **Vert.x Documentation**: https://vertx.io/docs/

---

**Last Updated**: March 9, 2026  
**Version**: 1.0  
**Docker Compose Version**: 3.8
