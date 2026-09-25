package com.quickflow.domain.settings;

import java.util.ArrayList;
import java.util.List;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The single user's profile and preferences, one row with id 1 (FR-10.2). */
@Entity
@Table(name = "app_settings")
public class AppSettings {

	public static final long ID = 1L;

	static final int DISPLAY_NAME_MAX = 100;

	@Id
	private Long id;

	@Column(length = DISPLAY_NAME_MAX)
	private String displayName;

	@Column(nullable = false)
	private boolean planStartNotifications;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DefaultPage defaultPage;

	protected AppSettings() {
	}

	/** The row used before the user saves anything: no name, notifications on, Dashboard first. */
	public static AppSettings defaults() {
		AppSettings settings = new AppSettings();
		settings.id = ID;
		settings.displayName = null;
		settings.planStartNotifications = true;
		settings.defaultPage = DefaultPage.DASHBOARD;
		return settings;
	}

	/** Replaces all three fields, or none of them if any is invalid. */
	public void update(String displayName, Boolean planStartNotifications, DefaultPage defaultPage) {
		List<FieldError> errors = new ArrayList<>();
		String cleanDisplayName = checkDisplayName(displayName, errors);
		if (planStartNotifications == null) {
			errors.add(new FieldError("planStartNotifications", "must not be null"));
		}
		if (defaultPage == null) {
			errors.add(new FieldError("defaultPage", "must not be null"));
		}
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
		this.displayName = cleanDisplayName;
		this.planStartNotifications = planStartNotifications;
		this.defaultPage = defaultPage;
	}

	/** Trimmed, at most 100 characters; blank means no name. */
	private static String checkDisplayName(String displayName, List<FieldError> errors) {
		if (displayName == null) {
			return null;
		}
		String trimmed = displayName.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() > DISPLAY_NAME_MAX) {
			errors.add(new FieldError("displayName", "must be at most " + DISPLAY_NAME_MAX + " characters"));
		}
		return trimmed;
	}

	public Long getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isPlanStartNotifications() {
		return planStartNotifications;
	}

	public DefaultPage getDefaultPage() {
		return defaultPage;
	}

}
