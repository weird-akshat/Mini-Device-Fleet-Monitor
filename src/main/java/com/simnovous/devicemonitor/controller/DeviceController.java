package com.simnovous.devicemonitor.controller;

import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
import com.simnovous.devicemonitor.service.DeviceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Device registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request) {
        return deviceService.registerDevice(request);
    }

    @GetMapping("/test")
    public int getResponse() {
        return deviceService.getResponse();
    }
}