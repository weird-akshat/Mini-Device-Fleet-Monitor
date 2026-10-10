
package com.simnovous.devicemonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simnovous.devicemonitor.controller.DeviceController;
import com.simnovous.devicemonitor.dto.DeviceResponse;
import com.simnovous.devicemonitor.dto.FleetSummaryResponse;
import com.simnovous.devicemonitor.dto.HeartbeatRequest;
import com.simnovous.devicemonitor.dto.RegisterDeviceRequest;
import com.simnovous.devicemonitor.entity.Device;
import com.simnovous.devicemonitor.exception.DeviceNotFoundException;
import com.simnovous.devicemonitor.exception.GlobalExceptionHandler;
import com.simnovous.devicemonitor.model.DeviceReportedStatus;
import com.simnovous.devicemonitor.model.DeviceStatus;
import com.simnovous.devicemonitor.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeviceControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DeviceController(deviceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerDevice_returnsCreatedDevice() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setId("device-001");
        request.setName("Living Room Sensor");

        Device device = new Device(
                "device-001",
                "Living Room Sensor",
                Instant.now());

        when(deviceService.registerDevice(any(RegisterDeviceRequest.class)))
                .thenReturn(device);

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("device-001"))
                .andExpect(jsonPath("$.name").value("Living Room Sensor"));
    }

    @Test
    void registerDevice_missingId_returnsBadRequest() throws Exception {
        String body = "{\"name\":\"Living Room Sensor\"}";

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void registerDevice_duplicateId_returnsConflict() throws Exception {
        RegisterDeviceRequest request = new RegisterDeviceRequest();
        request.setId("device-001");
        request.setName("Living Room Sensor");

        when(deviceService.registerDevice(any(RegisterDeviceRequest.class)))
                .thenThrow(new com.simnovous.devicemonitor.exception.DeviceAlreadyExistsException(
                        "Device already exists with ID: device-001"));

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Device already exists with ID: device-001"));
    }

    @Test
    void receiveHeartbeat_returnsProcessedResponse() throws Exception {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceReportedStatus.OK);
        request.setCpuUsage(22.5);
        request.setSignalStrength(88);

        Instant now = Instant.now();

        DeviceResponse response = new DeviceResponse(
                "device-001",
                "Living Room Sensor",
                DeviceStatus.ONLINE,
                now,
                now,
                22.5,
                88);

        when(deviceService.receiveHeartbeat(eq("device-001"), any(HeartbeatRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/devices/device-001/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Heartbeat processed"))
                .andExpect(jsonPath("$.data.id").value("device-001"));
    }

    @Test
    void receiveHeartbeat_unknownDevice_returnsNotFound() throws Exception {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceReportedStatus.OK);

        when(deviceService.receiveHeartbeat(eq("missing-device"), any(HeartbeatRequest.class)))
                .thenThrow(new DeviceNotFoundException("Device not found: missing-device"));

        mockMvc.perform(post("/devices/missing-device/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Device not found: missing-device"));
    }

    @Test
    void receiveHeartbeat_invalidCpuUsage_returnsBadRequest() throws Exception {
        String body = "{\"status\":\"OK\",\"cpuUsage\":101.0,\"signalStrength\":75}";

        mockMvc.perform(post("/devices/device-001/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    void getAllDevices_returnsList() throws Exception {
        Instant now = Instant.now();

        DeviceResponse device = new DeviceResponse(
                "device-001",
                "Living Room Sensor",
                DeviceStatus.ONLINE,
                now,
                now,
                35.0,
                72);

        when(deviceService.getAllDevices()).thenReturn(List.of(device));

        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Devices retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value("device-001"));
    }

    @Test
    void getAllDevices_emptyList_returnsEmptyArray() throws Exception {
        when(deviceService.getAllDevices()).thenReturn(List.of());

        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getDeviceById_returnsDevice() throws Exception {
        Instant now = Instant.now();

        DeviceResponse response = new DeviceResponse(
                "device-001",
                "Living Room Sensor",
                DeviceStatus.ONLINE,
                now,
                now,
                18.0,
                90);

        when(deviceService.getDeviceById("device-001")).thenReturn(response);

        mockMvc.perform(get("/devices/device-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("device-001"))
                .andExpect(jsonPath("$.data.name").value("Living Room Sensor"));
    }

    @Test
    void getDeviceById_unknownDevice_returnsNotFound() throws Exception {
        when(deviceService.getDeviceById("missing-device"))
                .thenThrow(new DeviceNotFoundException("Device not found: missing-device"));

        mockMvc.perform(get("/devices/missing-device"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Device not found: missing-device"));
    }

    @Test
    void getFleetSummary_returnsSummary() throws Exception {
        FleetSummaryResponse summary = new FleetSummaryResponse(3, 2, 1);

        when(deviceService.getFleetSummary()).thenReturn(summary);

        mockMvc.perform(get("/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fleet summary retrieved successfully"))
                .andExpect(jsonPath("$.data.totalDevices").value(3))
                .andExpect(jsonPath("$.data.onlineDevices").value(2))
                .andExpect(jsonPath("$.data.offlineDevices").value(1));
    }
}
