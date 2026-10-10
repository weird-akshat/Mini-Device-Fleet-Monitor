package com.simnovous.devicemonitor.mapper;

import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
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
}