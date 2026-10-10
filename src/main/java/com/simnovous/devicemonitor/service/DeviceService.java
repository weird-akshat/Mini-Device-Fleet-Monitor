package com.simnovous.devicemonitor.service;

import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
import com.simnovous.devicemonitor.exception.DeviceAlreadyExistsException;
import com.simnovous.devicemonitor.exception.InvalidDeviceRequestException;
import com.simnovous.devicemonitor.mapper.DeviceMapper;
import com.simnovous.devicemonitor.repository.DeviceRepository;

import lombok.RequiredArgsConstructor;

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

    public int getResponse() {
        return 0;
    }
}