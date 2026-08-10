package dev.sahilbasumatary.tennisdataservice.contract;

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
import dev.sahilbasumatary.tennisdataservice.controller.PlayerController;
import dev.sahilbasumatary.tennisdataservice.dto.response.PlayerResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Backhand;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Hand;
import dev.sahilbasumatary.tennisdataservice.service.PlayerIdentityService;
import dev.sahilbasumatary.tennisdataservice.service.PlayerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Provider("tennis-data-service")
@PactFolder("pacts")
class PlayerControllerProviderPactTest {

    private final PlayerService playerService = mock(PlayerService.class);
    private final PlayerIdentityService playerIdentityService = mock(PlayerIdentityService.class);

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
        target.setControllers(new PlayerController(playerService, playerIdentityService));
        target.setMessageConverters(json);
        context.setTarget(target);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("players exist")
    void playersExist() {
        Instant stamped = Instant.parse("2024-01-01T12:00:00Z");
        when(playerService.listPlayers(isNull(), isNull()))
                .thenReturn(
                        List.of(
                                new PlayerResponse(
                                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                        "atp-100",
                                        "Novak",
                                        "Djokovic",
                                        "SRB",
                                        LocalDate.of(1987, 5, 22),
                                        Hand.RIGHT,
                                        Backhand.TWO_HANDED,
                                        188,
                                        77,
                                        2003,
                                        1,
                                        11000,
                                        Gender.MALE,
                                        stamped,
                                        stamped)));
    }
}
