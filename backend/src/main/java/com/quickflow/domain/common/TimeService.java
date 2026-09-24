package com.quickflow.domain.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

/** The only source of "now" and "today", in the app time zone of the injected {@link Clock}. */
@Service
public class TimeService {

	private final Clock clock;

	public TimeService(Clock clock) {
		this.clock = clock;
	}

	public OffsetDateTime now() {
		return OffsetDateTime.now(clock);
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

	public ZoneId zone() {
		return clock.getZone();
	}

	public OffsetDateTime toOffset(Instant instant) {
		return instant.atZone(zone()).toOffsetDateTime();
	}

}
