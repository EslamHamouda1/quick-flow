package com.quickflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.quickflow.domain.settings.AppSettings;
import com.quickflow.domain.settings.DefaultPage;
import com.quickflow.domain.settings.SettingsRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
class SettingsRepositoryTest {

	@Autowired
	private SettingsRepository settings;

	@Autowired
	private EntityManager em;

	@Test
	@DisplayName("NFR-3: the settings row is saved and read back with its default values")
	void nfr3_settingsRowSavedAndReadBack() {
		settings.saveAndFlush(AppSettings.defaults());
		em.clear();

		AppSettings read = settings.findById(AppSettings.ID).orElseThrow();
		assertThat(read.getId()).isEqualTo(1L);
		assertThat(read.getDisplayName()).isNull();
		assertThat(read.isPlanStartNotifications()).isTrue();
		assertThat(read.getDefaultPage()).isEqualTo(DefaultPage.DASHBOARD);
	}

	@Test
	@DisplayName("NFR-3: updated settings are read back after flush and clear")
	void nfr3_updatedRowReadBackAfterFlushAndClear() {
		AppSettings saved = settings.saveAndFlush(AppSettings.defaults());
		saved.update("Eve", false, DefaultPage.HABITS);
		em.flush();
		em.clear();

		AppSettings read = settings.findById(AppSettings.ID).orElseThrow();
		assertThat(read.getDisplayName()).isEqualTo("Eve");
		assertThat(read.isPlanStartNotifications()).isFalse();
		assertThat(read.getDefaultPage()).isEqualTo(DefaultPage.HABITS);
	}

	@Test
	@DisplayName("NFR-3: repeated saves keep exactly one settings row holding the latest values")
	void nfr3_onlyOneRowAfterRepeatedSaves() {
		settings.saveAndFlush(AppSettings.defaults());
		AppSettings second = AppSettings.defaults();
		second.update("Eve", false, DefaultPage.LEARNING);
		AppSettings saved = settings.saveAndFlush(second);
		assertThat(saved.getId()).isEqualTo(AppSettings.ID);
		em.clear();

		assertThat(settings.count()).isEqualTo(1L);
		AppSettings read = settings.findById(AppSettings.ID).orElseThrow();
		assertThat(read.getDisplayName()).isEqualTo("Eve");
		assertThat(read.isPlanStartNotifications()).isFalse();
		assertThat(read.getDefaultPage()).isEqualTo(DefaultPage.LEARNING);
	}

}
