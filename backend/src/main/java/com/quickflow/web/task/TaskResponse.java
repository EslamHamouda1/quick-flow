package com.quickflow.web.task;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "Task")
public record TaskResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) String title,
		@Schema(types = {"string", "null"}) String description,
		@Schema(requiredMode = RequiredMode.REQUIRED) TaskStatus status,
		@Schema(requiredMode = RequiredMode.REQUIRED) TaskPriority priority,
		@Schema(types = {"string", "null"}, format = "date") LocalDate dueDate,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime updatedAt,
		@Schema(types = {"string", "null"}, format = "date-time") OffsetDateTime completedAt,
		@Schema(requiredMode = RequiredMode.REQUIRED) boolean archived,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "Computed on read (FR-01.7)") boolean overdue,
		@ArraySchema(arraySchema = @Schema(requiredMode = RequiredMode.REQUIRED,
				description = "Lower case (Q1), alphabetical; empty when the task has no tags"), maxItems = 10,
				uniqueItems = true, schema = @Schema(minLength = 1, maxLength = 30)) List<String> tags) {

	public static TaskResponse from(Task task, TimeService time) {
		return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
				task.getPriority(), task.getDueDate(), time.toOffset(task.getCreatedAt()),
				time.toOffset(task.getUpdatedAt()),
				task.getCompletedAt() == null ? null : time.toOffset(task.getCompletedAt()), task.isArchived(),
				task.isOverdue(time.today()), task.getTags());
	}

}
