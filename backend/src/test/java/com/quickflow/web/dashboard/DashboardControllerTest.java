package com.quickflow.web.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.dashboard.DashboardService;
import com.quickflow.domain.dashboard.DashboardSummary;
import com.quickflow.domain.habit.Habit;
import com.quickflow.domain.habit.HabitFrequency;
import com.quickflow.domain.habit.HabitProgress;
import com.quickflow.domain.habit.HabitView;
import com.quickflow.domain.plan.Plan;
import com.quickflow.domain.plan.PlanItem;
import com.quickflow.domain.plan.PlanItemRef;
import com.quickflow.domain.plan.PlanProgress;
import com.quickflow.domain.plan.PlanSourceType;
import com.quickflow.domain.plan.PlanStatus;
import com.quickflow.domain.plan.PlanView;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskStatus;

@WebMvcTest(DashboardController.class)
@Import(DashboardControllerTest.FixedTime.class)
class DashboardControllerTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

	private static final OffsetDateTime NOW_LOCAL = NOW.atZone(CAIRO).toOffsetDateTime();

	@TestConfiguration
	static class FixedTime {

		@Bean
		TimeService timeService() {
			return new TimeService(Clock.fixed(NOW, CAIRO));
		}

	}

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private DashboardService dashboardService;

	private static Task task(long id, String title, TaskStatus status, LocalDate dueDate) {
		Task task = new Task(title, null, status, TaskPriority.HIGH, dueDate, NOW);
		ReflectionTestUtils.setField(task, "id", id);
		return task;
	}

	private static HabitView habit() {
		Habit habit = new Habit("Read", "20 pages", HabitFrequency.DAILY, NOW);
		ReflectionTestUtils.setField(habit, "id", 7L);
		return new HabitView(habit, new HabitProgress(true, true, 3));
	}

	private static PlanView plan() {
		Plan plan = new Plan("Weekly focus", 120, NOW, NOW.plusSeconds(7200), 1,
				List.of(new PlanItemRef(PlanSourceType.TASK, 11L), new PlanItemRef(PlanSourceType.HABIT, 12L)), NOW);
		ReflectionTestUtils.setField(plan, "id", 5L);
		List<PlanItem> planItems = plan.getItems();
		ReflectionTestUtils.setField(planItems.get(0), "id", 21L);
		ReflectionTestUtils.setField(planItems.get(1), "id", 22L);
		planItems.get(0).refreshTitle("Write report");
		planItems.get(1).refreshTitle("Read");
		planItems.get(0).setDone(true);
		List<PlanView.Item> items = List.of(new PlanView.Item(planItems.get(0), "Write report", false),
				new PlanView.Item(planItems.get(1), "Read", false));
		return new PlanView(plan, items, new PlanProgress(PlanStatus.IN_PROGRESS, 1, 2, 50, 7200L));
	}

	private static DashboardSummary fullSummary() {
		return new DashboardSummary(NOW_LOCAL,
				List.of(task(1L, "Due today", TaskStatus.TODO, LocalDate.of(2026, 9, 24))),
				List.of(task(2L, "Late", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 9, 20))),
				List.of(task(3L, "Finished", TaskStatus.DONE, LocalDate.of(2026, 9, 24))), 33,
				new DashboardSummary.TaskCounts(3, 1), List.of(habit()), new DashboardSummary.HabitCounts(1, 1),
				List.of(plan()), new DashboardSummary.PlanCounts(2, 1, 4),
				new DashboardSummary.LearningSnapshot(1, 2, 3, 4, 9));
	}

	private static DashboardSummary emptySummary() {
		return new DashboardSummary(NOW_LOCAL, List.of(), List.of(), List.of(), 0,
				new DashboardSummary.TaskCounts(0, 0), List.of(), new DashboardSummary.HabitCounts(0, 0), List.of(),
				new DashboardSummary.PlanCounts(0, 0, 0), new DashboardSummary.LearningSnapshot(0, 0, 0, 0, 0));
	}

	@Test
	@DisplayName("US5: GET /api/dashboard returns 200 with every contract field")
	void getDashboardReturns200WithEveryContractField() {
		given(this.dashboardService.summary()).willReturn(fullSummary());

		var result = assertThat(this.mvc.get().uri("/api/dashboard"));
		result.hasStatus(200).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.now").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.dueToday").asArray().hasSize(1);
		json.extractingPath("$.dueToday[0].id").isEqualTo(1);
		json.extractingPath("$.dueToday[0].title").isEqualTo("Due today");
		json.extractingPath("$.dueToday[0].status").isEqualTo("TODO");
		json.extractingPath("$.dueToday[0].priority").isEqualTo("HIGH");
		json.extractingPath("$.dueToday[0].dueDate").isEqualTo("2026-09-24");
		json.extractingPath("$.dueToday[0].overdue").isEqualTo(false);
		json.extractingPath("$.overdue").asArray().hasSize(1);
		json.extractingPath("$.overdue[0].id").isEqualTo(2);
		json.extractingPath("$.overdue[0].overdue").isEqualTo(true);
		json.extractingPath("$.completedToday").asArray().hasSize(1);
		json.extractingPath("$.completedToday[0].id").isEqualTo(3);
		json.extractingPath("$.completedToday[0].status").isEqualTo("DONE");
		json.extractingPath("$.completedToday[0].completedAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.taskCompletionPercent").isEqualTo(33);
		json.extractingPath("$.taskCounts.total").isEqualTo(3);
		json.extractingPath("$.taskCounts.done").isEqualTo(1);
		json.extractingPath("$.habits").asArray().hasSize(1);
		json.extractingPath("$.habits[0].id").isEqualTo(7);
		json.extractingPath("$.habits[0].name").isEqualTo("Read");
		json.extractingPath("$.habits[0].frequency").isEqualTo("DAILY");
		json.extractingPath("$.habits[0].completedToday").isEqualTo(true);
		json.extractingPath("$.habits[0].doneForCurrentPeriod").isEqualTo(true);
		json.extractingPath("$.habits[0].currentStreak").isEqualTo(3);
		json.extractingPath("$.habitCounts.active").isEqualTo(1);
		json.extractingPath("$.habitCounts.completedToday").isEqualTo(1);
		json.extractingPath("$.activePlans").asArray().hasSize(1);
		json.extractingPath("$.activePlans[0].id").isEqualTo(5);
		json.extractingPath("$.activePlans[0].title").isEqualTo("Weekly focus");
		json.extractingPath("$.activePlans[0].status").isEqualTo("IN_PROGRESS");
		json.extractingPath("$.activePlans[0].items").asArray().hasSize(2);
		json.extractingPath("$.activePlans[0].doneItems").isEqualTo(1);
		json.extractingPath("$.activePlans[0].totalItems").isEqualTo(2);
		json.extractingPath("$.activePlans[0].progressPercent").isEqualTo(50);
		json.extractingPath("$.activePlans[0].restSeconds").isEqualTo(7200);
		json.extractingPath("$.planCounts.notStarted").isEqualTo(2);
		json.extractingPath("$.planCounts.inProgress").isEqualTo(1);
		json.extractingPath("$.planCounts.completed").isEqualTo(4);
		json.extractingPath("$.learning.notStarted").isEqualTo(1);
		json.extractingPath("$.learning.inProgress").isEqualTo(2);
		json.extractingPath("$.learning.completed").isEqualTo(3);
		json.extractingPath("$.learning.milestonesDone").isEqualTo(4);
		json.extractingPath("$.learning.milestonesTotal").isEqualTo(9);
	}

	@Test
	@DisplayName("US5: an empty dashboard is zeros and empty arrays")
	void getDashboardEmptyIsZerosAndEmptyArrays() {
		given(this.dashboardService.summary()).willReturn(emptySummary());

		var result = assertThat(this.mvc.get().uri("/api/dashboard"));
		result.hasStatus(200).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.dueToday").asArray().isEmpty();
		json.extractingPath("$.overdue").asArray().isEmpty();
		json.extractingPath("$.completedToday").asArray().isEmpty();
		json.extractingPath("$.habits").asArray().isEmpty();
		json.extractingPath("$.activePlans").asArray().isEmpty();
		json.extractingPath("$.taskCompletionPercent").isEqualTo(0);
		json.extractingPath("$.taskCounts.total").isEqualTo(0);
		json.extractingPath("$.taskCounts.done").isEqualTo(0);
		json.extractingPath("$.habitCounts.active").isEqualTo(0);
		json.extractingPath("$.habitCounts.completedToday").isEqualTo(0);
		json.extractingPath("$.planCounts.notStarted").isEqualTo(0);
		json.extractingPath("$.planCounts.inProgress").isEqualTo(0);
		json.extractingPath("$.planCounts.completed").isEqualTo(0);
		json.extractingPath("$.learning.notStarted").isEqualTo(0);
		json.extractingPath("$.learning.inProgress").isEqualTo(0);
		json.extractingPath("$.learning.completed").isEqualTo(0);
		json.extractingPath("$.learning.milestonesDone").isEqualTo(0);
		json.extractingPath("$.learning.milestonesTotal").isEqualTo(0);
	}

	@Test
	@DisplayName("US5: dashboard date-times are returned in the app zone")
	void getDashboardDateTimesInAppZone() {
		given(this.dashboardService.summary()).willReturn(fullSummary());

		var result = assertThat(this.mvc.get().uri("/api/dashboard"));
		result.hasStatus(200);
		var json = result.bodyJson();
		json.extractingPath("$.now").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.dueToday[0].createdAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.habits[0].createdAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.activePlans[0].startDateTime").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.activePlans[0].endDateTime").isEqualTo("2026-09-24T14:00:00+03:00");
	}

}
