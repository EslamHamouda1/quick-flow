package com.quickflow.domain.habit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickflow.domain.common.ConflictException;
import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.common.ValidationException;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-09-20T08:00:00Z");
	private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

	@Mock
	private HabitRepository habits;

	@Mock
	private HabitCompletionRepository completions;

	private HabitService service;

	@BeforeEach
	void setUp() {
		service = new HabitService(habits, completions, new TimeService(Clock.fixed(NOW, CAIRO)));
	}

	private static Habit existing(long id) {
		Habit habit = new Habit("Read", "Twenty pages", HabitFrequency.DAILY, EARLIER);
		ReflectionTestUtils.setField(habit, "id", id);
		return habit;
	}

	@Test
	@DisplayName("FR-04.1: a completion without a date defaults to today")
	void fr04_1_completionDefaultsToToday() {
		Habit habit = existing(7L);
		given(habits.findById(7L)).willReturn(Optional.of(habit));
		given(completions.existsByHabitIdAndCompletionDate(7L, TODAY)).willReturn(false);
		given(completions.saveAndFlush(any(HabitCompletion.class))).willAnswer(inv -> inv.getArgument(0));

		HabitCompletion completion = service.complete(7L, null);

		assertThat(completion.getCompletionDate()).isEqualTo(TODAY);
		assertThat(completion.getCreatedAt()).isEqualTo(NOW);
		assertThat(completion.getHabit()).isSameAs(habit);
	}

	@Test
	@DisplayName("FR-04.1: a future completion date is rejected on field date")
	void fr04_1_futureDateRejectedWithFieldDate() {
		given(habits.findById(7L)).willReturn(Optional.of(existing(7L)));

		assertThatThrownBy(() -> service.complete(7L, LocalDate.of(2026, 9, 25)))
				.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
						.extracting(FieldError::field).containsExactly("date"));
		verify(completions, never()).saveAndFlush(any(HabitCompletion.class));
	}

	@Test
	@DisplayName("FR-04.1: a past completion date is accepted")
	void fr04_1_pastDateAccepted() {
		Habit habit = existing(7L);
		LocalDate past = LocalDate.of(2026, 9, 20);
		given(habits.findById(7L)).willReturn(Optional.of(habit));
		given(completions.existsByHabitIdAndCompletionDate(7L, past)).willReturn(false);
		given(completions.saveAndFlush(any(HabitCompletion.class))).willAnswer(inv -> inv.getArgument(0));

		HabitCompletion completion = service.complete(7L, past);

		assertThat(completion.getCompletionDate()).isEqualTo(past);
		assertThat(completion.getCreatedAt()).isEqualTo(NOW);
		assertThat(completion.getHabit()).isSameAs(habit);
	}

	@Test
	@DisplayName("BR-7, FR-04.2: completing an already completed date is a conflict")
	void br7_existingCompletionIsConflict() {
		given(habits.findById(7L)).willReturn(Optional.of(existing(7L)));
		given(completions.existsByHabitIdAndCompletionDate(7L, TODAY)).willReturn(true);

		assertThatThrownBy(() -> service.complete(7L, TODAY))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Habit 7 is already complete for 2026-09-24");
		verify(completions, never()).saveAndFlush(any(HabitCompletion.class));
	}

	@Test
	@DisplayName("BR-7: a unique-constraint violation on save is a conflict")
	void br7_dataIntegrityViolationIsConflict() {
		given(habits.findById(7L)).willReturn(Optional.of(existing(7L)));
		given(completions.existsByHabitIdAndCompletionDate(7L, TODAY)).willReturn(false);
		willThrow(new DataIntegrityViolationException("duplicate")).given(completions)
				.saveAndFlush(any(HabitCompletion.class));

		assertThatThrownBy(() -> service.complete(7L, TODAY))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Habit 7 is already complete for 2026-09-24");
	}

	@Test
	@DisplayName("FR-04.4: completing an inactive habit is a conflict")
	void fr04_4_inactiveHabitCompletionIsConflict() {
		Habit habit = existing(7L);
		habit.deactivate();
		given(habits.findById(7L)).willReturn(Optional.of(habit));

		assertThatThrownBy(() -> service.complete(7L, TODAY))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Habit 7 is inactive");
		verify(completions, never()).saveAndFlush(any(HabitCompletion.class));
	}

	@Test
	@DisplayName("FR-04.3: undo removes the completion for that date")
	void fr04_3_undoRemovesCompletion() {
		Habit habit = existing(7L);
		LocalDate date = LocalDate.of(2026, 9, 20);
		HabitCompletion completion = new HabitCompletion(habit, date, EARLIER);
		given(habits.findById(7L)).willReturn(Optional.of(habit));
		given(completions.findByHabitIdAndCompletionDate(7L, date)).willReturn(Optional.of(completion));

		service.undo(7L, date);

		verify(completions).delete(completion);
	}

	@Test
	@DisplayName("FR-04.3: undo of a missing completion is not found")
	void fr04_3_undoMissingCompletionNotFound() {
		LocalDate date = LocalDate.of(2026, 9, 20);
		given(habits.findById(7L)).willReturn(Optional.of(existing(7L)));
		given(completions.findByHabitIdAndCompletionDate(7L, date)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.undo(7L, date))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Habit 7 has no completion for 2026-09-20");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "get", "update", "activate", "deactivate", "delete", "complete", "undo", "completions" })
	@DisplayName("FR-03.2: an unknown habit id throws NotFoundException")
	void fr03_2_unknownHabitNotFound(String operation) {
		given(habits.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> {
			switch (operation) {
				case "get" -> service.get(99L);
				case "update" -> service.update(99L, "Read", null, HabitFrequency.DAILY);
				case "activate" -> service.activate(99L);
				case "deactivate" -> service.deactivate(99L);
				case "delete" -> service.delete(99L);
				case "complete" -> service.complete(99L, null);
				case "undo" -> service.undo(99L, LocalDate.of(2026, 9, 20));
				case "completions" -> service.completions(99L);
				default -> throw new IllegalArgumentException(operation);
			}
		}).isInstanceOf(NotFoundException.class).hasMessage("Habit 99 not found");
	}

	@Test
	@DisplayName("FR-03.2: create sets createdAt from the clock and starts active with no progress")
	void fr03_2_createSetsCreatedAtFromClock() {
		given(habits.save(any(Habit.class))).willAnswer(inv -> inv.getArgument(0));

		HabitView view = service.create("Read", "Twenty pages", HabitFrequency.DAILY);

		assertThat(view.habit().getCreatedAt()).isEqualTo(NOW);
		assertThat(view.habit().isActive()).isTrue();
		assertThat(view.habit().getName()).isEqualTo("Read");
		assertThat(view.progress()).isEqualTo(new HabitProgress(false, false, 0));
		verify(habits).save(view.habit());
		verifyNoInteractions(completions);
	}

	@Test
	@DisplayName("FR-04.5: get computes progress from the habit's completion dates")
	void fr04_5_getComputesProgressFromCompletionDates() {
		Habit habit = existing(7L);
		given(habits.findById(7L)).willReturn(Optional.of(habit));
		given(completions.findDatesByHabitId(7L)).willReturn(List.of(LocalDate.of(2026, 9, 23), TODAY));

		HabitView view = service.get(7L);

		assertThat(view.habit()).isSameAs(habit);
		assertThat(view.progress()).isEqualTo(new HabitProgress(true, true, 2));
	}

	@Test
	@DisplayName("FR-03.2: list without a filter returns all habits")
	void fr03_2_listWithoutFilterUsesAll() {
		Habit first = existing(7L);
		Habit second = existing(8L);
		given(habits.findAllByOrderByCreatedAtAscIdAsc()).willReturn(List.of(first, second));
		given(completions.findDatesByHabitId(7L)).willReturn(List.of(TODAY));
		given(completions.findDatesByHabitId(8L)).willReturn(List.of());

		List<HabitView> views = service.list(null);

		assertThat(views).extracting(HabitView::habit).containsExactly(first, second);
		assertThat(views.get(0).progress()).isEqualTo(new HabitProgress(true, true, 1));
		assertThat(views.get(1).progress()).isEqualTo(new HabitProgress(false, false, 0));
		verify(habits, never()).findByActiveOrderByCreatedAtAscIdAsc(anyBoolean());
	}

	@Test
	@DisplayName("FR-03.2: list with active=true uses the active filter")
	void fr03_2_listActiveUsesActiveFilter() {
		Habit habit = existing(7L);
		given(habits.findByActiveOrderByCreatedAtAscIdAsc(true)).willReturn(List.of(habit));
		given(completions.findDatesByHabitId(7L)).willReturn(List.of());

		List<HabitView> views = service.list(true);

		assertThat(views).extracting(HabitView::habit).containsExactly(habit);
		verify(habits, never()).findAllByOrderByCreatedAtAscIdAsc();
	}

	@Test
	@DisplayName("FR-03.2: deactivate and activate toggle the active flag")
	void fr03_2_deactivateAndActivate() {
		Habit habit = existing(7L);
		given(habits.findById(7L)).willReturn(Optional.of(habit));

		HabitView deactivated = service.deactivate(7L);
		assertThat(deactivated.habit().isActive()).isFalse();

		HabitView activated = service.activate(7L);
		assertThat(activated.habit().isActive()).isTrue();
		assertThat(activated.habit().getCreatedAt()).isEqualTo(EARLIER);
	}

	@Test
	@DisplayName("FR-03.2: delete removes the existing habit through the repository")
	void fr03_2_deleteRemovesHabit() {
		Habit habit = existing(7L);
		given(habits.findById(7L)).willReturn(Optional.of(habit));

		service.delete(7L);

		verify(habits).delete(habit);
	}

	@Test
	@DisplayName("FR-04.3: completions are returned newest first from the repository")
	void fr04_3_completionsNewestFirstFromRepository() {
		Habit habit = existing(7L);
		HabitCompletion newer = new HabitCompletion(habit, TODAY, NOW);
		HabitCompletion older = new HabitCompletion(habit, LocalDate.of(2026, 9, 20), EARLIER);
		given(habits.findById(7L)).willReturn(Optional.of(habit));
		given(completions.findByHabitIdOrderByCompletionDateDesc(7L)).willReturn(List.of(newer, older));

		List<HabitCompletion> result = service.completions(7L);

		assertThat(result).containsExactly(newer, older);
	}

}
