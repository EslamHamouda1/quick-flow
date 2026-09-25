package com.quickflow.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.habit.Habit;
import com.quickflow.domain.habit.HabitFrequency;
import com.quickflow.domain.habit.HabitProgress;
import com.quickflow.domain.habit.HabitService;
import com.quickflow.domain.habit.HabitView;
import com.quickflow.domain.learning.LearningCardRepository;
import com.quickflow.domain.learning.LearningMilestoneRepository;
import com.quickflow.domain.learning.LearningStatus;
import com.quickflow.domain.plan.Plan;
import com.quickflow.domain.plan.PlanGroup;
import com.quickflow.domain.plan.PlanItemRef;
import com.quickflow.domain.plan.PlanProgress;
import com.quickflow.domain.plan.PlanService;
import com.quickflow.domain.plan.PlanSourceType;
import com.quickflow.domain.plan.PlanStatus;
import com.quickflow.domain.plan.PlanView;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskRepository;
import com.quickflow.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	/** 2026-09-24 00:30 in Cairo (UTC+3, Egyptian summer time): "today" differs from the UTC date. */
	private static final Instant NOW = Instant.parse("2026-09-23T21:30:00Z");
	private static final Instant EARLIER = Instant.parse("2026-09-20T08:00:00Z");
	private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
	private static final Instant START = Instant.parse("2026-09-23T20:00:00Z");

	@Mock
	private TaskRepository tasks;

	@Mock
	private HabitService habitService;

	@Mock
	private PlanService planService;

	@Mock
	private LearningCardRepository cards;

	@Mock
	private LearningMilestoneRepository milestones;

	private DashboardService service;

	@BeforeEach
	void setUp() {
		service = new DashboardService(tasks, habitService, planService, cards, milestones,
				new TimeService(Clock.fixed(NOW, CAIRO)));
		lenient().when(tasks.findByDueDateAndArchivedFalseOrderByIdAsc(any())).thenReturn(List.of());
		lenient().when(tasks.findOverdue(any())).thenReturn(List.of());
		lenient().when(tasks.findCompletedBetween(any(), any())).thenReturn(List.of());
		lenient().when(tasks.countByArchivedFalse()).thenReturn(0L);
		lenient().when(tasks.countByArchivedFalseAndStatus(any())).thenReturn(0L);
		lenient().when(habitService.list(any())).thenReturn(List.of());
		lenient().when(planService.list(any())).thenReturn(List.of());
		lenient().when(cards.countByStatus(any())).thenReturn(0L);
		lenient().when(milestones.countByDoneTrue()).thenReturn(0L);
		lenient().when(milestones.count()).thenReturn(0L);
	}

	private static Task task(long id, TaskStatus status, LocalDate dueDate) {
		Task task = new Task("Task " + id, null, status, TaskPriority.MEDIUM, dueDate, EARLIER);
		ReflectionTestUtils.setField(task, "id", id);
		return task;
	}

	private static HabitView habit(long id, boolean completedToday) {
		Habit habit = new Habit("Habit " + id, null, HabitFrequency.DAILY, EARLIER);
		ReflectionTestUtils.setField(habit, "id", id);
		return new HabitView(habit, new HabitProgress(completedToday, completedToday, completedToday ? 1 : 0));
	}

	private static PlanView plan(long id, int priority, Instant start, PlanStatus status, int percent) {
		Plan plan = new Plan("Plan " + id, 60, start, start.plus(Duration.ofHours(3)), priority,
				List.of(new PlanItemRef(PlanSourceType.TASK, id)), EARLIER);
		ReflectionTestUtils.setField(plan, "id", id);
		return new PlanView(plan, List.of(), new PlanProgress(status, 0, 1, percent, null));
	}

	private static PlanView plan(long id, PlanStatus status) {
		return plan(id, 1, START, status, 0);
	}

	private static List<Long> planIds(List<PlanView> views) {
		return views.stream().map(v -> v.plan().getId()).toList();
	}

	@Test
	@DisplayName("FR-09.1: due today comes from the repository for today's date in the app zone")
	void fr09_1_dueTodayFromRepositoryForToday() {
		List<Task> due = List.of(task(1L, TaskStatus.TODO, TODAY), task(2L, TaskStatus.DONE, TODAY));
		given(tasks.findByDueDateAndArchivedFalseOrderByIdAsc(TODAY)).willReturn(due);

		assertThat(service.summary().dueToday()).isEqualTo(due);
	}

	@Test
	@DisplayName("FR-09.1: overdue tasks come from findOverdue for today")
	void fr09_1_overdueFromFindOverdue() {
		List<Task> overdue = List.of(task(3L, TaskStatus.TODO, TODAY.minusDays(2)));
		given(tasks.findOverdue(TODAY)).willReturn(overdue);

		assertThat(service.summary().overdue()).isEqualTo(overdue);
	}

	@Test
	@DisplayName("FR-09.1: completed today uses Cairo midnight-to-midnight bounds")
	void fr09_1_completedTodayUsesCairoMidnightBounds() {
		Instant from = TODAY.atStartOfDay(CAIRO).toInstant();
		Instant to = TODAY.plusDays(1).atStartOfDay(CAIRO).toInstant();
		assertThat(from).isEqualTo(Instant.parse("2026-09-23T21:00:00Z"));
		assertThat(to).isEqualTo(Instant.parse("2026-09-24T21:00:00Z"));
		List<Task> completed = List.of(task(4L, TaskStatus.DONE, null));
		given(tasks.findCompletedBetween(from, to)).willReturn(completed);

		assertThat(service.summary().completedToday()).isEqualTo(completed);
		verify(tasks).findCompletedBetween(from, to);
	}

	@Test
	@DisplayName("AC-US5-3: the task completion percent is floored")
	void acUs5_3_completionPercentFloors() {
		given(tasks.countByArchivedFalse()).willReturn(3L);
		given(tasks.countByArchivedFalseAndStatus(TaskStatus.DONE)).willReturn(1L, 2L);

		assertThat(service.summary().taskCompletionPercent()).isEqualTo(33);
		assertThat(service.summary().taskCompletionPercent()).isEqualTo(66);
	}

	@Test
	@DisplayName("AC-US5-3: all tasks done is 100 percent")
	void acUs5_3_completionPercentAllDoneIs100() {
		given(tasks.countByArchivedFalse()).willReturn(4L);
		given(tasks.countByArchivedFalseAndStatus(TaskStatus.DONE)).willReturn(4L);

		assertThat(service.summary().taskCompletionPercent()).isEqualTo(100);
	}

	@Test
	@DisplayName("AC-US5-3: no tasks is 0 percent")
	void acUs5_3_completionPercentZeroWithNoTasks() {
		DashboardSummary summary = service.summary();

		assertThat(summary.taskCompletionPercent()).isZero();
		assertThat(summary.taskCounts()).isEqualTo(new DashboardSummary.TaskCounts(0, 0));
	}

	@Test
	@DisplayName("FR-09.1: task counts are the non-archived total and done")
	void fr09_1_taskCountsNonArchived() {
		given(tasks.countByArchivedFalse()).willReturn(7L);
		given(tasks.countByArchivedFalseAndStatus(TaskStatus.DONE)).willReturn(2L);

		assertThat(service.summary().taskCounts()).isEqualTo(new DashboardSummary.TaskCounts(7, 2));
		verify(tasks).countByArchivedFalseAndStatus(TaskStatus.DONE);
	}

	@Test
	@DisplayName("AC-US5-2: only active habits are shown, in the service's order")
	void acUs5_2_habitsAreActiveOnly() {
		List<HabitView> active = List.of(habit(2L, false), habit(1L, true));
		given(habitService.list(true)).willReturn(active);

		assertThat(service.summary().habits()).isEqualTo(active);
		verify(habitService).list(true);
	}

	@Test
	@DisplayName("AC-US5-2: habit counts are active habits and those completed today")
	void acUs5_2_habitCountsCompletedToday() {
		given(habitService.list(true)).willReturn(List.of(habit(1L, true), habit(2L, false), habit(3L, true)));

		assertThat(service.summary().habitCounts()).isEqualTo(new DashboardSummary.HabitCounts(3, 2));
	}

	@Test
	@DisplayName("AC-US5-4: active plans are only those in progress")
	void acUs5_4_activePlansOnlyInProgress() {
		given(planService.list(PlanGroup.ALL)).willReturn(List.of(plan(1L, PlanStatus.NOT_STARTED),
				plan(2L, PlanStatus.IN_PROGRESS), plan(3L, PlanStatus.COMPLETED)));

		assertThat(planIds(service.summary().activePlans())).containsExactly(2L);
	}

	@Test
	@DisplayName("AC-US5-4: active plans are ordered by priority, then start, then id")
	void acUs5_4_activePlansOrderedByPriorityThenStart() {
		Instant later = START.plus(Duration.ofHours(1));
		given(planService.list(PlanGroup.ALL)).willReturn(List.of(
				plan(3L, 2, START, PlanStatus.IN_PROGRESS, 0),
				plan(2L, 1, later, PlanStatus.IN_PROGRESS, 0),
				plan(1L, 1, later, PlanStatus.IN_PROGRESS, 0),
				plan(4L, 1, START, PlanStatus.IN_PROGRESS, 0)));

		assertThat(planIds(service.summary().activePlans())).containsExactly(4L, 1L, 2L, 3L);
	}

	@Test
	@DisplayName("FR-09.1: plan counts cover every plan by status")
	void fr09_1_planCountsPerStatus() {
		given(planService.list(PlanGroup.ALL)).willReturn(List.of(plan(1L, PlanStatus.NOT_STARTED),
				plan(2L, PlanStatus.IN_PROGRESS), plan(3L, PlanStatus.COMPLETED), plan(4L, PlanStatus.COMPLETED),
				plan(5L, PlanStatus.IN_PROGRESS), plan(6L, PlanStatus.COMPLETED)));

		assertThat(service.summary().planCounts()).isEqualTo(new DashboardSummary.PlanCounts(1, 2, 3));
	}

	@Test
	@DisplayName("AC-US5-5: learning cards are counted per status")
	void acUs5_5_learningCardsPerStatus() {
		given(cards.countByStatus(LearningStatus.NOT_STARTED)).willReturn(4L);
		given(cards.countByStatus(LearningStatus.IN_PROGRESS)).willReturn(2L);
		given(cards.countByStatus(LearningStatus.COMPLETED)).willReturn(1L);

		DashboardSummary.LearningSnapshot learning = service.summary().learning();

		assertThat(learning.notStarted()).isEqualTo(4);
		assertThat(learning.inProgress()).isEqualTo(2);
		assertThat(learning.completed()).isEqualTo(1);
	}

	@Test
	@DisplayName("AC-US5-5: milestones are shown as done of total")
	void acUs5_5_milestonesDoneOfTotal() {
		given(milestones.countByDoneTrue()).willReturn(3L);
		given(milestones.count()).willReturn(8L);

		assertThat(service.summary().learning()).isEqualTo(new DashboardSummary.LearningSnapshot(0, 0, 0, 3, 8));
	}

	@Test
	@DisplayName("FR-09.1: now comes from the clock in the app zone")
	void fr09_1_nowFromClock() {
		DashboardSummary summary = service.summary();

		assertThat(summary.now()).isEqualTo(NOW.atZone(CAIRO).toOffsetDateTime());
		assertThat(summary.now().toInstant()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("FR-08.5: toggling a plan item changes the next summary")
	void fr08_5_planToggleChangesNextSummary() {
		given(planService.list(PlanGroup.ALL)).willReturn(List.of(plan(1L, 1, START, PlanStatus.IN_PROGRESS, 33)),
				List.of(plan(1L, 1, START, PlanStatus.IN_PROGRESS, 66)));

		assertThat(service.summary().activePlans()).singleElement()
				.satisfies(v -> assertThat(v.progress().progressPercent()).isEqualTo(33));
		assertThat(service.summary().activePlans()).singleElement()
				.satisfies(v -> assertThat(v.progress().progressPercent()).isEqualTo(66));
	}

	@Test
	@DisplayName("FR-09.2: figures are recomputed on each call, nothing is cached")
	void fr09_2_figuresRecomputedOnEachCall() {
		service.summary();
		service.summary();

		verify(tasks, times(2)).findByDueDateAndArchivedFalseOrderByIdAsc(TODAY);
		verify(tasks, times(2)).findOverdue(TODAY);
		verify(tasks, times(2)).findCompletedBetween(any(), any());
		verify(tasks, times(2)).countByArchivedFalse();
		verify(tasks, times(2)).countByArchivedFalseAndStatus(TaskStatus.DONE);
		verify(habitService, times(2)).list(true);
		verify(planService, times(2)).list(PlanGroup.ALL);
		verify(cards, times(2)).countByStatus(LearningStatus.NOT_STARTED);
		verify(cards, times(2)).countByStatus(LearningStatus.IN_PROGRESS);
		verify(cards, times(2)).countByStatus(LearningStatus.COMPLETED);
		verify(milestones, times(2)).countByDoneTrue();
		verify(milestones, times(2)).count();
	}

}
