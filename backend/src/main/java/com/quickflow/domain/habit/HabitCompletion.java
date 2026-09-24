package com.quickflow.domain.habit;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One completion of a habit for a date; at most one per habit and date (BR-7). */
@Entity
@Table(name = "habit_completion",
		uniqueConstraints = @UniqueConstraint(columnNames = { "habit_id", "completion_date" }))
public class HabitCompletion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "habit_id", nullable = false)
	private Habit habit;

	@Column(name = "completion_date", nullable = false)
	private LocalDate completionDate;

	@Column(nullable = false)
	private Instant createdAt;

	protected HabitCompletion() {
	}

	public HabitCompletion(Habit habit, LocalDate completionDate, Instant now) {
		this.habit = habit;
		this.completionDate = completionDate;
		this.createdAt = now;
	}

	public Long getId() {
		return id;
	}

	public Habit getHabit() {
		return habit;
	}

	public Long getHabitId() {
		return habit.getId();
	}

	public LocalDate getCompletionDate() {
		return completionDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
