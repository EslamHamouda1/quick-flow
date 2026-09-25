package com.quickflow.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.common.ValidationException;
import com.quickflow.domain.task.TaskService;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-09-20T08:00:00Z");
	private static final Instant START = NOW.minus(Duration.ofHours(1));
	private static final Instant END = NOW.plus(Duration.ofHours(2));
	private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

	private static final PlanItemRef TASK_1 = new PlanItemRef(PlanSourceType.TASK, 1L);
	private static final PlanItemRef HABIT_2 = new PlanItemRef(PlanSourceType.HABIT, 2L);
	private static final PlanItemRef CARD_3 = new PlanItemRef(PlanSourceType.LEARNING_RESOURCE, 3L);

	@Mock
	private PlanRepository plans;

	@Mock
	private PlanSourceResolver sources;

	@Mock
	private TaskService taskService;

	private PlanService service;

	@BeforeEach
	void setUp() {
		service = new PlanService(plans, sources, taskService, new TimeService(Clock.fixed(NOW, CAIRO)));
	}

	/** Sets the plan id and item ids (planId * 10 + 1, + 2, ...). */
	private static void assignIds(Plan plan, long id) {
		ReflectionTestUtils.setField(plan, "id", id);
		List<PlanItem> items = plan.getItems();
		for (int i = 0; i < items.size(); i++) {
			ReflectionTestUtils.setField(items.get(i), "id", id * 10 + i + 1);
		}
	}

	private static Plan existing(long id, Instant start, Instant end, int priority, PlanItemRef... refs) {
		Plan plan = new Plan("Plan " + id, 60, start, end, priority, List.of(refs), EARLIER);
		assignIds(plan, id);
		for (PlanItem item : plan.getItems()) {
			item.refreshTitle("Stored " + item.getSourceType() + " " + item.getSourceId());
		}
		return plan;
	}

	private static Plan existing(long id, PlanItemRef... refs) {
		return existing(id, START, END, 1, refs);
	}

	private void allSourcesExist() {
		lenient().when(sources.title(any(PlanSourceType.class), anyLong())).thenReturn(Optional.of("Current"));
	}

	private void title(PlanSourceType type, long id, String title) {
		lenient().when(sources.title(type, id)).thenReturn(Optional.ofNullable(title));
	}

	private void saveAssignsIds() {
		lenient().when(plans.save(any(Plan.class))).thenAnswer(inv -> {
			Plan plan = inv.getArgument(0);
			if (plan.getId() == null) {
				assignIds(plan, 1L);
			}
			return plan;
		});
	}

	private static PlanView.Item viewItem(PlanView view, long itemId) {
		return view.items().stream().filter(i -> i.item().getId() == itemId).findFirst().orElseThrow();
	}

	private static List<Long> ids(List<PlanView> views) {
		return views.stream().map(v -> v.plan().getId()).toList();
	}

	@Test
	@DisplayName("BR-10: an unknown or deleted source is rejected with the item's field and nothing is saved")
	void br10_unknownSourceRejectedWithItemField() {
		title(PlanSourceType.TASK, 1L, "Write report");
		title(PlanSourceType.HABIT, 2L, null);
		title(PlanSourceType.LEARNING_RESOURCE, 3L, "Learn Rust");

		assertThatThrownBy(() -> service.create("Morning", 60, START, END, 1, List.of(TASK_1, HABIT_2, CARD_3)))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("items[1].sourceId"));
		verify(plans, never()).save(any(Plan.class));
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("BR-10: each source type is resolved through the source resolver")
	void br10_eachSourceTypeResolved() {
		title(PlanSourceType.TASK, 1L, "Write report");
		title(PlanSourceType.HABIT, 2L, "Run");
		title(PlanSourceType.LEARNING_RESOURCE, 3L, "Learn Rust");
		saveAssignsIds();

		service.create("Morning", 60, START, END, 1, List.of(TASK_1, HABIT_2, CARD_3));

		verify(sources, atLeastOnce()).title(PlanSourceType.TASK, 1L);
		verify(sources, atLeastOnce()).title(PlanSourceType.HABIT, 2L);
		verify(sources, atLeastOnce()).title(PlanSourceType.LEARNING_RESOURCE, 3L);
	}

	@Test
	@DisplayName("FR-07.1: create sets createdAt from the clock and saves the plan")
	void fr07_1_createSetsCreatedAtFromClock() {
		allSourcesExist();
		saveAssignsIds();

		PlanView view = service.create("Morning", 90, START, END, 2, List.of(TASK_1, HABIT_2));

		Plan plan = view.plan();
		assertThat(plan.getCreatedAt()).isEqualTo(NOW);
		assertThat(plan.getTitle()).isEqualTo("Morning");
		assertThat(plan.getEstimatedDurationMinutes()).isEqualTo(90);
		assertThat(plan.getStartDateTime()).isEqualTo(START);
		assertThat(plan.getEndDateTime()).isEqualTo(END);
		assertThat(plan.getPriorityOrder()).isEqualTo(2);
		assertThat(plan.getItems()).hasSize(2).allSatisfy(item -> assertThat(item.isDone()).isFalse());
		assertThat(view.progress().doneItems()).isZero();
		assertThat(view.progress().totalItems()).isEqualTo(2);
		verify(plans).save(plan);
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("FR-07.2: create stores each source's current title as a snapshot")
	void fr07_2_sourceTitleSnapshotOnCreate() {
		title(PlanSourceType.TASK, 1L, "Write report");
		title(PlanSourceType.HABIT, 2L, "Run");
		title(PlanSourceType.LEARNING_RESOURCE, 3L, "Learn Rust");
		saveAssignsIds();

		PlanView view = service.create("Morning", 60, START, END, 1, List.of(TASK_1, HABIT_2, CARD_3));

		assertThat(view.plan().getItems()).extracting(PlanItem::getSourceTitle)
				.containsExactly("Write report", "Run", "Learn Rust");
		assertThat(view.plan().getItems()).extracting(PlanItem::getSourceType)
				.containsExactly(PlanSourceType.TASK, PlanSourceType.HABIT, PlanSourceType.LEARNING_RESOURCE);
		assertThat(view.items()).extracting(PlanView.Item::sourceTitle)
				.containsExactly("Write report", "Run", "Learn Rust");
		assertThat(view.items()).extracting(PlanView.Item::sourceRemoved).containsExactly(false, false, false);
	}

	@Test
	@DisplayName("BR-13: ticking a TASK item completes the task")
	void br13_taskItemDoneCompletesTask() {
		Plan plan = existing(7L, TASK_1, HABIT_2);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		allSourcesExist();
		saveAssignsIds();

		service.setItemDone(7L, 71L, true);

		assertThat(plan.findItem(71L)).get().extracting(PlanItem::isDone).isEqualTo(true);
		verify(taskService).complete(1L);
	}

	@Test
	@DisplayName("BR-13: un-ticking a TASK item leaves the task unchanged")
	void br13_taskItemUndoneLeavesTask() {
		Plan plan = existing(7L, TASK_1, HABIT_2);
		plan.findItem(71L).orElseThrow().setDone(true);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		allSourcesExist();
		saveAssignsIds();

		service.setItemDone(7L, 71L, false);

		assertThat(plan.findItem(71L)).get().extracting(PlanItem::isDone).isEqualTo(false);
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("BR-13: ticking a HABIT item changes no source")
	void br13_habitItemDoneChangesNoSource() {
		Plan plan = existing(7L, TASK_1, HABIT_2);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		allSourcesExist();
		saveAssignsIds();

		service.setItemDone(7L, 72L, true);

		assertThat(plan.findItem(72L)).get().extracting(PlanItem::isDone).isEqualTo(true);
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("BR-13: ticking a LEARNING_RESOURCE item changes no source")
	void br13_learningItemDoneChangesNoSource() {
		Plan plan = existing(7L, TASK_1, CARD_3);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		allSourcesExist();
		saveAssignsIds();

		service.setItemDone(7L, 72L, true);

		assertThat(plan.findItem(72L)).get().extracting(PlanItem::isDone).isEqualTo(true);
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("BR-13: ticking a TASK item whose task was deleted saves the flag and completes nothing")
	void br13_taskItemOfDeletedTaskDoneNoPropagation() {
		Plan plan = existing(7L, TASK_1, HABIT_2);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		allSourcesExist();
		title(PlanSourceType.TASK, 1L, null);
		saveAssignsIds();

		PlanView view = service.setItemDone(7L, 71L, true);

		assertThat(plan.findItem(71L)).get().extracting(PlanItem::isDone).isEqualTo(true);
		assertThat(view.progress().doneItems()).isEqualTo(1);
		verify(taskService, never()).complete(anyLong());
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("FR-08.5: setItemDone returns the view with the new progress")
	void fr08_5_setItemDoneReturnsNewProgress() {
		Plan plan = existing(7L, TASK_1, HABIT_2, CARD_3);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		allSourcesExist();
		saveAssignsIds();

		PlanView view = service.setItemDone(7L, 72L, true);

		assertThat(view.plan()).isSameAs(plan);
		assertThat(view.progress().doneItems()).isEqualTo(1);
		assertThat(view.progress().totalItems()).isEqualTo(3);
		assertThat(view.progress().progressPercent()).isEqualTo(33);
		assertThat(view.progress().status()).isEqualTo(PlanStatus.IN_PROGRESS);
		assertThat(viewItem(view, 72L).item().isDone()).isTrue();
	}

	@Test
	@DisplayName("FR-07.6: an item whose source was deleted is marked removed, keeps its stored title and still counts")
	void fr07_6_deletedSourceMarkedRemovedKeepsTitleAndCounts() {
		Plan plan = existing(7L, TASK_1, HABIT_2);
		plan.findItem(72L).orElseThrow().refreshTitle("Old run");
		plan.findItem(72L).orElseThrow().setDone(true);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		title(PlanSourceType.TASK, 1L, "Write report");
		title(PlanSourceType.HABIT, 2L, null);

		PlanView view = service.get(7L);

		PlanView.Item removed = viewItem(view, 72L);
		assertThat(removed.sourceRemoved()).isTrue();
		assertThat(removed.sourceTitle()).isEqualTo("Old run");
		assertThat(removed.item().getSourceTitle()).isEqualTo("Old run");
		assertThat(viewItem(view, 71L).sourceRemoved()).isFalse();
		assertThat(view.items()).hasSize(2);
		assertThat(view.progress().totalItems()).isEqualTo(2);
		assertThat(view.progress().doneItems()).isEqualTo(1);
		assertThat(view.progress().progressPercent()).isEqualTo(50);
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("FR-07.6: an existing source's current (renamed) title is shown")
	void fr07_6_existingSourceTitleRefreshed() {
		Plan plan = existing(7L, TASK_1);
		plan.findItem(71L).orElseThrow().refreshTitle("Old report");
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		title(PlanSourceType.TASK, 1L, "Renamed report");

		PlanView view = service.get(7L);

		PlanView.Item item = viewItem(view, 71L);
		assertThat(item.sourceRemoved()).isFalse();
		assertThat(item.sourceTitle()).isEqualTo("Renamed report");
	}

	@Test
	@DisplayName("BR-14: deleting a plan removes only the plan and touches no source")
	void br14_deletePlanTouchesNoSource() {
		Plan plan = existing(7L, TASK_1, HABIT_2, CARD_3);
		given(plans.findById(7L)).willReturn(Optional.of(plan));

		service.delete(7L);

		verify(plans).delete(plan);
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("FR-07.3: update edits the plan's fields and keeps items and createdAt")
	void fr07_3_updateEditsFields() {
		Plan plan = existing(7L, TASK_1, HABIT_2);
		plan.findItem(71L).orElseThrow().setDone(true);
		given(plans.findById(7L)).willReturn(Optional.of(plan));
		allSourcesExist();
		saveAssignsIds();
		Instant newStart = NOW.plus(Duration.ofDays(1));
		Instant newEnd = newStart.plus(Duration.ofHours(3));

		PlanView view = service.update(7L, "Evening", 120, newStart, newEnd, 4);

		assertThat(view.plan()).isSameAs(plan);
		assertThat(plan.getTitle()).isEqualTo("Evening");
		assertThat(plan.getEstimatedDurationMinutes()).isEqualTo(120);
		assertThat(plan.getStartDateTime()).isEqualTo(newStart);
		assertThat(plan.getEndDateTime()).isEqualTo(newEnd);
		assertThat(plan.getPriorityOrder()).isEqualTo(4);
		assertThat(plan.getCreatedAt()).isEqualTo(EARLIER);
		assertThat(plan.getItems()).extracting(PlanItem::getId).containsExactly(71L, 72L);
		assertThat(plan.getItems()).extracting(PlanItem::isDone).containsExactly(true, false);
		assertThat(view.progress().status()).isEqualTo(PlanStatus.NOT_STARTED);
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("FR-07.3: update with end not after start is rejected on endDateTime")
	void fr07_3_updateEndBeforeStartRejected() {
		Plan plan = existing(7L, TASK_1);
		given(plans.findById(7L)).willReturn(Optional.of(plan));

		assertThatThrownBy(() -> service.update(7L, "Evening", 120, END, START, 1))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("endDateTime"));
		assertThatThrownBy(() -> service.update(7L, "Evening", 120, START, START, 1))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("endDateTime"));
		verify(plans, never()).save(any(Plan.class));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "get", "update", "delete", "setItemDone" })
	@DisplayName("An unknown plan id throws NotFoundException")
	void unknownPlanNotFound(String operation) {
		given(plans.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> {
			switch (operation) {
				case "get" -> service.get(99L);
				case "update" -> service.update(99L, "Evening", 60, START, END, 1);
				case "delete" -> service.delete(99L);
				case "setItemDone" -> service.setItemDone(99L, 991L, true);
				default -> throw new IllegalArgumentException(operation);
			}
		}).isInstanceOf(NotFoundException.class).hasMessage("Plan 99 not found");
		verify(plans, never()).delete(any(Plan.class));
		verify(plans, never()).save(any(Plan.class));
		verifyNoInteractions(taskService);
	}

	@Test
	@DisplayName("An item id of another plan throws NotFoundException and changes nothing")
	void itemOfOtherPlanNotFound() {
		Plan plan = existing(7L, HABIT_2);
		Plan other = existing(8L, TASK_1);
		given(plans.findById(7L)).willReturn(Optional.of(plan));

		assertThatThrownBy(() -> service.setItemDone(7L, 81L, true))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Plan item 81 not found on plan 7");
		assertThat(other.findItem(81L)).get().extracting(PlanItem::isDone).isEqualTo(false);
		assertThat(plan.findItem(71L)).get().extracting(PlanItem::isDone).isEqualTo(false);
		verify(plans, never()).save(any(Plan.class));
		verifyNoInteractions(taskService);
	}

	/** Active: 1 (p2, in progress), 2 (p1, start later), 3 (p1, start earlier), 4 (p1, not started).
		Completed: 5 (ended 1 day ago), 6 (ended 2 days ago), 7 (all items done, ends in 5 days). */
	private void stubMixedPlans() {
		Instant earlyStart = NOW.minus(Duration.ofHours(3));
		Plan p1 = existing(1L, earlyStart, END, 2, TASK_1);
		Plan p2 = existing(2L, START, END, 1, TASK_1);
		Plan p3 = existing(3L, earlyStart, END, 1, TASK_1);
		Plan p4 = existing(4L, NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(4)), 1, TASK_1);
		Plan p5 = existing(5L, NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofDays(1)), 1, TASK_1);
		Plan p6 = existing(6L, NOW.minus(Duration.ofDays(4)), NOW.minus(Duration.ofDays(2)), 1, TASK_1);
		Plan p7 = existing(7L, START, NOW.plus(Duration.ofDays(5)), 1, TASK_1);
		p7.findItem(71L).orElseThrow().setDone(true);
		given(plans.findAll()).willReturn(List.of(p6, p1, p7, p4, p2, p5, p3));
		allSourcesExist();
	}

	@Test
	@DisplayName("List active: NOT_STARTED and IN_PROGRESS plans by priorityOrder then startDateTime")
	void listActiveOrderedByPriorityThenStart() {
		stubMixedPlans();

		List<PlanView> result = service.list(PlanGroup.ACTIVE);

		assertThat(ids(result)).containsExactly(3L, 2L, 4L, 1L);
		assertThat(result).extracting(v -> v.progress().status())
				.containsExactly(PlanStatus.IN_PROGRESS, PlanStatus.IN_PROGRESS, PlanStatus.NOT_STARTED,
						PlanStatus.IN_PROGRESS);
	}

	@Test
	@DisplayName("List completed: completed plans by endDateTime descending")
	void listCompletedOrderedByEndDesc() {
		stubMixedPlans();

		List<PlanView> result = service.list(PlanGroup.COMPLETED);

		assertThat(ids(result)).containsExactly(7L, 5L, 6L);
		assertThat(result).extracting(v -> v.progress().status()).containsOnly(PlanStatus.COMPLETED);
	}

	@Test
	@DisplayName("List all: active plans first, then completed plans")
	void listAllActiveThenCompleted() {
		stubMixedPlans();

		List<PlanView> result = service.list(PlanGroup.ALL);

		assertThat(ids(result)).containsExactly(3L, 2L, 4L, 1L, 7L, 5L, 6L);
	}

	@Test
	@DisplayName("The group parameter accepts active, completed and all; anything else is rejected on group")
	void groupParamParsedAndUnknownRejected() {
		assertThat(PlanGroup.fromParam("active")).isEqualTo(PlanGroup.ACTIVE);
		assertThat(PlanGroup.fromParam("completed")).isEqualTo(PlanGroup.COMPLETED);
		assertThat(PlanGroup.fromParam("all")).isEqualTo(PlanGroup.ALL);

		for (String bad : new String[] { "done", "ACTIVE", "", null }) {
			assertThatThrownBy(() -> PlanGroup.fromParam(bad))
					.as("group=%s", bad)
					.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
							.extracting(FieldError::field).containsExactly("group"));
		}
	}

}
