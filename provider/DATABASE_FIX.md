# Database Connection Error Fix

## Problem
```
Database error: com/ongres/scram/common/exception/ScramException
```

This error occurs because PostgreSQL uses SCRAM-SHA-256 authentication by default in recent versions, and the required dependency is missing.

## Solution

I've added the SCRAM client dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.ongres.scram</groupId>
  <artifactId>client</artifactId>
  <version>2.1</version>
</dependency>
```

## Steps to Fix

### 1. Rebuild the Project

Stop the running application and rebuild:

```bash
cd C:\Users\USER\Gokul\provider
mvn clean package
```

### 2. Restart the Application

```bash
mvn exec:java
```

You should now see:
```
Database connection pool initialized
Provider service started on port 8080
Base URL: /provider/api
```

Without the SCRAM error!

### 3. Test the Database Connection

Test creating a device to verify the connection works:

```bash
curl -X POST http://localhost:8080/provider/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceName\":\"Test Sensor\",\"deviceType\":\"sensor\",\"firmwareVersion\":\"1.0.0\",\"location\":\"Test Location\",\"status\":\"active\"}"
```

## Alternative Solutions

If you still encounter issues, you can try:

### Option 1: Update PostgreSQL Authentication Method

Change PostgreSQL to use MD5 authentication instead of SCRAM:

1. Edit `postgresql.conf`:
   ```
   password_encryption = md5
   ```

2. Edit `pg_hba.conf`:
   ```
   # Change from scram-sha-256 to md5
   host    all             all             127.0.0.1/32            md5
   ```

3. Restart PostgreSQL service

4. Update the password:
   ```sql
   ALTER USER postgres WITH PASSWORD '123';
   ```

### Option 2: Verify PostgreSQL is Running

Make sure PostgreSQL service is running:

```bash
# Check PostgreSQL service status (Windows)
sc query postgresql-x64-14

# Or use services.msc to check
```

### Option 3: Check Database Configuration

Verify your database settings in `src/main/resources/db-config.json`:

```json
{
  "database": {
    "host": "localhost",
    "port": 5432,
    "database": "postgres",
    "user": "postgres",
    "password": "123",
    "pool_size": 5
  }
}
```

## Verification Checklist

- [ ] PostgreSQL service is running
- [ ] Database 'postgres' exists
- [ ] User 'postgres' with password '123' can connect
- [ ] Table 'devices' is created
- [ ] SCRAM dependency is added to pom.xml
- [ ] Project is rebuilt with `mvn clean package`
- [ ] Application starts without errors

## Testing After Fix

Once the application starts successfully:

1. **Test Health Check:**
   ```bash
   curl http://localhost:8080/health
   ```

2. **Test Device Creation:**
   ```bash
   curl -X POST http://localhost:8080/provider/api/devices ^
     -H "Content-Type: application/json" ^
     -d "{\"deviceName\":\"Temperature Sensor\",\"deviceType\":\"sensor\"}"
   ```

3. **Verify in Database:**
   ```sql
   SELECT * FROM devices;
   ```

## Need More Help?

If you continue to see errors, check:

1. PostgreSQL logs for connection errors
2. Make sure the devices table is created (see README.md for schema)
3. Verify network connectivity to localhost:5432
4. Check firewall settings
