package com.quickflow.web.habit;

import java.time.OffsetDateTime;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.habit.Habit;
import com.quickflow.domain.habit.HabitFrequency;
import com.quickflow.domain.habit.HabitProgress;
import com.quickflow.domain.habit.HabitView;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "Habit")
public record HabitResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) String name,
		@Schema(types = {"string", "null"}) String description,
		@Schema(requiredMode = RequiredMode.REQUIRED) HabitFrequency frequency,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt,
		@Schema(requiredMode = RequiredMode.REQUIRED) boolean active,
		@Schema(requiredMode = RequiredMode.REQUIRED) boolean completedToday,
		@Schema(requiredMode = RequiredMode.REQUIRED,
				description = "Today (DAILY) or current Monday-Sunday week (WEEKLY)") boolean doneForCurrentPeriod,
		@Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0",
				description = "Consecutive periods with a completion") int currentStreak) {

	public static HabitResponse from(HabitView view, TimeService time) {
		Habit habit = view.habit();
		HabitProgress progress = view.progress();
		return new HabitResponse(habit.getId(), habit.getName(), habit.getDescription(), habit.getFrequency(),
				time.toOffset(habit.getCreatedAt()), habit.isActive(), progress.completedToday(),
				progress.doneForCurrentPeriod(), progress.currentStreak());
	}

}
