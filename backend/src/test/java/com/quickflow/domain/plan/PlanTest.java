package com.quickflow.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;

class PlanTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), ZoneId.of("Africa/Cairo"));

	private static final Instant NOW = CLOCK.instant();

	private static final Instant START = NOW.plus(Duration.ofHours(1));

	private static final Instant END = START.plus(Duration.ofHours(2));

	private static final List<PlanItemRef> ITEMS = List.of(
			new PlanItemRef(PlanSourceType.TASK, 1L),
			new PlanItemRef(PlanSourceType.HABIT, 2L));

	private static Plan plan() {
		return new Plan("Deep work", 90, START, END, 1, ITEMS, NOW);
	}

	private static Plan planWithTitle(String title) {
		return new Plan(title, 90, START, END, 1, ITEMS, NOW);
	}

	private static Plan planWithDuration(int duration) {
		return new Plan("Deep work", duration, START, END, 1, ITEMS, NOW);
	}

	private static void assertFieldError(ValidationException ex, String field) {
		assertThat(ex).isNotNull();
		assertThat(ex.getErrors()).extracting(FieldError::field).contains(field);
	}

	@Test
	@DisplayName("BR-10: a plan with no items is rejected with field items")
	void br10_noItemsRejectedWithFieldItems() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Plan("Deep work", 90, START, END, 1, List.of(), NOW));

		assertFieldError(ex, "items");
	}

	@Test
	@DisplayName("BR-10: a plan with null items is rejected with field items")
	void br10_nullItemsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Plan("Deep work", 90, START, END, 1, null, NOW));

		assertFieldError(ex, "items");
	}

	@Test
	@DisplayName("BR-11: an end equal to the start is rejected with field endDateTime")
	void br11_endEqualStartRejectedWithFieldEndDateTime() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Plan("Deep work", 90, START, START, 1, ITEMS, NOW));

		assertFieldError(ex, "endDateTime");
	}

	@Test
	@DisplayName("BR-11: an end before the start is rejected with field endDateTime")
	void br11_endBeforeStartRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Plan("Deep work", 90, START, START.minusSeconds(1), 1, ITEMS, NOW));

		assertFieldError(ex, "endDateTime");
	}

	@Test
	@DisplayName("BR-11: update re-checks that the end is after the start")
	void br11_updateReChecksEndAfterStart() {
		Plan plan = plan();

		ValidationException equal = catchThrowableOfType(ValidationException.class,
				() -> plan.update("Deep work", 90, START, START, 1));
		ValidationException before = catchThrowableOfType(ValidationException.class,
				() -> plan.update("Deep work", 90, END, START, 1));

		assertFieldError(equal, "endDateTime");
		assertFieldError(before, "endDateTime");
		assertThat(plan.getStartDateTime()).isEqualTo(START);
		assertThat(plan.getEndDateTime()).isEqualTo(END);
	}

	@Test
	@DisplayName("FR-07.1: a blank or null title is rejected with field title")
	void fr07_1_blankTitleRejected() {
		ValidationException blank = catchThrowableOfType(ValidationException.class, () -> planWithTitle("   "));
		ValidationException missing = catchThrowableOfType(ValidationException.class, () -> planWithTitle(null));

		assertFieldError(blank, "title");
		assertFieldError(missing, "title");
	}

	@Test
	@DisplayName("FR-07.1: a title of 201 characters is rejected")
	void fr07_1_title201CharsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> planWithTitle("t".repeat(201)));

		assertFieldError(ex, "title");
	}

	@Test
	@DisplayName("FR-07.1: a title of 200 characters is accepted and a title is stored trimmed")
	void fr07_1_title200CharsAccepted() {
		String title = "t".repeat(200);

		assertThat(planWithTitle(title).getTitle()).isEqualTo(title);
		assertThat(planWithTitle("  Deep work  ").getTitle()).isEqualTo("Deep work");
	}

	@Test
	@DisplayName("FR-07.1: an estimated duration of 0 is rejected")
	void fr07_1_durationZeroRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class, () -> planWithDuration(0));

		assertFieldError(ex, "estimatedDurationMinutes");
	}

	@Test
	@DisplayName("FR-07.1: an estimated duration of 100001 is rejected")
	void fr07_1_duration100001Rejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class, () -> planWithDuration(100001));

		assertFieldError(ex, "estimatedDurationMinutes");
	}

	@Test
	@DisplayName("FR-07.1: estimated durations of 1 and 100000 are accepted")
	void fr07_1_durationBoundsAccepted() {
		assertThat(planWithDuration(1).getEstimatedDurationMinutes()).isEqualTo(1);
		assertThat(planWithDuration(100000).getEstimatedDurationMinutes()).isEqualTo(100000);
	}

	@Test
	@DisplayName("FR-07.1: a priority order of 0 is rejected")
	void fr07_1_priorityOrderZeroRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Plan("Deep work", 90, START, END, 0, ITEMS, NOW));

		assertFieldError(ex, "priorityOrder");
	}

	@Test
	@DisplayName("FR-07.1: a new plan stores its fields and has createdAt set to now")
	void fr07_1_createdAtSet() {
		Plan plan = plan();

		assertThat(plan.getCreatedAt()).isEqualTo(NOW);
		assertThat(plan.getTitle()).isEqualTo("Deep work");
		assertThat(plan.getEstimatedDurationMinutes()).isEqualTo(90);
		assertThat(plan.getStartDateTime()).isEqualTo(START);
		assertThat(plan.getEndDateTime()).isEqualTo(END);
		assertThat(plan.getPriorityOrder()).isEqualTo(1);
	}

	@Test
	@DisplayName("FR-07.2: the same source twice is rejected with field items[1]")
	void fr07_2_duplicateSourceRejected() {
		List<PlanItemRef> items = List.of(
				new PlanItemRef(PlanSourceType.TASK, 1L),
				new PlanItemRef(PlanSourceType.TASK, 1L));

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Plan("Deep work", 90, START, END, 1, items, NOW));

		assertFieldError(ex, "items[1]");
		assertThat(ex.getErrors()).extracting(FieldError::field).doesNotContain("items[0]");
	}

	@Test
	@DisplayName("FR-07.2: the same id with a different source type is accepted")
	void fr07_2_sameIdDifferentTypeAccepted() {
		List<PlanItemRef> items = List.of(
				new PlanItemRef(PlanSourceType.TASK, 5L),
				new PlanItemRef(PlanSourceType.HABIT, 5L),
				new PlanItemRef(PlanSourceType.LEARNING_RESOURCE, 5L));

		Plan plan = new Plan("Deep work", 90, START, END, 1, items, NOW);

		assertThat(plan.getItems()).hasSize(3);
	}

	@Test
	@DisplayName("FR-07.2: new items are not done and keep their source type and id in order")
	void fr07_2_newItemsNotDone() {
		Plan plan = plan();

		assertThat(plan.getItems()).extracting(PlanItem::isDone).containsExactly(false, false);
		assertThat(plan.getItems()).extracting(PlanItem::getSourceType)
				.containsExactly(PlanSourceType.TASK, PlanSourceType.HABIT);
		assertThat(plan.getItems()).extracting(PlanItem::getSourceId).containsExactly(1L, 2L);
		assertThat(plan.getItems()).allSatisfy(item -> assertThat(item.getPlan()).isSameAs(plan));
	}

	@Test
	@DisplayName("FR-07.3: update changes the five fields and keeps items, their done flags and createdAt")
	void fr07_3_updateKeepsItems() {
		Plan plan = plan();
		PlanItem first = plan.getItems().get(0);
		PlanItem second = plan.getItems().get(1);
		first.setDone(true);
		Instant newStart = START.plus(Duration.ofDays(1));
		Instant newEnd = newStart.plus(Duration.ofHours(3));

		plan.update("  Focus block  ", 180, newStart, newEnd, 4);

		assertThat(plan.getTitle()).isEqualTo("Focus block");
		assertThat(plan.getEstimatedDurationMinutes()).isEqualTo(180);
		assertThat(plan.getStartDateTime()).isEqualTo(newStart);
		assertThat(plan.getEndDateTime()).isEqualTo(newEnd);
		assertThat(plan.getPriorityOrder()).isEqualTo(4);
		assertThat(plan.getCreatedAt()).isEqualTo(NOW);
		assertThat(plan.getItems()).containsExactly(first, second);
		assertThat(first.isDone()).isTrue();
		assertThat(second.isDone()).isFalse();
	}

	@Test
	@DisplayName("findItem returns the item with that id or empty")
	void findItemByIdReturnsItemOrEmpty() {
		Plan plan = plan();
		PlanItem first = plan.getItems().get(0);
		PlanItem second = plan.getItems().get(1);
		ReflectionTestUtils.setField(first, "id", 7L);
		ReflectionTestUtils.setField(second, "id", 8L);

		assertThat(plan.findItem(7L)).containsSame(first);
		assertThat(plan.findItem(8L)).containsSame(second);
		assertThat(plan.findItem(99L)).isEmpty();
	}

	@Test
	@DisplayName("doneItems and totalItems count the plan's items")
	void doneAndTotalCounts() {
		List<PlanItemRef> items = List.of(
				new PlanItemRef(PlanSourceType.TASK, 1L),
				new PlanItemRef(PlanSourceType.HABIT, 2L),
				new PlanItemRef(PlanSourceType.LEARNING_RESOURCE, 3L));
		Plan plan = new Plan("Deep work", 90, START, END, 1, items, NOW);

		assertThat(plan.doneItems()).isZero();
		assertThat(plan.totalItems()).isEqualTo(3);

		plan.getItems().get(0).setDone(true);
		plan.getItems().get(2).setDone(true);

		assertThat(plan.doneItems()).isEqualTo(2);
		assertThat(plan.totalItems()).isEqualTo(3);

		plan.getItems().get(0).setDone(false);

		assertThat(plan.doneItems()).isEqualTo(1);
	}

}
