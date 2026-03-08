# Authentication Quick Reference

## 🔑 API Keys

### Provider Service API Keys
```
pk_live_12345abcdef67890provider       - Primary Provider Key
pk_live_subscriber987654321xyz         - Subscriber Service Key
pk_live_admin_secret_key_2026          - Admin Client Key
```

### Subscriber Service API Keys
```
sk_live_client_app_key_12345           - Client App Key
sk_live_mobile_app_key_67890           - Mobile App Key
sk_live_dashboard_key_abcdef           - Dashboard Key
```

---

## 📡 Usage Example

```bash
# Provider
curl -H "X-API-Key: pk_live_12345abcdef67890provider" \
     http://localhost:8080/provider/api/devices

# Subscriber
curl -H "X-API-Key: sk_live_client_app_key_12345" \
     http://localhost:8081/subscriber/api/devices
```

---

## 📖 Full Documentation

See [AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md)
