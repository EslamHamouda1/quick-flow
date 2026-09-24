package com.quickflow.domain.habit;

/** A habit together with its progress for today. */
public record HabitView(Habit habit, HabitProgress progress) {
}
