package iot.provider.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Device {
  private Long id;
  private UUID deviceUuid;
  private String deviceName;
  private String deviceType;
  private String firmwareVersion;
  private String location;
  private String status;
  private LocalDateTime createdAt;

  public Device() {
  }

  public Device(UUID deviceUuid, String deviceName, String deviceType, 
                String firmwareVersion, String location, String status) {
    this.deviceUuid = deviceUuid;
    this.deviceName = deviceName;
    this.deviceType = deviceType;
    this.firmwareVersion = firmwareVersion;
    this.location = location;
    this.status = status;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UUID getDeviceUuid() {
    return deviceUuid;
  }

  public void setDeviceUuid(UUID deviceUuid) {
    this.deviceUuid = deviceUuid;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  public String getDeviceType() {
    return deviceType;
  }

  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }

  public String getFirmwareVersion() {
    return firmwareVersion;
  }

  public void setFirmwareVersion(String firmwareVersion) {
    this.firmwareVersion = firmwareVersion;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "Device{" +
      "id=" + id +
      ", deviceUuid=" + deviceUuid +
      ", deviceName='" + deviceName + '\'' +
      ", deviceType='" + deviceType + '\'' +
      ", firmwareVersion='" + firmwareVersion + '\'' +
      ", location='" + location + '\'' +
      ", status='" + status + '\'' +
      ", createdAt=" + createdAt +
      '}';
  }
}
