package com.quickflow.web.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.learning.LearningCard;
import com.quickflow.domain.learning.LearningMilestone;
import com.quickflow.domain.learning.LearningNote;
import com.quickflow.domain.learning.LearningService;
import com.quickflow.domain.learning.LearningStatus;

@WebMvcTest(LearningCardController.class)
@Import(LearningCardControllerTest.FixedTime.class)
class LearningCardControllerTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	@TestConfiguration
	static class FixedTime {

		@Bean
		TimeService timeService() {
			return new TimeService(Clock.fixed(NOW, ZoneId.of("Africa/Cairo")));
		}

	}

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private LearningService learningService;

	private static LearningCard card(String title, String description) {
		LearningCard card = new LearningCard(title, description, NOW);
		ReflectionTestUtils.setField(card, "id", 7L);
		return card;
	}

	private static LearningMilestone milestone(LearningCard card, String title, LocalDate targetDate, long id) {
		LearningMilestone milestone = card.addMilestone(title, targetDate);
		ReflectionTestUtils.setField(milestone, "id", id);
		return milestone;
	}

	private static LearningNote note(LearningCard card, String text, long id) {
		LearningNote note = card.addNote(text, NOW);
		ReflectionTestUtils.setField(note, "id", id);
		return note;
	}

	@Test
	@DisplayName("US3: POST /api/learning-cards creates a card and returns 201 NOT_STARTED")
	void createLearningCardReturns201NotStarted() {
		given(this.learningService.create("Learn Rust", "Ownership"))
			.willReturn(card("Learn Rust", "Ownership"));

		var result = assertThat(this.mvc.post().uri("/api/learning-cards")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Learn Rust","description":"Ownership"}
						"""));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(7);
		json.extractingPath("$.title").isEqualTo("Learn Rust");
		json.extractingPath("$.description").isEqualTo("Ownership");
		json.extractingPath("$.status").isEqualTo("NOT_STARTED");
		json.extractingPath("$.createdAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.milestones").asArray().isEmpty();
		json.extractingPath("$.notes").asArray().isEmpty();
		json.extractingPath("$.milestonesDone").isEqualTo(0);
		json.extractingPath("$.milestonesTotal").isEqualTo(0);

		then(this.learningService).should().create("Learn Rust", "Ownership");
	}

	@Test
	@DisplayName("BR-8: a blank title is rejected with 400 and field title")
	void br8_blankTitleIs400WithTitleField() {
		var result = assertThat(this.mvc.post().uri("/api/learning-cards")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"   \"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("title");

		verifyNoInteractions(this.learningService);
	}

	@Test
	@DisplayName("US3: an unknown status is rejected with 400 and field status")
	void unknownStatusIs400WithStatusField() {
		var result = assertThat(this.mvc.put().uri("/api/learning-cards/7")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Learn Rust\",\"status\":\"DONE\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("status");

		verifyNoInteractions(this.learningService);
	}

	@Test
	@DisplayName("US3: a missing status on update is rejected with 400 and field status")
	void missingStatusIs400WithStatusField() {
		var result = assertThat(this.mvc.put().uri("/api/learning-cards/7")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Learn Rust\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("status");

		verifyNoInteractions(this.learningService);
	}

	@Test
	@DisplayName("US3: PUT /api/learning-cards/{id} updates the card and returns 200")
	void updateLearningCardReturns200() {
		LearningCard updated = card("Learn Rust well", null);
		updated.update("Learn Rust well", null, LearningStatus.IN_PROGRESS);
		given(this.learningService.update(7L, "Learn Rust well", null, LearningStatus.IN_PROGRESS)).willReturn(updated);

		var result = assertThat(this.mvc.put().uri("/api/learning-cards/7")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Learn Rust well\",\"status\":\"IN_PROGRESS\"}"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$.id").isEqualTo(7);
		result.bodyJson().extractingPath("$.title").isEqualTo("Learn Rust well");
		result.bodyJson().extractingPath("$.status").isEqualTo("IN_PROGRESS");

		then(this.learningService).should().update(7L, "Learn Rust well", null, LearningStatus.IN_PROGRESS);
	}

	@Test
	@DisplayName("US3: GET /api/learning-cards lists cards with embedded milestones and notes")
	void listLearningCardsIncludesMilestonesAndNotes() {
		LearningCard card = card("Learn Rust", null);
		LearningMilestone done = milestone(card, "Read the book", LocalDate.of(2026, 10, 1), 11L);
		done.update("Read the book", LocalDate.of(2026, 10, 1), true);
		milestone(card, "Build a CLI", null, 12L);
		note(card, "Borrowing is tricky", 21L);
		given(this.learningService.list()).willReturn(List.of(card));

		var result = assertThat(this.mvc.get().uri("/api/learning-cards"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$[0].id").isEqualTo(7);
		json.extractingPath("$[0].milestones[0].id").isEqualTo(11);
		json.extractingPath("$[0].milestones[0].cardId").isEqualTo(7);
		json.extractingPath("$[0].milestones[0].title").isEqualTo("Read the book");
		json.extractingPath("$[0].milestones[0].done").isEqualTo(true);
		json.extractingPath("$[0].milestones[0].targetDate").isEqualTo("2026-10-01");
		json.extractingPath("$[0].milestones[1].id").isEqualTo(12);
		json.extractingPath("$[0].milestones[1].done").isEqualTo(false);
		json.extractingPath("$[0].notes[0].id").isEqualTo(21);
		json.extractingPath("$[0].notes[0].cardId").isEqualTo(7);
		json.extractingPath("$[0].notes[0].text").isEqualTo("Borrowing is tricky");
		json.extractingPath("$[0].notes[0].createdAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$[0].milestonesDone").isEqualTo(1);
		json.extractingPath("$[0].milestonesTotal").isEqualTo(2);

		then(this.learningService).should().list();
	}

	@Test
	@DisplayName("US3: POST /api/learning-cards/{id}/milestones adds a milestone and returns 201")
	void addMilestoneIs201() {
		LearningCard card = card("Learn Rust", null);
		given(this.learningService.addMilestone(7L, "Read the book", LocalDate.of(2026, 10, 1)))
			.willReturn(milestone(card, "Read the book", LocalDate.of(2026, 10, 1), 11L));

		var result = assertThat(this.mvc.post().uri("/api/learning-cards/7/milestones")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Read the book\",\"targetDate\":\"2026-10-01\"}"));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(11);
		json.extractingPath("$.cardId").isEqualTo(7);
		json.extractingPath("$.title").isEqualTo("Read the book");
		json.extractingPath("$.done").isEqualTo(false);
		json.extractingPath("$.targetDate").isEqualTo("2026-10-01");

		then(this.learningService).should().addMilestone(7L, "Read the book", LocalDate.of(2026, 10, 1));
	}

	@Test
	@DisplayName("US3: PUT /api/learning-cards/{id}/milestones/{milestoneId} updates the milestone and returns 200")
	void updateMilestoneIs200() {
		LearningCard card = card("Learn Rust", null);
		LearningMilestone updated = milestone(card, "Read the book", null, 11L);
		updated.update("Read the book", null, true);
		given(this.learningService.updateMilestone(7L, 11L, "Read the book", null, true)).willReturn(updated);

		var result = assertThat(this.mvc.put().uri("/api/learning-cards/7/milestones/11")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Read the book\",\"done\":true}"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$.id").isEqualTo(11);
		result.bodyJson().extractingPath("$.cardId").isEqualTo(7);
		result.bodyJson().extractingPath("$.done").isEqualTo(true);

		then(this.learningService).should().updateMilestone(7L, 11L, "Read the book", null, true);
	}

	@Test
	@DisplayName("US3: a missing done on milestone update is rejected with 400 and field done")
	void missingMilestoneDoneIs400WithDoneField() {
		var result = assertThat(this.mvc.put().uri("/api/learning-cards/7/milestones/11")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Read the book\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("done");

		verifyNoInteractions(this.learningService);
	}

	@Test
	@DisplayName("BR-9: a milestone of another card is 404 problem")
	void br9_milestoneOfOtherCardIs404() {
		given(this.learningService.updateMilestone(7L, 99L, "Read the book", null, false))
			.willThrow(new NotFoundException("Milestone 99 not found on learning card 7"));

		var result = assertThat(this.mvc.put().uri("/api/learning-cards/7/milestones/99")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Read the book\",\"done\":false}"));
		result.hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.status").isEqualTo(404);
		result.bodyJson().extractingPath("$.detail").isEqualTo("Milestone 99 not found on learning card 7");
	}

	@Test
	@DisplayName("US3: DELETE /api/learning-cards/{id}/milestones/{milestoneId} returns 204 with no body")
	void deleteMilestoneIs204() {
		var result = assertThat(this.mvc.delete().uri("/api/learning-cards/7/milestones/11"));
		result.hasStatus(204);
		result.body().isEmpty();

		then(this.learningService).should().deleteMilestone(7L, 11L);
	}

	@Test
	@DisplayName("US3: POST /api/learning-cards/{id}/notes adds a note and returns 201")
	void addNoteIs201() {
		LearningCard card = card("Learn Rust", null);
		given(this.learningService.addNote(7L, "Borrowing is tricky"))
			.willReturn(note(card, "Borrowing is tricky", 21L));

		var result = assertThat(this.mvc.post().uri("/api/learning-cards/7/notes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"Borrowing is tricky\"}"));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(21);
		json.extractingPath("$.cardId").isEqualTo(7);
		json.extractingPath("$.text").isEqualTo("Borrowing is tricky");
		json.extractingPath("$.createdAt").isEqualTo("2026-09-24T12:00:00+03:00");

		then(this.learningService).should().addNote(7L, "Borrowing is tricky");
	}

	@Test
	@DisplayName("US3: a blank note text is rejected with 400 and field text")
	void blankNoteTextIs400WithTextField() {
		var result = assertThat(this.mvc.post().uri("/api/learning-cards/7/notes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"  \"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("text");

		verifyNoInteractions(this.learningService);
	}

	@Test
	@DisplayName("US3: DELETE /api/learning-cards/{id}/notes/{noteId} returns 204 with no body")
	void deleteNoteIs204() {
		var result = assertThat(this.mvc.delete().uri("/api/learning-cards/7/notes/21"));
		result.hasStatus(204);
		result.body().isEmpty();

		then(this.learningService).should().deleteNote(7L, 21L);
	}

	@Test
	@DisplayName("US3: GET /api/learning-cards/{id} for an unknown id is 404 problem")
	void getLearningCardUnknownIs404() {
		given(this.learningService.get(99L)).willThrow(new NotFoundException("Learning card 99 not found"));

		var result = assertThat(this.mvc.get().uri("/api/learning-cards/99"));
		result.hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.status").isEqualTo(404);
		result.bodyJson().extractingPath("$.detail").isEqualTo("Learning card 99 not found");
	}

	@Test
	@DisplayName("US3: DELETE /api/learning-cards/{id} returns 204 with no body")
	void deleteLearningCardIs204() {
		var result = assertThat(this.mvc.delete().uri("/api/learning-cards/7"));
		result.hasStatus(204);
		result.body().isEmpty();

		then(this.learningService).should().delete(7L);
	}

}
