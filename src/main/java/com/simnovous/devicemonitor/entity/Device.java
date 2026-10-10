package com.simnovous.devicemonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
public class Device {

    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant lastHeartbeat;
    private Double cpuUsage;
    private Integer signalStrength;

    public Device(String id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }
}