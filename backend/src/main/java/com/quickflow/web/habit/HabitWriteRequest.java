package com.quickflow.web.habit;

import com.quickflow.domain.habit.HabitFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "HabitWrite")
public record HabitWriteRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 150) String name,
		@Schema(types = {"string", "null"}) @Size(max = 2000) String description,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull HabitFrequency frequency) {
}
