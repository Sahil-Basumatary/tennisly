package dev.sahilbasumatary.notificationservice.service;

import dev.sahilbasumatary.notificationservice.context.RequestContext;
import dev.sahilbasumatary.notificationservice.dto.request.RegisterDeviceTokenRequest;
import dev.sahilbasumatary.notificationservice.dto.response.DeviceTokenResponse;
import dev.sahilbasumatary.notificationservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.notificationservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.notificationservice.push.DevicePushToken;
import dev.sahilbasumatary.notificationservice.push.DevicePushTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);

    private final DevicePushTokenRepository tokenRepository;

    public DeviceTokenService(DevicePushTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> listMine() {
        String clerkId = requireUser();
        return tokenRepository.findByClerkId(clerkId).stream().map(DeviceTokenResponse::from).toList();
    }

    @Transactional
    public DeviceTokenResponse register(RegisterDeviceTokenRequest request) {
        String clerkId = requireUser();
        String tokenValue = request.token().trim();
        DevicePushToken token =
                tokenRepository
                        .findByToken(tokenValue)
                        .orElseGet(DevicePushToken::new);
        token.setClerkId(clerkId);
        token.setToken(tokenValue);
        token.setPlatform(request.platform());
        token.setActive(true);
        token.setLastSeenAt(Instant.now());
        DevicePushToken saved = tokenRepository.save(token);
        log.info(
                "Registered push token id={} clerkId={} platform={}",
                saved.getId(),
                clerkId,
                saved.getPlatform());
        return DeviceTokenResponse.from(saved);
    }

    @Transactional
    public void deactivate(UUID id) {
        String clerkId = requireUser();
        DevicePushToken token =
                tokenRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("DevicePushToken", id));
        if (!token.getClerkId().equals(clerkId)) {
            throw new ResourceNotFoundException("DevicePushToken", id);
        }
        token.setActive(false);
        tokenRepository.save(token);
        log.info("Deactivated push token id={} clerkId={}", id, clerkId);
    }

    private static String requireUser() {
        String clerkId = RequestContext.getUserId();
        if (clerkId == null || clerkId.isBlank()) {
            throw new UnauthorizedAccessException("Authenticated user required");
        }
        return clerkId;
    }
}
