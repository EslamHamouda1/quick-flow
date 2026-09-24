package com.quickflow.domain.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-09-20T08:00:00Z");
	private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
	private static final LocalDate TARGET = LocalDate.of(2026, 10, 1);

	@Mock
	private LearningCardRepository cards;

	@Mock
	private LearningMilestoneRepository milestones;

	@Mock
	private LearningNoteRepository notes;

	private LearningService service;

	@BeforeEach
	void setUp() {
		service = new LearningService(cards, milestones, notes, new TimeService(Clock.fixed(NOW, CAIRO)));
	}

	private static LearningCard existing(long id) {
		LearningCard card = new LearningCard("Learn Rust", "The book", EARLIER);
		ReflectionTestUtils.setField(card, "id", id);
		return card;
	}

	private static LearningMilestone milestoneOf(LearningCard card, long id) {
		LearningMilestone milestone = card.addMilestone("Chapter " + id, TARGET);
		ReflectionTestUtils.setField(milestone, "id", id);
		return milestone;
	}

	private static LearningNote noteOf(LearningCard card, long id) {
		LearningNote note = card.addNote("Note " + id, EARLIER);
		ReflectionTestUtils.setField(note, "id", id);
		return note;
	}

	@Test
	@DisplayName("FR-05.2: create sets createdAt from the clock and starts not started")
	void fr05_2_createSetsCreatedAtFromClock() {
		given(cards.save(any(LearningCard.class))).willAnswer(inv -> inv.getArgument(0));

		LearningCard card = service.create("Learn Rust", "The book");

		assertThat(card.getCreatedAt()).isEqualTo(NOW);
		assertThat(card.getTitle()).isEqualTo("Learn Rust");
		assertThat(card.getDescription()).isEqualTo("The book");
		assertThat(card.getStatus()).isEqualTo(LearningStatus.NOT_STARTED);
		verify(cards).save(card);
		verifyNoInteractions(milestones, notes);
	}

	@Test
	@DisplayName("FR-05.2: validation errors from create propagate and nothing is saved")
	void fr05_2_createValidationErrorPropagatesWithoutSaving() {
		assertThatThrownBy(() -> service.create("   ", null))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("title"));
		verify(cards, never()).save(any(LearningCard.class));
	}

	@Test
	@DisplayName("FR-05.2: list returns the repository's newest-first result")
	void fr05_2_listReturnsNewestFirstFromRepository() {
		LearningCard newer = existing(8L);
		LearningCard older = existing(7L);
		given(cards.findAllByOrderByCreatedAtDescIdDesc()).willReturn(List.of(newer, older));

		List<LearningCard> result = service.list();

		assertThat(result).containsExactly(newer, older);
	}

	@Test
	@DisplayName("FR-05.2: update saves the new title, description and status and keeps createdAt")
	void fr05_2_updateSavesStatus() {
		LearningCard card = existing(7L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		lenient().when(cards.save(any(LearningCard.class))).thenAnswer(inv -> inv.getArgument(0));

		LearningCard result = service.update(7L, "Learn Go", "Tour", LearningStatus.IN_PROGRESS);

		assertThat(result).isSameAs(card);
		assertThat(card.getStatus()).isEqualTo(LearningStatus.IN_PROGRESS);
		assertThat(card.getTitle()).isEqualTo("Learn Go");
		assertThat(card.getDescription()).isEqualTo("Tour");
		assertThat(card.getCreatedAt()).isEqualTo(EARLIER);
	}

	@Test
	@DisplayName("FR-05.2: delete removes the existing card through the repository")
	void fr05_2_deleteRemovesCard() {
		LearningCard card = existing(7L);
		given(cards.findById(7L)).willReturn(Optional.of(card));

		service.delete(7L);

		verify(cards).delete(card);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "get", "update", "delete", "addMilestone", "updateMilestone", "deleteMilestone",
			"addNote", "deleteNote" })
	@DisplayName("FR-05.2: an unknown learning card id throws NotFoundException")
	void fr05_2_unknownCardNotFound(String operation) {
		given(cards.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> {
			switch (operation) {
				case "get" -> service.get(99L);
				case "update" -> service.update(99L, "Learn Rust", null, LearningStatus.IN_PROGRESS);
				case "delete" -> service.delete(99L);
				case "addMilestone" -> service.addMilestone(99L, "Chapter 1", TARGET);
				case "updateMilestone" -> service.updateMilestone(99L, 3L, "Chapter 1", TARGET, true);
				case "deleteMilestone" -> service.deleteMilestone(99L, 3L);
				case "addNote" -> service.addNote(99L, "A note");
				case "deleteNote" -> service.deleteNote(99L, 5L);
				default -> throw new IllegalArgumentException(operation);
			}
		}).isInstanceOf(NotFoundException.class).hasMessage("Learning card 99 not found");
		verify(cards, never()).delete(any(LearningCard.class));
		verifyNoInteractions(milestones, notes);
	}

	@Test
	@DisplayName("FR-06.1: addMilestone returns a not-done milestone attached to the card")
	void fr06_1_addMilestoneReturnsNotDoneMilestoneOnCard() {
		LearningCard card = existing(7L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(milestones.save(any(LearningMilestone.class))).willAnswer(inv -> inv.getArgument(0));

		LearningMilestone milestone = service.addMilestone(7L, "Chapter 1", TARGET);

		assertThat(milestone.isDone()).isFalse();
		assertThat(milestone.getTitle()).isEqualTo("Chapter 1");
		assertThat(milestone.getTargetDate()).isEqualTo(TARGET);
		assertThat(milestone.getCard()).isSameAs(card);
		assertThat(milestone.getCardId()).isEqualTo(7L);
		assertThat(card.getMilestones()).containsExactly(milestone);
		verify(milestones).save(milestone);
	}

	@Test
	@DisplayName("FR-06.3: a milestone can be marked done and then undone")
	void fr06_3_milestoneDoneThenUndone() {
		LearningCard card = existing(7L);
		LearningMilestone milestone = milestoneOf(card, 3L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(milestones.findByIdAndCardId(3L, 7L)).willReturn(Optional.of(milestone));
		lenient().when(milestones.save(any(LearningMilestone.class))).thenAnswer(inv -> inv.getArgument(0));

		LearningMilestone done = service.updateMilestone(7L, 3L, "Chapter 3", TARGET, true);

		assertThat(done).isSameAs(milestone);
		assertThat(milestone.isDone()).isTrue();
		assertThat(card.milestonesDone()).isEqualTo(1);

		LearningMilestone undone = service.updateMilestone(7L, 3L, "Chapter 3", TARGET, false);

		assertThat(undone).isSameAs(milestone);
		assertThat(milestone.isDone()).isFalse();
		assertThat(card.milestonesDone()).isZero();
		assertThat(card.milestonesTotal()).isEqualTo(1);
	}

	@Test
	@DisplayName("FR-06.3: deleting a milestone removes it from the card")
	void fr06_3_deleteMilestoneRemovesIt() {
		LearningCard card = existing(7L);
		LearningMilestone milestone = milestoneOf(card, 3L);
		LearningMilestone kept = milestoneOf(card, 4L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(milestones.findByIdAndCardId(3L, 7L)).willReturn(Optional.of(milestone));

		service.deleteMilestone(7L, 3L);

		assertThat(card.getMilestones()).containsExactly(kept);
	}

	@Test
	@DisplayName("BR-9: updating a milestone of another card is not found")
	void br9_updateMilestoneOfOtherCardNotFound() {
		LearningCard card = existing(7L);
		LearningMilestone own = milestoneOf(card, 4L);
		LearningCard other = existing(8L);
		LearningMilestone foreign = milestoneOf(other, 3L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(milestones.findByIdAndCardId(3L, 7L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateMilestone(7L, 3L, "Hijacked", TARGET, true))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Milestone 3 not found on learning card 7");
		assertThat(card.getMilestones()).containsExactly(own);
		assertThat(other.getMilestones()).containsExactly(foreign);
		assertThat(foreign.getTitle()).isEqualTo("Chapter 3");
		assertThat(foreign.isDone()).isFalse();
		verify(milestones, never()).save(any(LearningMilestone.class));
	}

	@Test
	@DisplayName("BR-9: deleting a milestone of another card is not found")
	void br9_deleteMilestoneOfOtherCardNotFound() {
		LearningCard card = existing(7L);
		LearningMilestone own = milestoneOf(card, 4L);
		LearningCard other = existing(8L);
		LearningMilestone foreign = milestoneOf(other, 3L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(milestones.findByIdAndCardId(3L, 7L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteMilestone(7L, 3L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Milestone 3 not found on learning card 7");
		assertThat(card.getMilestones()).containsExactly(own);
		assertThat(other.getMilestones()).containsExactly(foreign);
		verify(milestones, never()).delete(any(LearningMilestone.class));
	}

	@Test
	@DisplayName("FR-06.2: addNote sets createdAt from the clock and attaches the note to the card")
	void fr06_2_noteCreatedAtFromClock() {
		LearningCard card = existing(7L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(notes.save(any(LearningNote.class))).willAnswer(inv -> inv.getArgument(0));

		LearningNote note = service.addNote(7L, "Ownership clicked");

		assertThat(note.getCreatedAt()).isEqualTo(NOW);
		assertThat(note.getText()).isEqualTo("Ownership clicked");
		assertThat(note.getCard()).isSameAs(card);
		assertThat(note.getCardId()).isEqualTo(7L);
		assertThat(card.getNotes()).containsExactly(note);
		verify(notes).save(note);
	}

	@Test
	@DisplayName("FR-06.3: deleting a note removes it from the card")
	void fr06_3_deleteNoteRemovesIt() {
		LearningCard card = existing(7L);
		LearningNote note = noteOf(card, 5L);
		LearningNote kept = noteOf(card, 6L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(notes.findByIdAndCardId(5L, 7L)).willReturn(Optional.of(note));

		service.deleteNote(7L, 5L);

		assertThat(card.getNotes()).containsExactly(kept);
	}

	@Test
	@DisplayName("BR-9: deleting a note of another card is not found")
	void br9_deleteNoteOfOtherCardNotFound() {
		LearningCard card = existing(7L);
		LearningNote own = noteOf(card, 6L);
		LearningCard other = existing(8L);
		LearningNote foreign = noteOf(other, 5L);
		given(cards.findById(7L)).willReturn(Optional.of(card));
		given(notes.findByIdAndCardId(5L, 7L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteNote(7L, 5L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Note 5 not found on learning card 7");
		assertThat(card.getNotes()).containsExactly(own);
		assertThat(other.getNotes()).containsExactly(foreign);
		verify(notes, never()).delete(any(LearningNote.class));
	}

}
