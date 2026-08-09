package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.UserPreference;
import java.util.Map;
import java.util.UUID;

public record UserPreferenceResponse(
        UUID id,
        String theme,
        boolean notificationsEnabled,
        boolean emailNotifications,
        boolean pushNotifications,
        String favoriteSurface,
        String locale,
        Map<String, Object> extraSettings) {

    public static UserPreferenceResponse from(UserPreference pref) {
        return new UserPreferenceResponse(
                pref.getId(),
                pref.getTheme(),
                pref.isNotificationsEnabled(),
                pref.isEmailNotifications(),
                pref.isPushNotifications(),
                pref.getFavoriteSurface(),
                pref.getLocale(),
                pref.getExtraSettings());
    }
}
