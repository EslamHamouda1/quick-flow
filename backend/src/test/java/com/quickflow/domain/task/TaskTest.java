package com.quickflow.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;

class TaskTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), ZoneId.of("Africa/Cairo"));

	private static final Instant NOW = CLOCK.instant();

	private static final LocalDate TODAY = LocalDate.now(CLOCK);

	private static Task task() {
		return new Task("Write report", null, TaskStatus.TODO, TaskPriority.MEDIUM, null, NOW);
	}

	private static Task withDueDate(LocalDate dueDate) {
		return new Task("Write report", null, TaskStatus.TODO, TaskPriority.MEDIUM, dueDate, NOW);
	}

	private static void assertFieldError(ValidationException ex, String field) {
		assertThat(ex).isNotNull();
		assertThat(ex.getErrors()).extracting(FieldError::field).contains(field);
	}

	@Test
	@DisplayName("BR-1, FR-01.5: a blank title is rejected")
	void br1_blankTitleRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Task("   ", null, TaskStatus.TODO, TaskPriority.MEDIUM, null, NOW));

		assertFieldError(ex, "title");
	}

	@Test
	@DisplayName("BR-1, FR-01.5: a title of 201 characters is rejected")
	void br1_title201CharsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Task("a".repeat(201), null, TaskStatus.TODO, TaskPriority.MEDIUM, null, NOW));

		assertFieldError(ex, "title");
	}

	@Test
	@DisplayName("BR-1: a title of 200 characters is accepted")
	void br1_title200CharsAccepted() {
		String title = "a".repeat(200);

		Task task = new Task(title, null, TaskStatus.TODO, TaskPriority.MEDIUM, null, NOW);

		assertThat(task.getTitle()).isEqualTo(title);
	}

	@Test
	@DisplayName("BR-2, FR-01.5: a description of 2001 characters is rejected")
	void br2_description2001CharsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Task("Write report", "d".repeat(2001), TaskStatus.TODO, TaskPriority.MEDIUM, null, NOW));

		assertFieldError(ex, "description");
	}

	@Test
	@DisplayName("BR-2: a null description is accepted")
	void br2_nullDescriptionAccepted() {
		Task task = new Task("Write report", null, TaskStatus.TODO, TaskPriority.MEDIUM, null, NOW);

		assertThat(task.getDescription()).isNull();
	}

	@Test
	@DisplayName("BR-3, FR-01.5: a null status is rejected on update and on status change")
	void br3_nullStatusRejected() {
		Task task = task();

		ValidationException onUpdate = catchThrowableOfType(ValidationException.class,
				() -> task.update("Write report", null, null, TaskPriority.MEDIUM, null, NOW));
		ValidationException onChange = catchThrowableOfType(ValidationException.class,
				() -> task.changeStatus(null, NOW));

		assertFieldError(onUpdate, "status");
		assertFieldError(onChange, "status");
	}

	@Test
	@DisplayName("BR-3: a null priority is rejected on update")
	void br3_nullPriorityRejected() {
		Task task = task();

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.update("Write report", null, TaskStatus.TODO, null, null, NOW));

		assertFieldError(ex, "priority");
	}

	@Test
	@DisplayName("BR-5: completing a task sets status DONE and completedAt")
	void br5_completeSetsDoneAndCompletedAt() {
		Task task = task();

		task.complete(NOW);

		assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
		assertThat(task.getCompletedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("FR-01.4: leaving DONE clears completedAt")
	void fr01_4_leavingDoneClearsCompletedAt() {
		Task task = task();
		task.complete(NOW);

		task.changeStatus(TaskStatus.IN_PROGRESS, NOW);

		assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
		assertThat(task.getCompletedAt()).isNull();
	}

	@Test
	@DisplayName("FR-01.3: a new task defaults to TODO, MEDIUM and not archived")
	void fr01_3_defaultsTodoMediumNotArchived() {
		Task task = new Task("Write report", null, null, null, null, NOW);

		assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
		assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
		assertThat(task.isArchived()).isFalse();
	}

	@Test
	@DisplayName("FR-01.7: an open, unarchived task due yesterday is overdue")
	void fr01_7_overdueWhenDueYesterdayOpenNotArchived() {
		Task task = withDueDate(TODAY.minusDays(1));

		assertThat(task.isOverdue(TODAY)).isTrue();
	}

	@Test
	@DisplayName("FR-01.7: a task due today is not overdue")
	void fr01_7_notOverdueWhenDueToday() {
		Task task = withDueDate(TODAY);

		assertThat(task.isOverdue(TODAY)).isFalse();
	}

	@Test
	@DisplayName("FR-01.7: a DONE task is not overdue")
	void fr01_7_notOverdueWhenDone() {
		Task task = withDueDate(TODAY.minusDays(1));
		task.complete(NOW);

		assertThat(task.isOverdue(TODAY)).isFalse();
	}

	@Test
	@DisplayName("FR-01.7: an archived task is not overdue")
	void fr01_7_notOverdueWhenArchived() {
		Task task = withDueDate(TODAY.minusDays(1));
		task.archive(NOW);

		assertThat(task.isOverdue(TODAY)).isFalse();
	}

	@Test
	@DisplayName("FR-01.7: a task without a due date is not overdue")
	void fr01_7_notOverdueWithoutDueDate() {
		Task task = withDueDate(null);

		assertThat(task.isOverdue(TODAY)).isFalse();
	}

}
