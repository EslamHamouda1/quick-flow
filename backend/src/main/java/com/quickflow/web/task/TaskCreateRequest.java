package com.quickflow.web.task;

import java.time.LocalDate;

import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "TaskCreate")
public record TaskCreateRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 1, max = 200) String title,
		@Schema(types = {"string", "null"}) @Size(max = 2000) String description,
		@Schema(description = "Default TODO") TaskStatus status,
		@Schema(description = "Default MEDIUM") TaskPriority priority,
		@Schema(types = {"string", "null"}, format = "date") LocalDate dueDate) {
}
