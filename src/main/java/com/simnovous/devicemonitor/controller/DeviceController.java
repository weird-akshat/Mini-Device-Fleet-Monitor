
package com.simnovous.devicemonitor.controller;

import com.simnovous.devicemonitor.dto.ApiResponse;
import com.simnovous.devicemonitor.dto.DeviceResponse;
import com.simnovous.devicemonitor.dto.FleetSummaryResponse;
import com.simnovous.devicemonitor.dto.HeartbeatRequest;
import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
import com.simnovous.devicemonitor.service.DeviceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public Device registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request) {

        log.info("Registering device with id={}", request.getId());

        Device device = deviceService.registerDevice(request);

        log.info("Device registered successfully with id={}", device.getId());

        return device;
    }

    @GetMapping("/devices/test")
    public int getResponse() {
        log.debug("Received request for /devices/test");
        return deviceService.getResponse();
    }

    @PostMapping("/devices/{id}/heartbeat")
    public ApiResponse<DeviceResponse> receiveHeartbeat(
            @PathVariable String id,
            @Valid @RequestBody HeartbeatRequest request) {

        log.debug("Received heartbeat request for deviceId={}", id);

        DeviceResponse response = deviceService.receiveHeartbeat(id, request);

        return new ApiResponse<>(
                true,
                "Heartbeat processed",
                response);
    }

    @GetMapping({ "/devices", "/devices/{id}" })
    public ApiResponse<?> getDevices(
            @PathVariable(required = false) String id) {

        if (id != null) {
            DeviceResponse device = deviceService.getDeviceById(id);

            return new ApiResponse<>(
                    true,
                    "Device retrieved successfully",
                    device);
        }

        List<DeviceResponse> devices = deviceService.getAllDevices();

        return new ApiResponse<>(
                true,
                "Devices retrieved successfully",
                devices);
    }

    @GetMapping("/summary")
    public ApiResponse<FleetSummaryResponse> getFleetSummary() {
        log.debug("Fetching fleet summary");

        FleetSummaryResponse summary = deviceService.getFleetSummary();

        return new ApiResponse<>(
                true,
                "Fleet summary retrieved successfully",
                summary);
    }
}
