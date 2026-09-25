package com.quickflow.domain.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingsService {

	private final SettingsRepository settings;

	public SettingsService(SettingsRepository settings) {
		this.settings = settings;
	}

	/** The single settings row, created with the defaults on first read (FR-10.2, A-2). */
	public AppSettings get() {
		return settings.findById(AppSettings.ID).orElseGet(() -> settings.save(AppSettings.defaults()));
	}

	/** Full replace of the three fields (A-1); works on an empty database too. */
	public AppSettings update(String displayName, Boolean planStartNotifications, DefaultPage defaultPage) {
		AppSettings current = get();
		current.update(displayName, planStartNotifications, defaultPage);
		return current;
	}

}
