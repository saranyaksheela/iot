# Docker Deployment - Quick Reference
## One-Page Cheat Sheet

---

## 🚀 Quick Start (3 Commands)

```cmd
cd C:\Users\USER\Gokul\provider
deploy.bat
# Select option 10 (Full Deployment)
```

---

## 📋 Essential Commands

### Start Services
```cmd
docker-compose up -d --build          # Build and start
docker-compose up -d                  # Start without building
```

### Stop Services
```cmd
docker-compose down                   # Stop containers
docker-compose down -v                # Stop and remove volumes
```

### View Status
```cmd
docker-compose ps                     # Container status
docker-compose logs -f provider       # View logs
docker stats --no-stream              # Resource usage
```

### Run Tests
```cmd
test-services.bat                     # Full test suite
curl http://localhost:8080/health     # Quick health check
```

---

## 🔗 Service URLs

| Service | URL | API Key Header |
|---------|-----|----------------|
| **Provider Health** | http://localhost:8080/health | - |
| **Provider API** | http://localhost:8080/provider/api | X-API-Key: pk_live_12345abcdef67890provider |
| **Subscriber Health** | http://localhost:8081/health | - |
| **Subscriber API** | http://localhost:8081/subscriber/api | X-API-Key: sk_live_client_app_key_12345 |
| **PostgreSQL** | localhost:5432 | User: postgres, Pass: 123 |

---

## 🧪 Quick API Tests

### Health Check
```cmd
curl http://localhost:8080/health
```

### Get All Devices
```cmd
curl -H "X-API-Key: pk_live_12345abcdef67890provider" ^
     http://localhost:8080/provider/api/devices
```

### Create Device
```cmd
curl -X POST ^
     -H "Content-Type: application/json" ^
     -H "X-API-Key: pk_live_12345abcdef67890provider" ^
     -d "{\"deviceName\":\"Test Sensor\",\"deviceType\":\"temperature\",\"firmwareVersion\":\"1.0.0\"}" ^
     http://localhost:8080/provider/api/devices
```

---

## 🗄️ Database Access

### Connect to PostgreSQL
```cmd
docker exec -it iot-postgres psql -U postgres
```

### Common SQL Queries
```sql
-- View all devices
SELECT * FROM devices;

-- View recent telemetry
SELECT * FROM telemetry_data ORDER BY received_at DESC LIMIT 10;

-- Count by type
SELECT device_type, COUNT(*) FROM devices GROUP BY device_type;

-- Exit
\q
```

---

## 📊 Troubleshooting

### Services Won't Start
```cmd
# Check logs
docker-compose logs

# Clean and restart
docker-compose down -v
docker-compose up -d --build
```

### Port Already in Use
```cmd
# Check what's using port 8080
netstat -ano | findstr :8080

# Or change port in docker-compose.yml
```

### Container Crashes
```cmd
# View recent logs
docker-compose logs --tail=50 provider

# Check container status
docker ps -a

# Restart specific service
docker-compose restart provider
```

---

## 🔄 Common Workflows

### Update Code & Redeploy
```cmd
# Make your code changes, then:
docker-compose up -d --build provider
```

### View Live Logs
```cmd
docker-compose logs -f --tail=100 provider
```

### Backup Database
```cmd
docker exec iot-postgres pg_dump -U postgres postgres > backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%.sql
```

### Clean Everything
```cmd
docker-compose down -v
docker rmi iot-provider:latest iot-subscriber:latest
docker image prune -f
```

---

## 📁 Important Files

| File | Purpose |
|------|---------|
| `Dockerfile` | Container image definition |
| `docker-compose.yml` | Multi-container orchestration |
| `init-db.sql` | Database initialization |
| `deploy.bat` | Interactive deployment menu |
| `test-services.bat` | Automated test suite |
| `.dockerignore` | Build exclusions |

---

## 🎯 Deploy.bat Menu (Quick Ref)

| Option | Action |
|--------|--------|
| 1 | Build images only |
| 2 | Build & start services |
| 3 | Start services (no build) |
| 4 | Stop services |
| 7 | Run test suite |
| 8 | Health check |
| 9 | Clean up everything |
| 10 | **Full deployment (recommended)** |

---

## 🔒 API Keys

### Provider
- `pk_live_12345abcdef67890provider` (main)
- `pk_live_subscriber987654321xyz` (subscriber access)

### Subscriber
- `sk_live_client_app_key_12345` (client app)
- `sk_live_mobile_app_key_67890` (mobile)

---

## ⚡ Pro Tips

1. **Always use `deploy.bat` for management** - It handles everything safely
2. **Check logs first** when troubleshooting - Most issues are visible there
3. **Wait 30-60 seconds** after starting services before testing
4. **Use option 10** in deploy.bat for first-time deployment
5. **Run `test-services.bat`** after any changes to verify

---

## 📞 Need Help?

```cmd
# Check deployment guide
type DOCKER_DEPLOYMENT.md | more

# View deploy.bat menu
deploy.bat

# Run health check
curl http://localhost:8080/health
curl http://localhost:8081/health

# Check container logs
docker-compose logs provider
```

---

**For detailed documentation, see**: `DOCKER_DEPLOYMENT.md`
