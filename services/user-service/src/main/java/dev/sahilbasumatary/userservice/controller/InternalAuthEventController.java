package dev.sahilbasumatary.userservice.controller;

import dev.sahilbasumatary.common.event.OrganizationEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.userservice.service.AuthProjectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth-events")
public class InternalAuthEventController {

    private final AuthProjectionService authProjectionService;

    public InternalAuthEventController(AuthProjectionService authProjectionService) {
        this.authProjectionService = authProjectionService;
    }

    @PostMapping("/users")
    public ResponseEntity<Void> applyUser(@RequestBody UserEvent event) {
        authProjectionService.applyUser(event);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/organizations")
    public ResponseEntity<Void> applyOrganization(@RequestBody OrganizationEvent event) {
        authProjectionService.applyOrganization(event);
        return ResponseEntity.noContent().build();
    }
}
