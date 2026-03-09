-- Database initialization script for IoT Provider Service
-- Creates necessary tables and indexes

-- Create devices table
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    device_uuid UUID UNIQUE NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    firmware_version VARCHAR(50),
    location VARCHAR(100),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create telemetry_data table
CREATE TABLE IF NOT EXISTS telemetry_data (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    payload JSONB,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_devices_uuid ON devices(device_uuid);
CREATE INDEX IF NOT EXISTS idx_devices_type ON devices(device_type);
CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status);
CREATE INDEX IF NOT EXISTS idx_telemetry_device_id ON telemetry_data(device_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_received_at ON telemetry_data(received_at DESC);
CREATE INDEX IF NOT EXISTS idx_telemetry_payload ON telemetry_data USING GIN (payload);

-- Insert sample data for testing
INSERT INTO devices (device_uuid, device_name, device_type, firmware_version, location, status)
VALUES 
    (gen_random_uuid(), 'Temperature Sensor 1', 'temperature', '1.0.0', 'Building A - Floor 1', 'active'),
    (gen_random_uuid(), 'Humidity Sensor 1', 'humidity', '1.0.0', 'Building A - Floor 2', 'active'),
    (gen_random_uuid(), 'Motion Detector 1', 'motion', '2.0.0', 'Building B - Entrance', 'active')
ON CONFLICT (device_uuid) DO NOTHING;

-- Insert sample telemetry data
INSERT INTO telemetry_data (device_id, topic_id, payload, received_at)
SELECT 
    d.id,
    1,
    jsonb_build_object(
        'temperature', (random() * 30 + 15)::numeric(5,2),
        'unit', 'celsius',
        'timestamp', NOW()
    ),
    NOW() - (random() * interval '24 hours')
FROM devices d
WHERE d.device_type = 'temperature'
LIMIT 5;

-- Grant permissions (if needed)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

-- Display table information
\echo 'Database initialized successfully!'
\echo 'Tables created:'
\dt

\echo 'Sample data inserted:'
SELECT COUNT(*) as device_count FROM devices;
SELECT COUNT(*) as telemetry_count FROM telemetry_data;
