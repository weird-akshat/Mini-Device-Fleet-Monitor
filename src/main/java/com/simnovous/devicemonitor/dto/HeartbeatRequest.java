package com.simnovous.devicemonitor.dto;

import com.simnovous.devicemonitor.model.DeviceReportedStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class HeartbeatRequest {

    private Instant timestamp;

    private DeviceReportedStatus status;

    @Min(value = 0, message = "CPU usage must be at least 0")
    @Max(value = 100, message = "CPU usage must not exceed 100")
    private Double cpuUsage;

    @Min(value = 0, message = "Signal strength must be at least 0")
    @Max(value = 100, message = "Signal strength must not exceed 100")
    private Integer signalStrength;
}