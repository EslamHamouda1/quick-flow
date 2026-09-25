package com.quickflow.domain.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

	@Mock
	private SettingsRepository settings;

	private SettingsService service;

	@BeforeEach
	void setUp() {
		service = new SettingsService(settings);
	}

	private static AppSettings existing() {
		AppSettings row = AppSettings.defaults();
		row.update("Ada", false, DefaultPage.TASKS);
		return row;
	}

	private void givenExisting(AppSettings row) {
		given(settings.findById(AppSettings.ID)).willReturn(Optional.of(row));
	}

	private void givenEmptyDatabase() {
		given(settings.findById(AppSettings.ID)).willReturn(Optional.empty());
		given(settings.save(any(AppSettings.class))).willAnswer(inv -> inv.getArgument(0));
	}

	@Test
	@DisplayName("FR-10.2: the first read returns the default settings")
	void fr10_2_firstReadReturnsDefaults() {
		givenEmptyDatabase();

		AppSettings result = service.get();

		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getDisplayName()).isNull();
		assertThat(result.isPlanStartNotifications()).isTrue();
		assertThat(result.getDefaultPage()).isEqualTo(DefaultPage.DASHBOARD);
	}

	@Test
	@DisplayName("FR-10.2: the first read stores the defaults as the single row with id 1")
	void fr10_2_firstReadStoresDefaults() {
		givenEmptyDatabase();

		AppSettings result = service.get();

		ArgumentCaptor<AppSettings> saved = ArgumentCaptor.forClass(AppSettings.class);
		verify(settings, times(1)).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(AppSettings.ID);
		assertThat(saved.getValue().getDisplayName()).isNull();
		assertThat(saved.getValue().isPlanStartNotifications()).isTrue();
		assertThat(saved.getValue().getDefaultPage()).isEqualTo(DefaultPage.DASHBOARD);
		assertThat(result).isSameAs(saved.getValue());
	}

	@Test
	@DisplayName("FR-10.2: an existing row is returned and not recreated")
	void fr10_2_existingRowReturnedNotRecreated() {
		AppSettings row = existing();
		givenExisting(row);

		AppSettings result = service.get();

		assertThat(result).isSameAs(row);
		assertThat(result.getDisplayName()).isEqualTo("Ada");
		assertThat(result.isPlanStartNotifications()).isFalse();
		assertThat(result.getDefaultPage()).isEqualTo(DefaultPage.TASKS);
		verify(settings, never()).save(any(AppSettings.class));
	}

	@Test
	@DisplayName("FR-10.2: an update stores all three fields")
	void fr10_2_updateSavesAllThreeFields() {
		AppSettings row = existing();
		givenExisting(row);

		AppSettings result = service.update("Grace", true, DefaultPage.HABITS);

		assertThat(result).isSameAs(row);
		assertThat(result.getId()).isEqualTo(AppSettings.ID);
		assertThat(result.getDisplayName()).isEqualTo("Grace");
		assertThat(result.isPlanStartNotifications()).isTrue();
		assertThat(result.getDefaultPage()).isEqualTo(DefaultPage.HABITS);
	}

	@Test
	@DisplayName("FR-10.2: an update on an empty database creates the row and applies the values")
	void fr10_2_updateOnEmptyDatabaseCreatesRow() {
		givenEmptyDatabase();

		AppSettings result = service.update("Linus", false, DefaultPage.PLANS);

		ArgumentCaptor<AppSettings> saved = ArgumentCaptor.forClass(AppSettings.class);
		verify(settings, times(1)).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(AppSettings.ID);
		assertThat(result).isSameAs(saved.getValue());
		assertThat(result.getDisplayName()).isEqualTo("Linus");
		assertThat(result.isPlanStartNotifications()).isFalse();
		assertThat(result.getDefaultPage()).isEqualTo(DefaultPage.PLANS);
	}

	@Test
	@DisplayName("FR-10.2: a display name over 100 characters is rejected on field displayName")
	void fr10_2_displayNameOver100Rejected() {
		givenExisting(existing());

		assertThatThrownBy(() -> service.update("a".repeat(101), true, DefaultPage.DASHBOARD))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("displayName"));
	}

	@Test
	@DisplayName("FR-10.2: a display name of exactly 100 characters is accepted")
	void fr10_2_displayNameOf100Accepted() {
		givenExisting(existing());
		String name = "b".repeat(100);

		AppSettings result = service.update(name, true, DefaultPage.DASHBOARD);

		assertThat(result.getDisplayName()).isEqualTo(name);
	}

	@Test
	@DisplayName("FR-10.2: a null display name clears the stored name")
	void fr10_2_displayNameNullClears() {
		givenExisting(existing());

		AppSettings result = service.update(null, false, DefaultPage.TASKS);

		assertThat(result.getDisplayName()).isNull();
	}

	@Test
	@DisplayName("FR-10.2: a blank display name is stored as null and a padded name is trimmed")
	void fr10_2_blankDisplayNameStoredAsNull() {
		AppSettings row = existing();
		givenExisting(row);

		AppSettings blank = service.update("   ", false, DefaultPage.TASKS);
		assertThat(blank.getDisplayName()).isNull();

		AppSettings trimmed = service.update("  Eve  ", false, DefaultPage.TASKS);
		assertThat(trimmed.getDisplayName()).isEqualTo("Eve");
	}

	@Test
	@DisplayName("FR-10.2: a null default page is rejected on field defaultPage")
	void fr10_2_nullDefaultPageRejected() {
		givenExisting(existing());

		assertThatThrownBy(() -> service.update("Ada", true, null))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("defaultPage"));
	}

	@Test
	@DisplayName("FR-10.2: a null plan-start notifications flag is rejected on field planStartNotifications")
	void fr10_2_nullPlanStartNotificationsRejected() {
		givenExisting(existing());

		assertThatThrownBy(() -> service.update("Ada", null, DefaultPage.DASHBOARD))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("planStartNotifications"));
	}

	@Test
	@DisplayName("FR-10.2: a rejected update leaves the stored values unchanged")
	void fr10_2_rejectedUpdateLeavesValuesUnchanged() {
		AppSettings row = existing();
		givenExisting(row);

		assertThatThrownBy(() -> service.update("c".repeat(101), null, null))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field)
						.containsExactlyInAnyOrder("displayName", "planStartNotifications", "defaultPage"));

		assertThat(row.getDisplayName()).isEqualTo("Ada");
		assertThat(row.isPlanStartNotifications()).isFalse();
		assertThat(row.getDefaultPage()).isEqualTo(DefaultPage.TASKS);
		verify(settings, never()).save(any(AppSettings.class));
	}
}
