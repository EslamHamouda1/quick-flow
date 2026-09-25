package com.quickflow.web.plan;

import java.time.OffsetDateTime;
import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.plan.Plan;
import com.quickflow.domain.plan.PlanProgress;
import com.quickflow.domain.plan.PlanStatus;
import com.quickflow.domain.plan.PlanView;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "Plan")
public record PlanResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) String title,
		@Schema(requiredMode = RequiredMode.REQUIRED) List<PlanItemResponse> items,
		@Schema(requiredMode = RequiredMode.REQUIRED) int estimatedDurationMinutes,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime startDateTime,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime endDateTime,
		@Schema(requiredMode = RequiredMode.REQUIRED) int priorityOrder,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "Computed on read (FR-08.4, Q2)")
		PlanStatus status,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt,
		@Schema(requiredMode = RequiredMode.REQUIRED) int doneItems,
		@Schema(requiredMode = RequiredMode.REQUIRED) int totalItems,
		@Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0", maximum = "100") int progressPercent,
		@Schema(types = {"integer", "null"}, format = "int64",
				description = "Seconds until endDateTime while start <= now < end, otherwise null (BR-12)")
		Long restSeconds) {

	static PlanResponse from(PlanView view, TimeService time) {
		Plan plan = view.plan();
		PlanProgress progress = view.progress();
		return new PlanResponse(plan.getId(), plan.getTitle(),
				view.items().stream().map(PlanItemResponse::from).toList(), plan.getEstimatedDurationMinutes(),
				time.toOffset(plan.getStartDateTime()), time.toOffset(plan.getEndDateTime()), plan.getPriorityOrder(),
				progress.status(), time.toOffset(plan.getCreatedAt()), progress.doneItems(), progress.totalItems(),
				progress.progressPercent(), progress.restSeconds());
	}

}
