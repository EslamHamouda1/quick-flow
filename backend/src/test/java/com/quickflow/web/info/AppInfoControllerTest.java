package com.quickflow.web.info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.quickflow.domain.common.TimeService;

@WebMvcTest(AppInfoController.class)
class AppInfoControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TimeService timeService;

	@Test
	void getAppInfoReturnsZoneAndNow() {
		given(this.timeService.zone()).willReturn(ZoneId.of("Africa/Cairo"));
		given(this.timeService.now())
			.willReturn(OffsetDateTime.of(2026, 9, 24, 20, 15, 0, 0, ZoneOffset.ofHours(3)));

		var result = assertThat(this.mvc.get().uri("/api/app-info"))
			.hasStatusOk()
			.hasContentType(MediaType.APPLICATION_JSON)
			.bodyJson();
		result.extractingPath("$.timeZone").isEqualTo("Africa/Cairo");
		result.extractingPath("$.now").isEqualTo("2026-09-24T20:15:00+03:00");
	}

}
