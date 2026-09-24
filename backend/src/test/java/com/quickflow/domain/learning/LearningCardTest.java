package com.quickflow.domain.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;

class LearningCardTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), ZoneId.of("Africa/Cairo"));

	private static final Instant NOW = CLOCK.instant();

	private static final LocalDate TODAY = LocalDate.now(CLOCK);

	private static LearningCard card() {
		return new LearningCard("Spring Boot", null, NOW);
	}

	private static void assertFieldError(ValidationException ex, String field) {
		assertThat(ex).isNotNull();
		assertThat(ex.getErrors()).extracting(FieldError::field).contains(field);
	}

	@Test
	@DisplayName("BR-8: a blank title is rejected with field title")
	void br8_blankTitleRejectedWithFieldTitle() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new LearningCard("   ", null, NOW));

		assertFieldError(ex, "title");
	}

	@Test
	@DisplayName("BR-8: a null title is rejected")
	void br8_nullTitleRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new LearningCard(null, null, NOW));

		assertFieldError(ex, "title");
	}

	@Test
	@DisplayName("FR-05.2: a title is stored trimmed")
	void fr05_2_titleTrimmed() {
		LearningCard card = new LearningCard("  Spring Boot  ", null, NOW);

		assertThat(card.getTitle()).isEqualTo("Spring Boot");
	}

	@Test
	@DisplayName("FR-05.2: a title of 201 characters is rejected")
	void fr05_2_title201CharsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new LearningCard("t".repeat(201), null, NOW));

		assertFieldError(ex, "title");
	}

	@Test
	@DisplayName("FR-05.2: a title of 200 characters is accepted")
	void fr05_2_title200CharsAccepted() {
		String title = "t".repeat(200);

		LearningCard card = new LearningCard(title, null, NOW);

		assertThat(card.getTitle()).isEqualTo(title);
	}

	@Test
	@DisplayName("FR-05.1: a description of 2001 characters is rejected")
	void fr05_1_description2001CharsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new LearningCard("Spring Boot", "d".repeat(2001), NOW));

		assertFieldError(ex, "description");
	}

	@Test
	@DisplayName("FR-05.1: a description of 2000 characters is accepted as given")
	void fr05_1_description2000CharsAccepted() {
		String description = "d".repeat(2000);

		LearningCard card = new LearningCard("Spring Boot", description, NOW);

		assertThat(card.getDescription()).isEqualTo(description);
	}

	@Test
	@DisplayName("FR-05.1: a null description is accepted")
	void fr05_1_nullDescriptionAccepted() {
		LearningCard card = new LearningCard("Spring Boot", null, NOW);

		assertThat(card.getDescription()).isNull();
	}

	@Test
	@DisplayName("FR-05.1: an empty description is stored as null")
	void fr05_1_emptyDescriptionStoredAsNull() {
		LearningCard card = new LearningCard("Spring Boot", "", NOW);

		assertThat(card.getDescription()).isNull();
	}

	@Test
	@DisplayName("FR-05.1: a new card is NOT_STARTED and has createdAt set")
	void fr05_1_newCardNotStartedWithCreatedAt() {
		LearningCard card = card();

		assertThat(card.getStatus()).isEqualTo(LearningStatus.NOT_STARTED);
		assertThat(card.getCreatedAt()).isEqualTo(NOW);
		assertThat(card.getMilestones()).isEmpty();
		assertThat(card.getNotes()).isEmpty();
		assertThat(card.milestonesDone()).isZero();
		assertThat(card.milestonesTotal()).isZero();
	}

	@Test
	@DisplayName("FR-05.2: a null status is rejected on update")
	void fr05_2_nullStatusRejectedOnUpdate() {
		LearningCard card = card();

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> card.update("Spring Boot", null, null));

		assertFieldError(ex, "status");
	}

	@Test
	@DisplayName("FR-05.2: update changes title, description and status but not createdAt, milestones or notes")
	void fr05_2_updateChangesTitleDescriptionStatus() {
		LearningCard card = card();
		card.addMilestone("Read the docs", null);
		card.addNote("Starter guide", NOW);

		card.update("  Spring Boot 4  ", "Deep dive", LearningStatus.IN_PROGRESS);

		assertThat(card.getTitle()).isEqualTo("Spring Boot 4");
		assertThat(card.getDescription()).isEqualTo("Deep dive");
		assertThat(card.getStatus()).isEqualTo(LearningStatus.IN_PROGRESS);
		assertThat(card.getCreatedAt()).isEqualTo(NOW);
		assertThat(card.getMilestones()).hasSize(1);
		assertThat(card.getNotes()).hasSize(1);

		card.update("Spring Boot 4", "", LearningStatus.COMPLETED);
		assertThat(card.getDescription()).isNull();
		assertThat(card.getStatus()).isEqualTo(LearningStatus.COMPLETED);

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> card.update("  ", null, LearningStatus.NOT_STARTED));

		assertFieldError(ex, "title");
	}

	@Test
	@DisplayName("FR-06.1: a blank or null milestone title is rejected")
	void fr06_1_milestoneBlankTitleRejected() {
		LearningCard card = card();

		ValidationException blank = catchThrowableOfType(ValidationException.class,
				() -> card.addMilestone("   ", null));
		ValidationException missing = catchThrowableOfType(ValidationException.class,
				() -> new LearningMilestone(card, null, null));

		assertFieldError(blank, "title");
		assertFieldError(missing, "title");
		assertThat(card.getMilestones()).isEmpty();
	}

	@Test
	@DisplayName("FR-06.1: a milestone title of 201 characters is rejected, 200 is accepted")
	void fr06_1_milestoneTitle201CharsRejected() {
		LearningCard card = card();

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> card.addMilestone("m".repeat(201), null));

		assertFieldError(ex, "title");
		assertThat(card.addMilestone("m".repeat(200), null).getTitle()).isEqualTo("m".repeat(200));
	}

	@Test
	@DisplayName("FR-06.1: a new milestone is not done, has a trimmed title and belongs to the card")
	void fr06_1_newMilestoneNotDone() {
		LearningCard card = card();

		LearningMilestone milestone = card.addMilestone("  Read the docs  ", TODAY);

		assertThat(milestone.isDone()).isFalse();
		assertThat(milestone.getTitle()).isEqualTo("Read the docs");
		assertThat(milestone.getTargetDate()).isEqualTo(TODAY);
		assertThat(milestone.getCard()).isSameAs(card);
		assertThat(card.getMilestones()).containsExactly(milestone);
	}

	@Test
	@DisplayName("FR-06.1: a milestone target date is optional and may be in the past")
	void fr06_1_milestoneTargetDateOptional() {
		LearningCard card = card();

		LearningMilestone noDate = card.addMilestone("No date", null);
		LearningMilestone pastDate = card.addMilestone("Past date", TODAY.minusDays(10));

		assertThat(noDate.getTargetDate()).isNull();
		assertThat(pastDate.getTargetDate()).isEqualTo(TODAY.minusDays(10));
	}

	@Test
	@DisplayName("FR-06.1: removeMilestone removes it from the card")
	void fr06_1_removeMilestoneRemovesFromList() {
		LearningCard card = card();
		LearningMilestone first = card.addMilestone("First", null);
		LearningMilestone second = card.addMilestone("Second", null);

		card.removeMilestone(first);

		assertThat(card.getMilestones()).containsExactly(second);
		assertThat(card.milestonesTotal()).isEqualTo(1);
	}

	@Test
	@DisplayName("FR-06.1: getMilestones and getNotes are unmodifiable")
	void fr06_1_milestonesAndNotesUnmodifiable() {
		LearningCard card = card();
		LearningMilestone milestone = card.addMilestone("First", null);
		LearningNote note = card.addNote("A note", NOW);

		assertThatThrownBy(() -> card.getMilestones().add(milestone))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> card.getMilestones().clear())
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> card.getNotes().add(note))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	@DisplayName("FR-06.2: a blank or null note text is rejected with field text")
	void fr06_2_noteBlankTextRejectedWithFieldText() {
		LearningCard card = card();

		ValidationException blank = catchThrowableOfType(ValidationException.class,
				() -> card.addNote("   ", NOW));
		ValidationException missing = catchThrowableOfType(ValidationException.class,
				() -> new LearningNote(card, null, NOW));

		assertFieldError(blank, "text");
		assertFieldError(missing, "text");
		assertThat(card.getNotes()).isEmpty();
	}

	@Test
	@DisplayName("FR-06.2: a note text of 5001 characters is rejected")
	void fr06_2_noteText5001CharsRejected() {
		LearningCard card = card();

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> card.addNote("n".repeat(5001), NOW));

		assertFieldError(ex, "text");
	}

	@Test
	@DisplayName("FR-06.2: a note text of 5000 characters is accepted and stored as given")
	void fr06_2_noteText5000CharsAccepted() {
		LearningCard card = card();
		String text = "n".repeat(5000);

		LearningNote note = card.addNote(text, NOW);

		assertThat(note.getText()).isEqualTo(text);
		assertThat(note.getCreatedAt()).isEqualTo(NOW);
		assertThat(note.getCard()).isSameAs(card);
		assertThat(card.getNotes()).containsExactly(note);
	}

	@Test
	@DisplayName("FR-06.2: a note text is not trimmed")
	void fr06_2_noteTextNotTrimmed() {
		LearningNote note = card().addNote("  spaced  ", NOW);

		assertThat(note.getText()).isEqualTo("  spaced  ");
	}

	@Test
	@DisplayName("FR-06.2: removeNote removes it from the card")
	void fr06_2_removeNoteRemovesFromList() {
		LearningCard card = card();
		LearningNote first = card.addNote("First", NOW);
		LearningNote second = card.addNote("Second", NOW);

		card.removeNote(first);

		assertThat(card.getNotes()).containsExactly(second);
	}

	@Test
	@DisplayName("FR-06.3: milestonesDone and milestonesTotal count the card's milestones")
	void fr06_3_milestoneCounts() {
		LearningCard card = card();
		LearningMilestone first = card.addMilestone("First", null);
		LearningMilestone second = card.addMilestone("Second", TODAY);
		card.addMilestone("Third", null);

		assertThat(card.milestonesDone()).isZero();
		assertThat(card.milestonesTotal()).isEqualTo(3);

		first.update("First", null, true);
		second.update("  Second done  ", TODAY.plusDays(1), true);

		assertThat(second.getTitle()).isEqualTo("Second done");
		assertThat(second.getTargetDate()).isEqualTo(TODAY.plusDays(1));
		assertThat(card.milestonesDone()).isEqualTo(2);
		assertThat(card.milestonesTotal()).isEqualTo(3);

		first.update("First", null, false);

		assertThat(card.milestonesDone()).isEqualTo(1);
		assertThat(card.milestonesTotal()).isEqualTo(3);

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> first.update(" ", null, true));

		assertFieldError(ex, "title");
	}

}
