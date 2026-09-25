package com.quickflow.web.plan;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "PlanCreate")
public record PlanCreateRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 200) String title,
		@Schema(requiredMode = RequiredMode.REQUIRED,
				description = "Existing, distinct tasks/habits/learning cards (BR-10)")
		@NotEmpty @Valid List<PlanItemRefRequest> items,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull @Min(1) @Max(100000) Integer estimatedDurationMinutes,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull OffsetDateTime startDateTime,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "Must be after startDateTime (BR-11)")
		@NotNull OffsetDateTime endDateTime,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "1 is the highest") @NotNull @Min(1)
		Integer priorityOrder) {
}
