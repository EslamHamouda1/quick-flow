package com.quickflow.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;

class TaskTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), ZoneId.of("Africa/Cairo"));

	private static final Instant NOW = CLOCK.instant();

	private static final LocalDate TODAY = LocalDate.now(CLOCK);

	private static final Instant LATER = NOW.plusSeconds(3600);

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

	private static void assertFieldError(ValidationException ex, String field, String message) {
		assertThat(ex).isNotNull();
		assertThat(ex.getErrors()).contains(new FieldError(field, message));
	}

	private static Task tagged(String... tags) {
		Task task = task();
		task.addTags(List.of(tags), NOW);
		return task;
	}

	private static List<String> tenTags() {
		return IntStream.rangeClosed(1, Task.TAGS_MAX).mapToObj(i -> String.format("tag%02d", i)).toList();
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

	@Test
	@DisplayName("FR-001, FR-002: added tags are all kept and listed alphabetically")
	void fr001_addTagsAddsAllSortedAlphabetically() {
		Task task = task();

		task.addTags(List.of("work", "urgent"), NOW);

		assertThat(task.getTags()).containsExactly("urgent", "work");
	}

	@Test
	@DisplayName("FR-002: the tag list cannot be modified from outside")
	void fr002_tagsAreUnmodifiable() {
		Task task = tagged("work");

		assertThatThrownBy(() -> task.getTags().add("urgent")).isInstanceOf(UnsupportedOperationException.class);
		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T1: a tag is trimmed")
	void brT1_tagIsTrimmed() {
		Task task = task();

		task.addTags(List.of("  work  "), NOW);

		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T1, FR-006: an empty tag is rejected and the tags are unchanged")
	void brT1_emptyTagRejected() {
		Task task = tagged("work");

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.addTags(List.of(""), LATER));

		assertFieldError(ex, "tags", "tag must not be blank");
		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T1, FR-006: a blank tag is rejected and the tags are unchanged")
	void brT1_blankTagRejected() {
		Task task = tagged("work");

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.addTags(List.of("   "), LATER));

		assertFieldError(ex, "tags", "tag must not be blank");
		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T1, FR-006: a null tag is rejected and the tags are unchanged")
	void brT1_nullTagRejected() {
		Task task = tagged("work");

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.addTags(Arrays.asList((String) null), LATER));

		assertFieldError(ex, "tags", "tag must not be blank");
		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T1, FR-006: a tag of 31 characters is rejected and the tags are unchanged")
	void brT1_tag31CharsRejected() {
		Task task = tagged("work");

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.addTags(List.of("a".repeat(Task.TAG_MAX + 1)), LATER));

		assertFieldError(ex, "tags", "must be at most 30 characters");
		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T1: a tag of 30 characters is accepted")
	void brT1_tag30CharsAccepted() {
		String tag = "a".repeat(Task.TAG_MAX);
		Task task = task();

		task.addTags(List.of(tag), NOW);

		assertThat(task.getTags()).containsExactly(tag);
	}

	@Test
	@DisplayName("BR-T1: a batch with one invalid tag adds none of its tags")
	void brT1_batchWithOneInvalidTagAddsNone() {
		Task task = tagged("work");

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.addTags(List.of("urgent", "   ", "home"), LATER));

		assertFieldError(ex, "tags", "tag must not be blank");
		assertThat(task.getTags()).containsExactly("work");
		assertThat(task.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("BR-T2: a tag is stored in lower case")
	void brT2_tagStoredLowerCase() {
		Task task = task();

		task.addTags(List.of("  Work "), NOW);

		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T2: adding an existing tag in another case keeps one tag and does not change updatedAt")
	void brT2_addingSameTagOtherCaseKeepsOne() {
		Task task = tagged("work");

		task.addTags(List.of("Work"), LATER);

		assertThat(task.getTags()).containsExactly("work");
		assertThat(task.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("BR-T2: the same tag twice in one call adds one tag")
	void brT2_sameTagTwiceInOneCallAddsOne() {
		Task task = task();

		task.addTags(List.of("work", "WORK"), NOW);

		assertThat(task.getTags()).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T2: removing a tag ignores case")
	void brT2_removeTagIgnoresCase() {
		Task task = tagged("work", "urgent");

		task.removeTag("  WORK ", LATER);

		assertThat(task.getTags()).containsExactly("urgent");
	}

	@Test
	@DisplayName("BR-T3, FR-007: an 11th distinct tag is rejected and the 10 tags are kept")
	void brT3_eleventhTagRejected() {
		Task task = task();
		task.addTags(tenTags(), NOW);

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.addTags(List.of("extra"), LATER));

		assertFieldError(ex, "tags", "must have at most 10 tags");
		assertThat(task.getTags()).containsExactlyElementsOf(tenTags());
		assertThat(task.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("BR-T3, FR-007: adding an existing tag to a task with 10 tags is accepted")
	void brT3_existingTagOnFullTaskAccepted() {
		Task task = task();
		task.addTags(tenTags(), NOW);

		task.addTags(List.of("TAG01"), LATER);

		assertThat(task.getTags()).containsExactlyElementsOf(tenTags());
		assertThat(task.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("BR-T3: more than 10 distinct tags in one call are rejected and none are added")
	void brT3_moreThanTenInOneCallRejected() {
		Task task = task();
		List<String> eleven = new ArrayList<>(tenTags());
		eleven.add("extra");

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> task.addTags(eleven, NOW));

		assertFieldError(ex, "tags", "must have at most 10 tags");
		assertThat(task.getTags()).isEmpty();
	}

	@Test
	@DisplayName("BR-T4: update keeps the tags")
	void brT4_updateKeepsTags() {
		Task task = tagged("work", "urgent");

		task.update("New title", "New description", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY, LATER);

		assertThat(task.getTags()).containsExactly("urgent", "work");
	}

	@Test
	@DisplayName("BR-T4: a status change keeps the tags")
	void brT4_changeStatusKeepsTags() {
		Task task = tagged("work", "urgent");

		task.changeStatus(TaskStatus.IN_PROGRESS, LATER);

		assertThat(task.getTags()).containsExactly("urgent", "work");
	}

	@Test
	@DisplayName("BR-T4: completing keeps the tags")
	void brT4_completeKeepsTags() {
		Task task = tagged("work", "urgent");

		task.complete(LATER);

		assertThat(task.getTags()).containsExactly("urgent", "work");
	}

	@Test
	@DisplayName("BR-T4: archiving keeps the tags")
	void brT4_archiveKeepsTags() {
		Task task = tagged("work", "urgent");

		task.archive(LATER);

		assertThat(task.getTags()).containsExactly("urgent", "work");
	}

	@Test
	@DisplayName("BR-T4: restoring keeps the tags")
	void brT4_restoreKeepsTags() {
		Task task = tagged("work", "urgent");
		task.archive(LATER);

		task.restore(LATER);

		assertThat(task.getTags()).containsExactly("urgent", "work");
	}

	@Test
	@DisplayName("FR-005: removing a tag removes it and sets updatedAt")
	void fr005_removeTagRemovesAndSetsUpdatedAt() {
		Task task = tagged("work", "urgent");

		task.removeTag("WORK", LATER);

		assertThat(task.getTags()).containsExactly("urgent");
		assertThat(task.getUpdatedAt()).isEqualTo(LATER);
	}

	@Test
	@DisplayName("FR-005: removing an absent tag changes nothing")
	void fr005_removeAbsentTagIsNoOp() {
		Task task = tagged("work");

		task.removeTag("home", LATER);

		assertThat(task.getTags()).containsExactly("work");
		assertThat(task.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("FR-005: a blank or null value to remove is rejected on tag")
	void fr005_blankRemoveValueRejectedOnTag() {
		Task task = tagged("work");

		ValidationException blank = catchThrowableOfType(ValidationException.class,
				() -> task.removeTag("   ", LATER));
		ValidationException none = catchThrowableOfType(ValidationException.class,
				() -> task.removeTag(null, LATER));

		assertFieldError(blank, "tag", "must not be blank");
		assertFieldError(none, "tag", "must not be blank");
		assertThat(task.getTags()).containsExactly("work");
		assertThat(task.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("FR-001: adding a new tag sets updatedAt")
	void fr001_addingNewTagSetsUpdatedAt() {
		Task task = tagged("work");

		task.addTags(List.of("urgent"), LATER);

		assertThat(task.getUpdatedAt()).isEqualTo(LATER);
	}

}
