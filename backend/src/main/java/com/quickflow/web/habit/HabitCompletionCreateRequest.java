package com.quickflow.web.habit;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HabitCompletionCreate")
public record HabitCompletionCreateRequest(
		@Schema(types = {"string", "null"}, format = "date",
				description = "Default today; a future date is rejected") LocalDate date) {
}
