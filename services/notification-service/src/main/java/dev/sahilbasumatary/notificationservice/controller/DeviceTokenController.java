package dev.sahilbasumatary.notificationservice.controller;

import dev.sahilbasumatary.notificationservice.dto.request.RegisterDeviceTokenRequest;
import dev.sahilbasumatary.notificationservice.dto.response.DeviceTokenResponse;
import dev.sahilbasumatary.notificationservice.service.DeviceTokenService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/me/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceTokenResponse>> list() {
        return ResponseEntity.ok(deviceTokenService.listMine());
    }

    @PostMapping
    public ResponseEntity<DeviceTokenResponse> register(
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        return ResponseEntity.ok(deviceTokenService.register(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deviceTokenService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
