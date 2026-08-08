package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.userservice.dto.request.ApiKeyValidationRequest;
import dev.sahilbasumatary.userservice.dto.response.ApiKeyValidationResponse;
import dev.sahilbasumatary.userservice.service.ApiKeyValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api-keys")
public class InternalApiKeyController {

    private final ApiKeyValidationService apiKeyValidationService;

    public InternalApiKeyController(ApiKeyValidationService apiKeyValidationService) {
        this.apiKeyValidationService = apiKeyValidationService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiKeyValidationResponse> validate(
            @Valid @RequestBody ApiKeyValidationRequest request) {
        return apiKeyValidationService
                .validate(request.apiKey())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
