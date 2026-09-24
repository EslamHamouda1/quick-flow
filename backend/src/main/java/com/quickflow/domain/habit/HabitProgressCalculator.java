package com.quickflow.domain.habit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Progress and streak of a habit (FR-04.5). A DAILY period is one date, a WEEKLY period is a Monday–Sunday
 * week (spec A-12). The streak counts consecutive periods with a completion, back from the current period
 * if it is done, otherwise from the previous one (A-7).
 */
public final class HabitProgressCalculator {

	private HabitProgressCalculator() {
	}

	public static HabitProgress progress(HabitFrequency frequency, Collection<LocalDate> completionDates,
			LocalDate today) {
		Set<LocalDate> periods = completionDates.stream()
				.map(date -> periodStart(frequency, date))
				.collect(Collectors.toSet());
		LocalDate current = periodStart(frequency, today);
		boolean done = periods.contains(current);
		int streak = 0;
		for (LocalDate period = done ? current : previous(frequency, current); periods.contains(period);
				period = previous(frequency, period)) {
			streak++;
		}
		return new HabitProgress(completionDates.contains(today), done, streak);
	}

	private static LocalDate periodStart(HabitFrequency frequency, LocalDate date) {
		return switch (frequency) {
			case DAILY -> date;
			case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		};
	}

	private static LocalDate previous(HabitFrequency frequency, LocalDate periodStart) {
		return switch (frequency) {
			case DAILY -> periodStart.minusDays(1);
			case WEEKLY -> periodStart.minusWeeks(1);
		};
	}

}
