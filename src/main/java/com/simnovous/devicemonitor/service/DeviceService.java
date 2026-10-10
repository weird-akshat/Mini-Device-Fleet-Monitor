package com.simnovous.devicemonitor.service;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simnovous.devicemonitor.dto.DeviceResponse;
import com.simnovous.devicemonitor.dto.HeartbeatRequest;
import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
import com.simnovous.devicemonitor.exception.DeviceAlreadyExistsException;
import com.simnovous.devicemonitor.exception.InvalidDeviceRequestException;
import com.simnovous.devicemonitor.exception.DeviceNotFoundException;
import com.simnovous.devicemonitor.mapper.DeviceMapper;
import com.simnovous.devicemonitor.model.DeviceReportedStatus;
import com.simnovous.devicemonitor.repository.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    @Transactional
    public Device registerDevice(RegisterDeviceRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new InvalidDeviceRequestException("Device ID is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new InvalidDeviceRequestException("Device name is required");
        }
        if (deviceRepository.existsById(request.getId())) {
            throw new DeviceAlreadyExistsException(
                    "Device already exists with ID: " + request.getId());
        }
        Device device = deviceMapper.toEntity(request);
        return deviceRepository.save(device);
    }

    @Transactional
    public DeviceResponse receiveHeartbeat(
            String deviceId,
            HeartbeatRequest request) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> {
                    log.warn("Heartbeat received for unknown deviceId={}", deviceId);
                    return new DeviceNotFoundException(
                            "Device not found: " + deviceId);
                });

        if (request.getStatus() != DeviceReportedStatus.OK) {
            log.warn(
                    "Ignoring heartbeat for deviceId={} due to status={}",
                    deviceId,
                    request.getStatus());

            return deviceMapper.toResponse(device);
        }

        device.setLastHeartbeat(Instant.now());

        if (request.getCpuUsage() != null) {
            device.setCpuUsage(request.getCpuUsage());
        }

        if (request.getSignalStrength() != null) {
            device.setSignalStrength(request.getSignalStrength());
        }

        Device savedDevice = deviceRepository.save(device);

        log.info("Heartbeat accepted for deviceId={}", deviceId);

        return deviceMapper.toResponse(savedDevice);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();

        log.debug("Retrieved {} devices", devices.size());

        return devices.stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> {
                    log.warn("Device not found: deviceId={}", deviceId);
                    return new DeviceNotFoundException(
                            "Device not found: " + deviceId);
                });

        log.debug("Retrieved deviceId={}", deviceId);

        return deviceMapper.toResponse(device);
    }

    public int getResponse() {
        return 0;
    }
}