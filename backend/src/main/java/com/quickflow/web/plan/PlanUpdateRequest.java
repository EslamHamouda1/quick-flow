package com.quickflow.web.plan;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "PlanUpdate")
public record PlanUpdateRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 200) String title,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Min(1) @Max(100000) Integer estimatedDurationMinutes,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull OffsetDateTime startDateTime,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull OffsetDateTime endDateTime,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Min(1) Integer priorityOrder) {
}
