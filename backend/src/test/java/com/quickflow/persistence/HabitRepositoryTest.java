package com.quickflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.quickflow.domain.habit.Habit;
import com.quickflow.domain.habit.HabitCompletion;
import com.quickflow.domain.habit.HabitCompletionRepository;
import com.quickflow.domain.habit.HabitFrequency;
import com.quickflow.domain.habit.HabitRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
class HabitRepositoryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

	private static final Instant BASE = Instant.parse("2026-09-20T08:00:00Z");

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	@Autowired
	private HabitRepository habits;

	@Autowired
	private HabitCompletionRepository completions;

	@Autowired
	private EntityManager em;

	private int created;

	private Habit habit(String name, HabitFrequency frequency) {
		Instant createdAt = BASE.plusSeconds(3600L * created++);
		return habits.saveAndFlush(new Habit(name, null, frequency, createdAt));
	}

	private Habit habit(String name) {
		return habit(name, HabitFrequency.DAILY);
	}

	private HabitCompletion complete(Habit habit, LocalDate date) {
		return completions.saveAndFlush(new HabitCompletion(habit, date, NOW));
	}

	@Test
	@DisplayName("BR-7: a second completion row for the same habit and date is rejected by the unique constraint")
	void br7_uniqueConstraintRejectsSecondRowForSameDate() {
		Habit habit = habit("Read");
		complete(habit, TODAY);

		assertThatThrownBy(() -> completions.saveAndFlush(new HabitCompletion(habit, TODAY, NOW)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("BR-7: the same habit may be completed on different dates")
	void br7_sameHabitDifferentDatesAllowed() {
		Habit habit = habit("Read");
		complete(habit, TODAY);
		complete(habit, TODAY.minusDays(1));
		em.flush();
		em.clear();

		assertThat(completions.findDatesByHabitId(habit.getId()))
				.containsExactlyInAnyOrder(TODAY, TODAY.minusDays(1));
	}

	@Test
	@DisplayName("BR-14: deleting a habit deletes its completions")
	void br14_deletingHabitDeletesCompletions() {
		Habit habit = habit("Read");
		complete(habit, TODAY);
		complete(habit, TODAY.minusDays(1));
		Long id = habit.getId();
		em.flush();
		em.clear();

		habits.delete(habits.findById(id).orElseThrow());
		habits.flush();
		em.clear();

		assertThat(habits.findById(id)).isEmpty();
		assertThat(completions.findByHabitIdOrderByCompletionDateDesc(id)).isEmpty();
		assertThat(completions.findDatesByHabitId(id)).isEmpty();
		assertThat(completions.count()).isZero();
	}

	@Test
	@DisplayName("FR-03.2: active filter lists active or inactive habits, oldest first")
	void fr03_2_activeFilter() {
		habit("first");
		Habit second = habit("second");
		habit("third", HabitFrequency.WEEKLY);
		Habit fourth = habit("fourth");
		second.deactivate();
		fourth.deactivate();
		habits.saveAllAndFlush(List.of(second, fourth));
		em.flush();
		em.clear();

		assertThat(habits.findByActiveOrderByCreatedAtAscIdAsc(true)).extracting(Habit::getName)
				.containsExactly("first", "third");
		assertThat(habits.findByActiveOrderByCreatedAtAscIdAsc(false)).extracting(Habit::getName)
				.containsExactly("second", "fourth");
		assertThat(habits.findAllByOrderByCreatedAtAscIdAsc()).extracting(Habit::getName)
				.containsExactly("first", "second", "third", "fourth");
	}

	@Test
	@DisplayName("FR-04.5: completion queries return only the given habit's completions")
	void fr04_5_completionDatesOfOneHabitOnly() {
		Habit read = habit("Read");
		Habit run = habit("Run");
		complete(read, TODAY.minusDays(2));
		complete(read, TODAY);
		complete(read, TODAY.minusDays(1));
		complete(run, TODAY.minusDays(5));
		em.flush();
		em.clear();

		assertThat(completions.findDatesByHabitId(read.getId()))
				.containsExactlyInAnyOrder(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));
		assertThat(completions.findDatesByHabitId(run.getId())).containsExactly(TODAY.minusDays(5));

		assertThat(completions.findByHabitIdOrderByCompletionDateDesc(read.getId()))
				.extracting(HabitCompletion::getCompletionDate)
				.containsExactly(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));

		assertThat(completions.existsByHabitIdAndCompletionDate(read.getId(), TODAY)).isTrue();
		assertThat(completions.existsByHabitIdAndCompletionDate(read.getId(), TODAY.minusDays(5))).isFalse();
		assertThat(completions.existsByHabitIdAndCompletionDate(run.getId(), TODAY)).isFalse();

		assertThat(completions.findByHabitIdAndCompletionDate(read.getId(), TODAY.minusDays(1)))
				.get().extracting(HabitCompletion::getCompletionDate).isEqualTo(TODAY.minusDays(1));
		assertThat(completions.findByHabitIdAndCompletionDate(run.getId(), TODAY)).isEmpty();
	}

}
