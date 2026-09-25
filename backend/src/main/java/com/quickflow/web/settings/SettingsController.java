package com.quickflow.web.settings;

import com.quickflow.domain.settings.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "settings")
public class SettingsController {

	private final SettingsService settingsService;

	public SettingsController(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Current settings (defaults on first read)")
	public SettingsDto getSettings() {
		return SettingsDto.from(settingsService.get());
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Replace the settings")
	public SettingsDto updateSettings(@Valid @RequestBody SettingsDto request) {
		return SettingsDto.from(
				settingsService.update(request.displayName(), request.planStartNotifications(), request.defaultPage()));
	}

}
