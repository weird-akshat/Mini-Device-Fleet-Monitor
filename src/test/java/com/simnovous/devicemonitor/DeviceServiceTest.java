package com.simnovous.devicemonitor;

import com.simnovous.devicemonitor.dto.DeviceResponse;
import com.simnovous.devicemonitor.dto.FleetSummaryResponse;
import com.simnovous.devicemonitor.dto.HeartbeatRequest;
import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
import com.simnovous.devicemonitor.exception.DeviceAlreadyExistsException;
import com.simnovous.devicemonitor.exception.DeviceNotFoundException;
import com.simnovous.devicemonitor.exception.InvalidDeviceRequestException;
import com.simnovous.devicemonitor.mapper.DeviceMapper;
import com.simnovous.devicemonitor.model.DeviceReportedStatus;
import com.simnovous.devicemonitor.model.DeviceStatus;
import com.simnovous.devicemonitor.repository.DeviceRepository;
import com.simnovous.devicemonitor.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void registerDevice_rejectsBlankId() {
        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setId("   ");
        request.setName("Living Room Sensor");

        assertThrows(InvalidDeviceRequestException.class, () -> deviceService.registerDevice(request));
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void registerDevice_rejectsDuplicateDevice() {
        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setId("device-001");
        request.setName("Living Room Sensor");

        when(deviceRepository.existsById("device-001")).thenReturn(true);

        assertThrows(DeviceAlreadyExistsException.class, () -> deviceService.registerDevice(request));
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void receiveHeartbeat_ignoresNonOkStatusAndDoesNotPersist() {
        Device device = new Device("device-001", "Living Room Sensor", Instant.now());
        device.setLastHeartbeat(Instant.now().minusSeconds(2));
        device.setCpuUsage(10.0);
        device.setSignalStrength(30);

        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceReportedStatus.WARNING);
        request.setCpuUsage(90.0);
        request.setSignalStrength(95);

        DeviceResponse existingResponse = new DeviceResponse(
                "device-001",
                "Living Room Sensor",
                DeviceStatus.ONLINE,
                Instant.now(),
                device.getLastHeartbeat(),
                10.0,
                30
        );

        when(deviceRepository.findById("device-001")).thenReturn(Optional.of(device));
        when(deviceMapper.toResponse(device)).thenReturn(existingResponse);

        DeviceResponse response = deviceService.receiveHeartbeat("device-001", request);

        assertThat(response).isSameAs(existingResponse);
        assertThat(device.getCpuUsage()).isEqualTo(10.0);
        assertThat(device.getSignalStrength()).isEqualTo(30);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void receiveHeartbeat_throwsWhenDeviceMissing() {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceReportedStatus.OK);

        when(deviceRepository.findById("missing-device")).thenReturn(Optional.empty());

        DeviceNotFoundException exception = assertThrows(
                DeviceNotFoundException.class,
                () -> deviceService.receiveHeartbeat("missing-device", request)
        );

        assertThat(exception.getMessage()).isEqualTo("Device not found: missing-device");
    }

    @Test
    void getFleetSummary_countsOnlineAndOfflineDevices() {
        Instant now = Instant.now();
        Device onlineDevice = new Device("device-001", "Online", now.minusSeconds(10));
        onlineDevice.setLastHeartbeat(now.minusSeconds(10));

        Device offlineDevice = new Device("device-002", "Offline", now.minusSeconds(100));
        offlineDevice.setLastHeartbeat(now.minusSeconds(100));

        Device noHeartbeatDevice = new Device("device-003", "No Heartbeat", now.minusSeconds(5));

        when(deviceRepository.findAll()).thenReturn(List.of(onlineDevice, offlineDevice, noHeartbeatDevice));

        FleetSummaryResponse summary = deviceService.getFleetSummary();

        assertThat(summary.getTotalDevices()).isEqualTo(3L);
        assertThat(summary.getOnlineDevices()).isEqualTo(1L);
        assertThat(summary.getOfflineDevices()).isEqualTo(2L);
    }
}
