package dev.sahilbasumatary.analyticsservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.analyticsservice.context.RequestContext;
import dev.sahilbasumatary.analyticsservice.dto.request.CreateSavedViewRequest;
import dev.sahilbasumatary.analyticsservice.entity.SavedAnalyticsView;
import dev.sahilbasumatary.analyticsservice.exception.BadRequestException;
import dev.sahilbasumatary.analyticsservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.analyticsservice.repository.SavedAnalyticsViewRepository;
import dev.sahilbasumatary.analyticsservice.validation.SavedViewConfigValidator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavedAnalyticsViewServiceTest {

    private static final String USER_ID = "user_clerk_abc";
    private static final UUID VIEW_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Mock private SavedAnalyticsViewRepository repository;
    @Mock private SavedViewConfigValidator configValidator;
    @InjectMocks private SavedAnalyticsViewService service;

    @BeforeEach
    void setUp() {
        RequestContext.setUserId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void createSetsUserIdFromContext() {
        when(repository.countByUserId(USER_ID)).thenReturn(0L);
        when(repository.save(any(SavedAnalyticsView.class)))
                .thenAnswer(
                        invocation -> {
                            SavedAnalyticsView view = invocation.getArgument(0);
                            view.setId(VIEW_ID);
                            return view;
                        });
        CreateSavedViewRequest request =
                new CreateSavedViewRequest("My trend", Map.of("chartType", "trend"), true);
        service.create(request);
        ArgumentCaptor<SavedAnalyticsView> captor = ArgumentCaptor.forClass(SavedAnalyticsView.class);
        verify(repository).save(captor.capture());
        SavedAnalyticsView saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("My trend", saved.getName());
        assertEquals(true, saved.isFavorite());
    }

    @Test
    void createRejectsWhenMaxViewsReached() {
        when(repository.countByUserId(USER_ID)).thenReturn(50L);
        CreateSavedViewRequest request =
                new CreateSavedViewRequest("View 51", Map.of("chartType", "overview"), false);
        assertThrows(BadRequestException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void getReturns404WhenViewOwnedByAnotherUser() {
        when(repository.findByIdAndUserId(VIEW_ID, USER_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getForCurrentUser(VIEW_ID));
    }
}
