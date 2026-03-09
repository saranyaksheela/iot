package iot.provider.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Device model.
 */
@DisplayName("Device Model Tests")
public class DeviceTest {

    @Test
    @DisplayName("Should create device with default constructor")
    void testDefaultConstructor() {
        Device device = new Device();
        
        assertNull(device.getId());
        assertNull(device.getDeviceUuid());
        assertNull(device.getDeviceName());
        assertNull(device.getDeviceType());
        assertNull(device.getFirmwareVersion());
        assertNull(device.getLocation());
        assertNull(device.getStatus());
        assertNull(device.getCreatedAt());
    }

    @Test
    @DisplayName("Should create device with parameterized constructor")
    void testParameterizedConstructor() {
        UUID uuid = UUID.randomUUID();
        String deviceName = "Test Sensor";
        String deviceType = "temperature-sensor";
        String firmwareVersion = "1.2.3";
        String location = "Lab A";
        String status = "active";

        Device device = new Device(uuid, deviceName, deviceType, firmwareVersion, location, status);

        assertNull(device.getId()); // Not set by constructor
        assertEquals(uuid, device.getDeviceUuid());
        assertEquals(deviceName, device.getDeviceName());
        assertEquals(deviceType, device.getDeviceType());
        assertEquals(firmwareVersion, device.getFirmwareVersion());
        assertEquals(location, device.getLocation());
        assertEquals(status, device.getStatus());
        assertNull(device.getCreatedAt()); // Not set by constructor
    }

    @Test
    @DisplayName("Should set and get all properties correctly")
    void testGettersAndSetters() {
        Device device = new Device();
        
        Long id = 123L;
        UUID uuid = UUID.randomUUID();
        String deviceName = "Updated Sensor";
        String deviceType = "humidity-sensor";
        String firmwareVersion = "2.0.0";
        String location = "Lab B";
        String status = "inactive";
        LocalDateTime createdAt = LocalDateTime.now();

        // Set all properties
        device.setId(id);
        device.setDeviceUuid(uuid);
        device.setDeviceName(deviceName);
        device.setDeviceType(deviceType);
        device.setFirmwareVersion(firmwareVersion);
        device.setLocation(location);
        device.setStatus(status);
        device.setCreatedAt(createdAt);

        // Verify all properties
        assertEquals(id, device.getId());
        assertEquals(uuid, device.getDeviceUuid());
        assertEquals(deviceName, device.getDeviceName());
        assertEquals(deviceType, device.getDeviceType());
        assertEquals(firmwareVersion, device.getFirmwareVersion());
        assertEquals(location, device.getLocation());
        assertEquals(status, device.getStatus());
        assertEquals(createdAt, device.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle null values in setters")
    void testNullValues() {
        Device device = new Device();

        // Set null values
        device.setId(null);
        device.setDeviceUuid(null);
        device.setDeviceName(null);
        device.setDeviceType(null);
        device.setFirmwareVersion(null);
        device.setLocation(null);
        device.setStatus(null);
        device.setCreatedAt(null);

        // Verify null values
        assertNull(device.getId());
        assertNull(device.getDeviceUuid());
        assertNull(device.getDeviceName());
        assertNull(device.getDeviceType());
        assertNull(device.getFirmwareVersion());
        assertNull(device.getLocation());
        assertNull(device.getStatus());
        assertNull(device.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testEmptyStrings() {
        Device device = new Device();

        device.setDeviceName("");
        device.setDeviceType("");
        device.setFirmwareVersion("");
        device.setLocation("");
        device.setStatus("");

        assertEquals("", device.getDeviceName());
        assertEquals("", device.getDeviceType());
        assertEquals("", device.getFirmwareVersion());
        assertEquals("", device.getLocation());
        assertEquals("", device.getStatus());
    }

    @Test
    @DisplayName("Should handle long strings")
    void testLongStrings() {
        Device device = new Device();
        
        String longString = "a".repeat(1000);
        
        device.setDeviceName(longString);
        device.setDeviceType(longString);
        device.setFirmwareVersion(longString);
        device.setLocation(longString);
        device.setStatus(longString);

        assertEquals(longString, device.getDeviceName());
        assertEquals(longString, device.getDeviceType());
        assertEquals(longString, device.getFirmwareVersion());
        assertEquals(longString, device.getLocation());
        assertEquals(longString, device.getStatus());
    }

    @Test
    @DisplayName("Should maintain object immutability for UUID")
    void testUuidImmutability() {
        UUID originalUuid = UUID.randomUUID();
        Device device = new Device();
        device.setDeviceUuid(originalUuid);

        UUID retrievedUuid = device.getDeviceUuid();
        
        // UUID objects are immutable, so this should be the same reference
        assertSame(originalUuid, retrievedUuid);
    }

    @Test
    @DisplayName("Should handle different UUID values")
    void testDifferentUuids() {
        Device device1 = new Device();
        Device device2 = new Device();
        
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        device1.setDeviceUuid(uuid1);
        device2.setDeviceUuid(uuid2);
        
        assertNotEquals(device1.getDeviceUuid(), device2.getDeviceUuid());
    }

    @Test
    @DisplayName("Should handle LocalDateTime correctly")
    void testLocalDateTime() {
        Device device = new Device();
        LocalDateTime now = LocalDateTime.now();
        
        device.setCreatedAt(now);
        
        assertEquals(now, device.getCreatedAt());
        
        // Test with past date
        LocalDateTime pastDate = LocalDateTime.of(2020, 1, 1, 12, 0, 0);
        device.setCreatedAt(pastDate);
        
        assertEquals(pastDate, device.getCreatedAt());
    }

    @Test
    @DisplayName("Should create multiple independent device instances")
    void testMultipleInstances() {
        Device device1 = new Device();
        Device device2 = new Device();
        
        device1.setId(1L);
        device1.setDeviceName("Device 1");
        
        device2.setId(2L);
        device2.setDeviceName("Device 2");
        
        // Verify independence
        assertEquals(1L, device1.getId());
        assertEquals("Device 1", device1.getDeviceName());
        
        assertEquals(2L, device2.getId());
        assertEquals("Device 2", device2.getDeviceName());
        
        // Changing one should not affect the other
        device1.setDeviceName("Updated Device 1");
        assertEquals("Updated Device 1", device1.getDeviceName());
        assertEquals("Device 2", device2.getDeviceName());
    }
}