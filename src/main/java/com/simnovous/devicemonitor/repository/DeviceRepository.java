package com.simnovous.devicemonitor.repository;

import com.simnovous.devicemonitor.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String> {
}