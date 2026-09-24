package com.quickflow.web.learning;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "NoteCreate")
public record NoteCreateRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 5000) String text) {
}
