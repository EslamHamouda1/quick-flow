package com.quickflow.domain.plan;

import java.util.Optional;

import com.quickflow.domain.habit.Habit;
import com.quickflow.domain.habit.HabitRepository;
import com.quickflow.domain.learning.LearningCard;
import com.quickflow.domain.learning.LearningCardRepository;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskRepository;

import org.springframework.stereotype.Component;

/** Looks up a plan item's source: whether it still exists (BR-10, FR-07.6) and its current title (A-4). */
@Component
public class PlanSourceResolver {

	private final TaskRepository tasks;

	private final HabitRepository habits;

	private final LearningCardRepository cards;

	public PlanSourceResolver(TaskRepository tasks, HabitRepository habits, LearningCardRepository cards) {
		this.tasks = tasks;
		this.habits = habits;
		this.cards = cards;
	}

	/** The task title, habit name or card title; empty when the source doesn't exist (hard delete, A-2). */
	public Optional<String> title(PlanSourceType type, long id) {
		return switch (type) {
			case TASK -> tasks.findById(id).map(Task::getTitle);
			case HABIT -> habits.findById(id).map(Habit::getName);
			case LEARNING_RESOURCE -> cards.findById(id).map(LearningCard::getTitle);
		};
	}

}
