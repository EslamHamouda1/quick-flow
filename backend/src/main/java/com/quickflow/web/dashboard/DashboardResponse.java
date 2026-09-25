package com.quickflow.web.dashboard;

import java.time.OffsetDateTime;
import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.dashboard.DashboardSummary;
import com.quickflow.domain.task.Task;
import com.quickflow.web.habit.HabitResponse;
import com.quickflow.web.plan.PlanResponse;
import com.quickflow.web.task.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "Dashboard")
public record DashboardResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime now,
		@Schema(requiredMode = RequiredMode.REQUIRED) List<TaskResponse> dueToday,
		@Schema(requiredMode = RequiredMode.REQUIRED) List<TaskResponse> overdue,
		@Schema(requiredMode = RequiredMode.REQUIRED) List<TaskResponse> completedToday,
		@Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0", maximum = "100") int taskCompletionPercent,
		@Schema(requiredMode = RequiredMode.REQUIRED) TaskCounts taskCounts,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "Active habits") List<HabitResponse> habits,
		@Schema(requiredMode = RequiredMode.REQUIRED) HabitCounts habitCounts,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "IN_PROGRESS plans by priority")
		List<PlanResponse> activePlans,
		@Schema(requiredMode = RequiredMode.REQUIRED) PlanCounts planCounts,
		@Schema(requiredMode = RequiredMode.REQUIRED) LearningSnapshot learning) {

	@Schema(name = "TaskCounts")
	public record TaskCounts(
			@Schema(requiredMode = RequiredMode.REQUIRED, description = "Non-archived tasks") int total,
			@Schema(requiredMode = RequiredMode.REQUIRED) int done) {
	}

	@Schema(name = "HabitCounts")
	public record HabitCounts(
			@Schema(requiredMode = RequiredMode.REQUIRED) int active,
			@Schema(requiredMode = RequiredMode.REQUIRED) int completedToday) {
	}

	@Schema(name = "PlanCounts")
	public record PlanCounts(
			@Schema(requiredMode = RequiredMode.REQUIRED) int notStarted,
			@Schema(requiredMode = RequiredMode.REQUIRED) int inProgress,
			@Schema(requiredMode = RequiredMode.REQUIRED) int completed) {
	}

	@Schema(name = "LearningSnapshot")
	public record LearningSnapshot(
			@Schema(requiredMode = RequiredMode.REQUIRED) int notStarted,
			@Schema(requiredMode = RequiredMode.REQUIRED) int inProgress,
			@Schema(requiredMode = RequiredMode.REQUIRED) int completed,
			@Schema(requiredMode = RequiredMode.REQUIRED) int milestonesDone,
			@Schema(requiredMode = RequiredMode.REQUIRED) int milestonesTotal) {
	}

	static DashboardResponse from(DashboardSummary s, TimeService time) {
		DashboardSummary.TaskCounts tc = s.taskCounts();
		DashboardSummary.HabitCounts hc = s.habitCounts();
		DashboardSummary.PlanCounts pc = s.planCounts();
		DashboardSummary.LearningSnapshot l = s.learning();
		return new DashboardResponse(s.now(), tasks(s.dueToday(), time), tasks(s.overdue(), time),
				tasks(s.completedToday(), time), s.taskCompletionPercent(), new TaskCounts(tc.total(), tc.done()),
				s.habits().stream().map(h -> HabitResponse.from(h, time)).toList(),
				new HabitCounts(hc.active(), hc.completedToday()),
				s.activePlans().stream().map(p -> PlanResponse.from(p, time)).toList(),
				new PlanCounts(pc.notStarted(), pc.inProgress(), pc.completed()),
				new LearningSnapshot(l.notStarted(), l.inProgress(), l.completed(), l.milestonesDone(),
						l.milestonesTotal()));
	}

	private static List<TaskResponse> tasks(List<Task> tasks, TimeService time) {
		return tasks.stream().map(t -> TaskResponse.from(t, time)).toList();
	}

}
