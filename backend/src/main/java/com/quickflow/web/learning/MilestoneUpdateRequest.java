package com.quickflow.web.learning;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "MilestoneUpdate")
public record MilestoneUpdateRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 200) String title,
		@Schema(types = {"string", "null"}, format = "date") LocalDate targetDate,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull Boolean done) {
}
