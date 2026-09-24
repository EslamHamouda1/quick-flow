package com.quickflow.domain.habit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum HabitFrequency {
	DAILY, WEEKLY
}
