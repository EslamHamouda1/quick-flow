package com.quickflow.domain.habit;

/** A habit's progress, computed on read (FR-04.5). */
public record HabitProgress(boolean completedToday, boolean doneForCurrentPeriod, int currentStreak) {
}
