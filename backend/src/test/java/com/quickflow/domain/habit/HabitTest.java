package com.quickflow.domain.habit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;

class HabitTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), ZoneId.of("Africa/Cairo"));

	private static final Instant NOW = CLOCK.instant();

	private static Habit habit() {
		return new Habit("Read", null, HabitFrequency.DAILY, NOW);
	}

	private static void assertFieldError(ValidationException ex, String field) {
		assertThat(ex).isNotNull();
		assertThat(ex.getErrors()).extracting(FieldError::field).contains(field);
	}

	@Test
	@DisplayName("BR-6: a blank or null name is rejected")
	void br6_blankNameRejected() {
		ValidationException blank = catchThrowableOfType(ValidationException.class,
				() -> new Habit("   ", null, HabitFrequency.DAILY, NOW));
		ValidationException missing = catchThrowableOfType(ValidationException.class,
				() -> new Habit(null, null, HabitFrequency.DAILY, NOW));

		assertFieldError(blank, "name");
		assertFieldError(missing, "name");
	}

	@Test
	@DisplayName("BR-6: a name of 151 characters is rejected")
	void br6_name151CharsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Habit("a".repeat(151), null, HabitFrequency.DAILY, NOW));

		assertFieldError(ex, "name");
	}

	@Test
	@DisplayName("BR-6: a name of 150 characters is accepted")
	void br6_name150CharsAccepted() {
		String name = "a".repeat(150);

		Habit habit = new Habit(name, null, HabitFrequency.DAILY, NOW);

		assertThat(habit.getName()).isEqualTo(name);
	}

	@Test
	@DisplayName("FR-03.3: a null frequency is rejected")
	void fr03_3_nullFrequencyRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Habit("Read", null, null, NOW));

		assertFieldError(ex, "frequency");
	}

	@Test
	@DisplayName("FR-03.1: a description of 2001 characters is rejected")
	void fr03_1_description2001CharsRejected() {
		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> new Habit("Read", "d".repeat(2001), HabitFrequency.DAILY, NOW));

		assertFieldError(ex, "description");
	}

	@Test
	@DisplayName("FR-03.1: a null description is accepted")
	void fr03_1_nullDescriptionAccepted() {
		Habit habit = new Habit("Read", null, HabitFrequency.WEEKLY, NOW);

		assertThat(habit.getDescription()).isNull();
	}

	@Test
	@DisplayName("FR-03.2: a new habit is active and has createdAt set")
	void fr03_2_newHabitActiveWithCreatedAt() {
		Habit habit = habit();

		assertThat(habit.isActive()).isTrue();
		assertThat(habit.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("FR-03.2: a habit can be deactivated and activated again")
	void fr03_2_deactivateThenActivate() {
		Habit habit = habit();

		habit.deactivate();
		assertThat(habit.isActive()).isFalse();
		habit.deactivate();
		assertThat(habit.isActive()).isFalse();

		habit.activate();
		assertThat(habit.isActive()).isTrue();
		habit.activate();
		assertThat(habit.isActive()).isTrue();
	}

	@Test
	@DisplayName("FR-03.2: update changes name, description and frequency but not active or createdAt")
	void fr03_2_updateChangesNameDescriptionFrequency() {
		Habit habit = habit();
		habit.deactivate();

		habit.update("Read books", "20 pages", HabitFrequency.WEEKLY);

		assertThat(habit.getName()).isEqualTo("Read books");
		assertThat(habit.getDescription()).isEqualTo("20 pages");
		assertThat(habit.getFrequency()).isEqualTo(HabitFrequency.WEEKLY);
		assertThat(habit.isActive()).isFalse();
		assertThat(habit.getCreatedAt()).isEqualTo(NOW);

		ValidationException ex = catchThrowableOfType(ValidationException.class,
				() -> habit.update("  ", null, HabitFrequency.DAILY));

		assertFieldError(ex, "name");
	}

}
