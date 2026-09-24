package com.quickflow.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimeServiceTest {

	private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

	private static TimeService at(Instant instant) {
		return new TimeService(Clock.fixed(instant, CAIRO));
	}

	@Test
	void todayIsCairoDate() {
		TimeService service = at(Instant.parse("2026-09-24T10:00:00Z"));

		assertThat(service.today()).isEqualTo(LocalDate.of(2026, 9, 24));
	}

	@Test
	void todayIsNextDayAt2330Utc() {
		assertThat(at(Instant.parse("2026-09-24T23:30:00Z")).today()).isEqualTo(LocalDate.of(2026, 9, 25));
		assertThat(at(Instant.parse("2026-01-15T23:30:00Z")).today()).isEqualTo(LocalDate.of(2026, 1, 16));
	}

	@Test
	void nowHasCairoOffset() {
		Instant instant = Instant.parse("2026-09-24T10:00:00Z");

		OffsetDateTime now = at(instant).now();

		assertThat(now.getOffset()).isEqualTo(CAIRO.getRules().getOffset(instant));
		assertThat(now.toInstant()).isEqualTo(instant);
	}

	@Test
	void zoneIsAfricaCairo() {
		assertThat(at(Instant.parse("2026-09-24T10:00:00Z")).zone()).isEqualTo(CAIRO);
	}

	@Test
	void toOffsetRendersInstantInCairo() {
		TimeService service = at(Instant.parse("2026-09-24T10:00:00Z"));
		Instant other = Instant.parse("2026-01-15T08:15:30Z");

		OffsetDateTime result = service.toOffset(other);

		assertThat(result.getOffset()).isEqualTo(CAIRO.getRules().getOffset(other));
		assertThat(result.toInstant()).isEqualTo(other);
	}

}
