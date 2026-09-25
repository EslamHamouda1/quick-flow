package com.quickflow.web.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.quickflow.domain.settings.AppSettings;
import com.quickflow.domain.settings.DefaultPage;
import com.quickflow.domain.settings.SettingsService;

@WebMvcTest(SettingsController.class)
class SettingsControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private SettingsService settingsService;

	private void assertBadRequestOnField(String body, String field) {
		var result = assertThat(this.mvc.put().uri("/api/settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo(field);

		verifyNoInteractions(this.settingsService);
	}

	@Test
	@DisplayName("GET /api/settings returns 200 with exactly the contract fields")
	void getSettingsReturns200WithExactlyContractFields() {
		given(this.settingsService.get()).willReturn(AppSettings.defaults());

		var result = assertThat(this.mvc.get().uri("/api/settings"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.displayName").isNull();
		json.extractingPath("$.planStartNotifications").isEqualTo(true);
		json.extractingPath("$.defaultPage").isEqualTo("DASHBOARD");
		json.extractingPath("$").asMap().containsOnlyKeys("displayName", "planStartNotifications", "defaultPage");

		then(this.settingsService).should().get();
	}

	@Test
	@DisplayName("PUT /api/settings saves the settings and returns 200 with the saved values")
	void updateSettingsReturns200WithSavedValues() {
		AppSettings saved = AppSettings.defaults();
		saved.update("Eve", false, DefaultPage.HABITS);
		given(this.settingsService.update("Eve", false, DefaultPage.HABITS)).willReturn(saved);

		var result = assertThat(this.mvc.put().uri("/api/settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"displayName":"Eve","planStartNotifications":false,"defaultPage":"HABITS"}
						"""));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.displayName").isEqualTo("Eve");
		json.extractingPath("$.planStartNotifications").isEqualTo(false);
		json.extractingPath("$.defaultPage").isEqualTo("HABITS");

		then(this.settingsService).should().update("Eve", false, DefaultPage.HABITS);
	}

	@Test
	@DisplayName("an unknown defaultPage is rejected with 400 and field defaultPage")
	void updateSettingsUnknownDefaultPageIs400() {
		assertBadRequestOnField("{\"displayName\":\"Eve\",\"planStartNotifications\":true,\"defaultPage\":\"FOO\"}",
				"defaultPage");
	}

	@Test
	@DisplayName("a displayName longer than 100 characters is rejected with 400 and field displayName")
	void updateSettingsTooLongDisplayNameIs400() {
		String tooLong = "a".repeat(101);
		assertBadRequestOnField("{\"displayName\":\"" + tooLong
				+ "\",\"planStartNotifications\":true,\"defaultPage\":\"DASHBOARD\"}", "displayName");
	}

	@Test
	@DisplayName("a missing defaultPage is rejected with 400 and field defaultPage")
	void updateSettingsMissingDefaultPageIs400() {
		assertBadRequestOnField("{\"displayName\":\"Eve\",\"planStartNotifications\":true}", "defaultPage");
	}

	@Test
	@DisplayName("a missing planStartNotifications is rejected with 400 and field planStartNotifications")
	void updateSettingsMissingPlanStartNotificationsIs400() {
		assertBadRequestOnField("{\"displayName\":\"Eve\",\"defaultPage\":\"DASHBOARD\"}", "planStartNotifications");
	}

	@Test
	@DisplayName("a non-boolean planStartNotifications is rejected with 400")
	void updateSettingsNonBooleanPlanStartNotificationsIs400() {
		var result = assertThat(this.mvc.put().uri("/api/settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Eve\",\"planStartNotifications\":\"yes\",\"defaultPage\":\"DASHBOARD\"}"));
		result.hasStatus(400);

		verifyNoInteractions(this.settingsService);
	}

	@Test
	@DisplayName("an invalid PUT never reaches the settings service")
	void updateSettingsInvalidDoesNotCallService() {
		var result = assertThat(this.mvc.put().uri("/api/settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"));
		result.hasStatus(400);

		verifyNoInteractions(this.settingsService);
	}

}
