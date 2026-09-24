package com.quickflow.domain.habit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HabitProgressCalculatorTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), ZoneId.of("Africa/Cairo"));

	/** Thursday 2026-09-24; its week runs Monday 2026-09-21 to Sunday 2026-09-27. */
	private static final LocalDate TODAY = LocalDate.now(CLOCK);

	private static LocalDate date(String iso) {
		return LocalDate.parse(iso);
	}

	@Test
	@DisplayName("FR-04.5: a daily habit completed today is completed today and done for the day")
	void fr04_5_dailyCompletedToday() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.DAILY, List.of(TODAY), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(true, true, 1));
	}

	@Test
	@DisplayName("FR-04.5: a daily habit without a completion today is not done for the day")
	void fr04_5_dailyNotDoneWithoutTodayCompletion() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.DAILY,
				List.of(TODAY.minusDays(1)), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(false, false, 1));
	}

	@Test
	@DisplayName("FR-04.5: a daily streak counts consecutive days back from today")
	void fr04_5_dailyStreakFromToday() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.DAILY,
				List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(true, true, 3));
	}

	@Test
	@DisplayName("FR-04.5: a daily streak counts back from yesterday when today is open")
	void fr04_5_dailyStreakFromYesterdayWhenTodayOpen() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.DAILY,
				List.of(TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3)), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(false, false, 3));
	}

	@Test
	@DisplayName("FR-04.5: a missed day breaks the daily streak")
	void fr04_5_dailyStreakBrokenByGap() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.DAILY,
				List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(3), TODAY.minusDays(4)), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(true, true, 2));
	}

	@Test
	@DisplayName("FR-04.5: the daily streak is 0 when neither today nor yesterday is completed")
	void fr04_5_dailyStreakZeroWhenTodayAndYesterdayOpen() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.DAILY,
				List.of(TODAY.minusDays(2), TODAY.minusDays(3)), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(false, false, 0));
	}

	@Test
	@DisplayName("FR-04.5: a weekly habit is done for the week by any completion in the current week")
	void fr04_5_weeklyDoneByAnyDateInCurrentWeek() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.WEEKLY,
				List.of(date("2026-09-21")), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(false, true, 1));
	}

	@Test
	@DisplayName("FR-04.5: a week runs Monday to Sunday, so a Sunday and the next Monday are different weeks")
	void fr04_5_weeklyWeekRunsMondayToSunday() {
		HabitProgress onMonday = HabitProgressCalculator.progress(HabitFrequency.WEEKLY,
				List.of(date("2026-09-27")), date("2026-09-28"));
		HabitProgress onSunday = HabitProgressCalculator.progress(HabitFrequency.WEEKLY,
				List.of(date("2026-09-21")), date("2026-09-27"));

		assertThat(onMonday).isEqualTo(new HabitProgress(false, false, 1));
		assertThat(onSunday).isEqualTo(new HabitProgress(false, true, 1));
	}

	@Test
	@DisplayName("FR-04.5: a weekly streak counts consecutive weeks, several completions in one week count once")
	void fr04_5_weeklyStreakOverConsecutiveWeeks() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.WEEKLY,
				List.of(date("2026-09-22"), date("2026-09-23"), TODAY, date("2026-09-15"), date("2026-09-08")),
				TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(true, true, 3));
	}

	@Test
	@DisplayName("FR-04.5: a weekly streak counts back from the previous week when the current week is open")
	void fr04_5_weeklyStreakFromPreviousWeekWhenCurrentOpen() {
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.WEEKLY,
				List.of(date("2026-09-17"), date("2026-09-10")), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(false, false, 2));
	}

	@Test
	@DisplayName("FR-04.5: a week without a completion breaks the weekly streak")
	void fr04_5_weeklyStreakBrokenByEmptyWeek() {
		// weeks: 09-21 (done), 09-14 (done), 09-07 (empty), 08-31 (done)
		HabitProgress progress = HabitProgressCalculator.progress(HabitFrequency.WEEKLY,
				List.of(date("2026-09-22"), date("2026-09-15"), date("2026-09-01")), TODAY);

		assertThat(progress).isEqualTo(new HabitProgress(false, true, 2));
	}

	@Test
	@DisplayName("FR-04.5: a weekly habit is completed today only for a completion dated today")
	void fr04_5_weeklyCompletedTodayOnlyForTodaysDate() {
		HabitProgress otherDay = HabitProgressCalculator.progress(HabitFrequency.WEEKLY,
				List.of(date("2026-09-23")), TODAY);
		HabitProgress today = HabitProgressCalculator.progress(HabitFrequency.WEEKLY, List.of(TODAY), TODAY);

		assertThat(otherDay).isEqualTo(new HabitProgress(false, true, 1));
		assertThat(today).isEqualTo(new HabitProgress(true, true, 1));
	}

	@Test
	@DisplayName("FR-04.5: a habit without completions is not done and has streak 0")
	void fr04_5_noCompletions() {
		HabitProgress daily = HabitProgressCalculator.progress(HabitFrequency.DAILY, List.of(), TODAY);
		HabitProgress weekly = HabitProgressCalculator.progress(HabitFrequency.WEEKLY, List.of(), TODAY);

		assertThat(daily).isEqualTo(new HabitProgress(false, false, 0));
		assertThat(weekly).isEqualTo(new HabitProgress(false, false, 0));
	}

}
