package com.quickflow.domain.dashboard;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.dashboard.DashboardSummary.HabitCounts;
import com.quickflow.domain.dashboard.DashboardSummary.LearningSnapshot;
import com.quickflow.domain.dashboard.DashboardSummary.PlanCounts;
import com.quickflow.domain.dashboard.DashboardSummary.TaskCounts;
import com.quickflow.domain.habit.HabitService;
import com.quickflow.domain.habit.HabitView;
import com.quickflow.domain.learning.LearningCardRepository;
import com.quickflow.domain.learning.LearningMilestoneRepository;
import com.quickflow.domain.learning.LearningStatus;
import com.quickflow.domain.plan.PlanGroup;
import com.quickflow.domain.plan.PlanService;
import com.quickflow.domain.plan.PlanStatus;
import com.quickflow.domain.plan.PlanView;
import com.quickflow.domain.task.TaskRepository;
import com.quickflow.domain.task.TaskStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the Dashboard from the current data on every call; nothing is cached (FR-09.2, A-9). */
@Service
@Transactional(readOnly = true)
public class DashboardService {

	/** Active plans: priority order, then start, then id (A-6). */
	private static final Comparator<PlanView> ACTIVE_ORDER = Comparator
			.comparingInt((PlanView v) -> v.plan().getPriorityOrder())
			.thenComparing(v -> v.plan().getStartDateTime())
			.thenComparing(v -> v.plan().getId(), Comparator.nullsLast(Comparator.naturalOrder()));

	private final TaskRepository tasks;

	private final HabitService habitService;

	private final PlanService planService;

	private final LearningCardRepository cards;

	private final LearningMilestoneRepository milestones;

	private final TimeService timeService;

	public DashboardService(TaskRepository tasks, HabitService habitService, PlanService planService,
			LearningCardRepository cards, LearningMilestoneRepository milestones, TimeService timeService) {
		this.tasks = tasks;
		this.habitService = habitService;
		this.planService = planService;
		this.cards = cards;
		this.milestones = milestones;
		this.timeService = timeService;
	}

	public DashboardSummary summary() {
		LocalDate today = timeService.today();
		ZoneId zone = timeService.zone();

		long total = tasks.countByArchivedFalse();
		long done = tasks.countByArchivedFalseAndStatus(TaskStatus.DONE);
		int percent = total == 0 ? 0 : (int) (100 * done / total);

		List<HabitView> habits = habitService.list(true);
		int habitsDoneToday = (int) habits.stream().filter(h -> h.progress().completedToday()).count();

		List<PlanView> plans = planService.list(PlanGroup.ALL);
		List<PlanView> activePlans = plans.stream()
				.filter(v -> v.progress().status() == PlanStatus.IN_PROGRESS)
				.sorted(ACTIVE_ORDER)
				.toList();

		return new DashboardSummary(timeService.now(), tasks.findByDueDateAndArchivedFalseOrderByIdAsc(today),
				tasks.findOverdue(today),
				tasks.findCompletedBetween(today.atStartOfDay(zone).toInstant(),
						today.plusDays(1).atStartOfDay(zone).toInstant()),
				percent, new TaskCounts((int) total, (int) done), habits,
				new HabitCounts(habits.size(), habitsDoneToday), activePlans,
				new PlanCounts(count(plans, PlanStatus.NOT_STARTED), count(plans, PlanStatus.IN_PROGRESS),
						count(plans, PlanStatus.COMPLETED)),
				new LearningSnapshot((int) cards.countByStatus(LearningStatus.NOT_STARTED),
						(int) cards.countByStatus(LearningStatus.IN_PROGRESS),
						(int) cards.countByStatus(LearningStatus.COMPLETED), (int) milestones.countByDoneTrue(),
						(int) milestones.count()));
	}

	private static int count(List<PlanView> plans, PlanStatus status) {
		return (int) plans.stream().filter(v -> v.progress().status() == status).count();
	}

}
