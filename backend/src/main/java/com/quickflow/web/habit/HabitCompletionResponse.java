package com.quickflow.web.habit;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.habit.HabitCompletion;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "HabitCompletion")
public record HabitCompletionResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) Long habitId,
		@Schema(requiredMode = RequiredMode.REQUIRED) LocalDate completionDate,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt) {

	static HabitCompletionResponse from(HabitCompletion completion, TimeService time) {
		return new HabitCompletionResponse(completion.getId(), completion.getHabitId(),
				completion.getCompletionDate(), time.toOffset(completion.getCreatedAt()));
	}

}
