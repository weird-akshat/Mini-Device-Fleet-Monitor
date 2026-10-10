package com.simnovous.devicemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

import com.simnovous.devicemonitor.model.DeviceStatus;

@Getter
@AllArgsConstructor
public class DeviceResponse {

    private String id;
    private String name;
    private DeviceStatus status;
    private Instant createdAt;
    private Instant lastHeartbeat;
    private Double cpuUsage;
    private Integer signalStrength;
}