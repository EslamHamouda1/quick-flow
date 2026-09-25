package com.quickflow.web.settings;

import com.quickflow.domain.settings.AppSettings;
import com.quickflow.domain.settings.DefaultPage;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response body of {@code /api/settings}; the contract uses one schema for both. */
@Schema(name = "Settings", description = "Profile and preferences of the single user (FR-10.2, Q3)")
public record SettingsDto(
		@Schema(types = {"string", "null"}) @Size(max = 100) String displayName,
		@Schema(requiredMode = RequiredMode.REQUIRED, defaultValue = "true") @NotNull Boolean planStartNotifications,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull DefaultPage defaultPage) {

	public static SettingsDto from(AppSettings settings) {
		return new SettingsDto(settings.getDisplayName(), settings.isPlanStartNotifications(),
				settings.getDefaultPage());
	}

}
