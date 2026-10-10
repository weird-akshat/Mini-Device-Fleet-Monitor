package com.simnovous.devicemonitor.mapper;

import com.simnovous.devicemonitor.dto.DeviceResponse;
import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
import com.simnovous.devicemonitor.model.DeviceStatus;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeviceMapper {

    public Device toEntity(RegisterDeviceRequest request) {
        return new Device(
                request.getId(),
                request.getName(),
                Instant.now());
    }

    public DeviceResponse toResponse(Device device) {
        Instant now = Instant.now();

        DeviceStatus status = DeviceStatus.OFFLINE;

        if (device.getLastHeartbeat() != null
                && device.getLastHeartbeat().isAfter(now.minusSeconds(30))) {
            status = DeviceStatus.ONLINE;
        }

        return new DeviceResponse(
                device.getId(),
                device.getName(),
                status,
                device.getCreatedAt(),
                device.getLastHeartbeat(),
                device.getCpuUsage(),
                device.getSignalStrength());
    }
}