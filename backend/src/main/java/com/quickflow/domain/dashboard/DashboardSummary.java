package com.quickflow.domain.dashboard;

import java.time.OffsetDateTime;
import java.util.List;

import com.quickflow.domain.habit.HabitView;
import com.quickflow.domain.plan.PlanView;
import com.quickflow.domain.task.Task;

/** Everything the Dashboard shows, computed from the current data on each read (FR-09.1, FR-09.2). */
public record DashboardSummary(OffsetDateTime now, List<Task> dueToday, List<Task> overdue,
		List<Task> completedToday, int taskCompletionPercent, TaskCounts taskCounts, List<HabitView> habits,
		HabitCounts habitCounts, List<PlanView> activePlans, PlanCounts planCounts, LearningSnapshot learning) {

	/** Non-archived tasks and those of them that are DONE (AC-US5-3). */
	public record TaskCounts(int total, int done) {
	}

	public record HabitCounts(int active, int completedToday) {
	}

	/** Every plan by computed status. */
	public record PlanCounts(int notStarted, int inProgress, int completed) {
	}

	/** Cards per status and milestones over all cards (AC-US5-5). */
	public record LearningSnapshot(int notStarted, int inProgress, int completed, int milestonesDone,
			int milestonesTotal) {
	}

}
