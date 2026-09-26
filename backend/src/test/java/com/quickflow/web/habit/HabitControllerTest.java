package com.quickflow.web.habit;

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

import com.quickflow.domain.common.ConflictException;
import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.habit.Habit;
import com.quickflow.domain.habit.HabitCompletion;
import com.quickflow.domain.habit.HabitFrequency;
import com.quickflow.domain.habit.HabitProgress;
import com.quickflow.domain.habit.HabitService;
import com.quickflow.domain.habit.HabitView;

@WebMvcTest(HabitController.class)
@Import(HabitControllerTest.FixedTime.class)
class HabitControllerTest {

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
	private HabitService habitService;

	private static Habit habit(String name, String description, HabitFrequency frequency) {
		Habit habit = new Habit(name, description, frequency, NOW);
		ReflectionTestUtils.setField(habit, "id", 7L);
		return habit;
	}

	private static HabitView view(Habit habit, HabitProgress progress) {
		return new HabitView(habit, progress);
	}

	private static HabitView view(Habit habit) {
		return view(habit, new HabitProgress(false, false, 0));
	}

	private static HabitCompletion completion(Habit habit, LocalDate date, long id) {
		HabitCompletion completion = new HabitCompletion(habit, date, NOW);
		ReflectionTestUtils.setField(completion, "id", id);
		return completion;
	}

	@Test
	@DisplayName("US2: POST /api/habits creates a habit and returns 201 with the habit")
	void createHabitReturns201Habit() {
		Habit created = habit("Read", "20 pages", HabitFrequency.DAILY);
		given(this.habitService.create("Read", "20 pages", HabitFrequency.DAILY))
			.willReturn(view(created, new HabitProgress(true, true, 3)));

		var result = assertThat(this.mvc.post().uri("/api/habits")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Read","description":"20 pages","frequency":"DAILY"}
						"""));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(7);
		json.extractingPath("$.name").isEqualTo("Read");
		json.extractingPath("$.description").isEqualTo("20 pages");
		json.extractingPath("$.frequency").isEqualTo("DAILY");
		json.extractingPath("$.createdAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.active").isEqualTo(true);
		json.extractingPath("$.completedToday").isEqualTo(true);
		json.extractingPath("$.doneForCurrentPeriod").isEqualTo(true);
		json.extractingPath("$.currentStreak").isEqualTo(3);

		then(this.habitService).should().create("Read", "20 pages", HabitFrequency.DAILY);
	}

	@Test
	@DisplayName("BR-6: a blank name is rejected with 400 and field name")
	void br6_blankNameIs400WithNameField() {
		var result = assertThat(this.mvc.post().uri("/api/habits")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\",\"frequency\":\"DAILY\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");

		verifyNoInteractions(this.habitService);
	}

	@Test
	@DisplayName("FR-03.3: an unknown frequency is rejected with 400 and field frequency")
	void unknownFrequencyIs400WithFrequencyField() {
		var result = assertThat(this.mvc.post().uri("/api/habits")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Read\",\"frequency\":\"MONTHLY\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("frequency");

		verifyNoInteractions(this.habitService);
	}

	@Test
	@DisplayName("FR-03.2: GET /api/habits?active=true lists active habits")
	void listHabitsActiveTrue() {
		given(this.habitService.list(true)).willReturn(List.of(view(habit("Read", null, HabitFrequency.DAILY))));

		var result = assertThat(this.mvc.get().uri("/api/habits?active=true"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$[0].id").isEqualTo(7);
		result.bodyJson().extractingPath("$[0].active").isEqualTo(true);

		then(this.habitService).should().list(true);
	}

	@Test
	@DisplayName("US2: GET /api/habits without a filter lists every habit")
	void listHabitsWithoutFilter() {
		given(this.habitService.list(null)).willReturn(List.of(view(habit("Read", null, HabitFrequency.WEEKLY))));

		var result = assertThat(this.mvc.get().uri("/api/habits"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$[0].id").isEqualTo(7);
		result.bodyJson().extractingPath("$[0].frequency").isEqualTo("WEEKLY");

		then(this.habitService).should().list(null);
	}

	@Test
	@DisplayName("FR-04.1: POST /api/habits/{id}/completions without a body completes today and returns 201")
	void completeHabitWithoutBodyIs201() {
		Habit habit = habit("Read", null, HabitFrequency.DAILY);
		given(this.habitService.complete(7L, null)).willReturn(completion(habit, LocalDate.of(2026, 9, 24), 11L));

		var result = assertThat(this.mvc.post().uri("/api/habits/7/completions"));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(11);
		json.extractingPath("$.habitId").isEqualTo(7);
		json.extractingPath("$.completionDate").isEqualTo("2026-09-24");
		json.extractingPath("$.createdAt").isEqualTo("2026-09-24T12:00:00+03:00");

		then(this.habitService).should().complete(7L, null);
	}

	@Test
	@DisplayName("FR-04.1: POST /api/habits/{id}/completions with a date completes that date and returns 201")
	void completeHabitWithDateIs201() {
		Habit habit = habit("Read", null, HabitFrequency.DAILY);
		given(this.habitService.complete(7L, LocalDate.of(2026, 9, 20)))
			.willReturn(completion(habit, LocalDate.of(2026, 9, 20), 12L));

		var result = assertThat(this.mvc.post().uri("/api/habits/7/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"2026-09-20\"}"));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$.id").isEqualTo(12);
		result.bodyJson().extractingPath("$.habitId").isEqualTo(7);
		result.bodyJson().extractingPath("$.completionDate").isEqualTo("2026-09-20");

		then(this.habitService).should().complete(7L, LocalDate.of(2026, 9, 20));
	}

	@Test
	@DisplayName("BR-7, FR-04.2: a second completion for the same date is 409 problem")
	void br7_duplicateCompletionIs409Problem() {
		given(this.habitService.complete(7L, null))
			.willThrow(new ConflictException("Habit 7 is already complete for 2026-09-24"));

		var result = assertThat(this.mvc.post().uri("/api/habits/7/completions"));
		result.hasStatus(409).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.status").isEqualTo(409);
		result.bodyJson().extractingPath("$.detail").isEqualTo("Habit 7 is already complete for 2026-09-24");
	}

	@Test
	@DisplayName("FR-04.3: DELETE /api/habits/{id}/completions/{date} undoes the completion and returns 204")
	void undoHabitCompletionIs204() {
		var result = assertThat(this.mvc.delete().uri("/api/habits/7/completions/2026-09-20"));
		result.hasStatus(204);
		result.body().isEmpty();

		then(this.habitService).should().undo(7L, LocalDate.of(2026, 9, 20));
	}

	@Test
	@DisplayName("FR-03.2: deactivate and activate each return 200 with the habit")
	void deactivateAndActivateReturn200() {
		Habit inactive = habit("Read", null, HabitFrequency.DAILY);
		inactive.deactivate();
		Habit active = habit("Read", null, HabitFrequency.DAILY);
		given(this.habitService.deactivate(7L)).willReturn(view(inactive));
		given(this.habitService.activate(7L)).willReturn(view(active));

		var deactivated = assertThat(this.mvc.post().uri("/api/habits/7/deactivate"));
		deactivated.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		deactivated.bodyJson().extractingPath("$.id").isEqualTo(7);
		deactivated.bodyJson().extractingPath("$.active").isEqualTo(false);

		var activated = assertThat(this.mvc.post().uri("/api/habits/7/activate"));
		activated.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		activated.bodyJson().extractingPath("$.id").isEqualTo(7);
		activated.bodyJson().extractingPath("$.active").isEqualTo(true);

		then(this.habitService).should().deactivate(7L);
		then(this.habitService).should().activate(7L);
	}

	@Test
	@DisplayName("BR-14: DELETE /api/habits/{id} returns 204 with no body")
	void deleteHabitIs204() {
		var result = assertThat(this.mvc.delete().uri("/api/habits/7"));
		result.hasStatus(204);
		result.body().isEmpty();

		then(this.habitService).should().delete(7L);
	}

	@Test
	@DisplayName("US2: GET /api/habits/{id} for an unknown id is 404 problem")
	void getHabitUnknownIs404() {
		given(this.habitService.get(99L)).willThrow(new NotFoundException("Habit 99 not found"));

		var result = assertThat(this.mvc.get().uri("/api/habits/99"));
		result.hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.status").isEqualTo(404);
		result.bodyJson().extractingPath("$.detail").isEqualTo("Habit 99 not found");
	}

	@Test
	@DisplayName("US2: GET /api/habits/{id}/completions lists completions newest first")
	void listHabitCompletionsNewestFirst() {
		Habit habit = habit("Read", null, HabitFrequency.DAILY);
		given(this.habitService.completions(7L)).willReturn(List.of(completion(habit, LocalDate.of(2026, 9, 22), 12L),
				completion(habit, LocalDate.of(2026, 9, 20), 11L)));

		var result = assertThat(this.mvc.get().uri("/api/habits/7/completions"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$[0].id").isEqualTo(12);
		result.bodyJson().extractingPath("$[0].habitId").isEqualTo(7);
		result.bodyJson().extractingPath("$[0].completionDate").isEqualTo("2026-09-22");
		result.bodyJson().extractingPath("$[1].id").isEqualTo(11);
		result.bodyJson().extractingPath("$[1].completionDate").isEqualTo("2026-09-20");

		then(this.habitService).should().completions(7L);
	}

	@Test
	@DisplayName("US2: PUT /api/habits/{id} updates the habit and returns 200")
	void updateHabitReturns200() {
		given(this.habitService.update(7L, "Read more", null, HabitFrequency.WEEKLY))
			.willReturn(view(habit("Read more", null, HabitFrequency.WEEKLY)));

		var result = assertThat(this.mvc.put().uri("/api/habits/7")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Read more\",\"frequency\":\"WEEKLY\"}"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$.id").isEqualTo(7);
		result.bodyJson().extractingPath("$.name").isEqualTo("Read more");
		result.bodyJson().extractingPath("$.frequency").isEqualTo("WEEKLY");

		then(this.habitService).should().update(7L, "Read more", null, HabitFrequency.WEEKLY);
	}

}
