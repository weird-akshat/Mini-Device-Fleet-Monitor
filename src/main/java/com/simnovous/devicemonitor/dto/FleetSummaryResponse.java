package com.simnovous.devicemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FleetSummaryResponse {

    private long totalDevices;
    private long onlineDevices;
    private long offlineDevices;
}