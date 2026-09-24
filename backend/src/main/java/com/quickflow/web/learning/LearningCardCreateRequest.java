package com.quickflow.web.learning;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LearningCardCreate")
public record LearningCardCreateRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 200) String title,
		@Schema(types = {"string", "null"}) @Size(max = 2000) String description) {
}
