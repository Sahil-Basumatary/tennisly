package dev.sahilbasumatary.matchservice.contract;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.sahilbasumatary.matchservice.controller.MatchController;
import dev.sahilbasumatary.matchservice.dto.response.MatchPlayerResponse;
import dev.sahilbasumatary.matchservice.dto.response.MatchResponse;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import dev.sahilbasumatary.matchservice.entity.Surface;
import dev.sahilbasumatary.matchservice.service.MatchService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Provider("match-service")
@PactFolder("pacts")
class MatchControllerProviderPactTest {

    private static final UUID MATCH_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final MatchService matchService = mock(MatchService.class);

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        if (context == null) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter(mapper);
        MockMvcTestTarget target = new MockMvcTestTarget();
        target.setControllers(new MatchController(matchService));
        target.setMessageConverters(json);
        context.setTarget(target);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("matches exist")
    void matchesExist() {
        when(matchService.listMatches(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleMatch()));
    }

    @State("match exists")
    void matchExists() {
        when(matchService.getMatch(eq(MATCH_ID))).thenReturn(sampleMatch());
    }

    private static MatchResponse sampleMatch() {
        Instant created = Instant.parse("2024-01-01T12:00:00Z");
        return new MatchResponse(
                MATCH_ID,
                "atp-finals-m1",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                Surface.HARD,
                MatchStatus.SCHEDULED,
                3,
                Instant.parse("2024-06-01T12:00:00Z"),
                Instant.parse("2024-06-01T12:05:00Z"),
                Instant.parse("2024-06-01T14:00:00Z"),
                Map.of(),
                Map.of(),
                List.of(
                        new MatchPlayerResponse(
                                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "Novak Djokovic",
                                PlayerSide.HOME,
                                1)),
                0,
                0,
                created,
                created);
    }
}
