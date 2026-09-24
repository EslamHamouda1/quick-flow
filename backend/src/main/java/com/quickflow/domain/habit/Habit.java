package com.quickflow.domain.habit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "habit")
public class Habit {

	static final int NAME_MAX = 150;

	static final int DESCRIPTION_MAX = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = NAME_MAX)
	private String name;

	@Column(length = DESCRIPTION_MAX)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private HabitFrequency frequency;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private boolean active;

	/** Removed together with the habit (BR-14). */
	@OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HabitCompletion> completions = new ArrayList<>();

	protected Habit() {
	}

	/** A new, active habit (FR-03.1, FR-03.2). */
	public Habit(String name, String description, HabitFrequency frequency, Instant now) {
		apply(name, description, frequency);
		this.createdAt = now;
		this.active = true;
	}

	/** Replaces the editable fields; never changes {@code active}, {@code createdAt} or completions (A-2). */
	public void update(String name, String description, HabitFrequency frequency) {
		apply(name, description, frequency);
	}

	public void activate() {
		active = true;
	}

	/** An inactive habit keeps its history but can't be completed (FR-04.4). */
	public void deactivate() {
		active = false;
	}

	private void apply(String name, String description, HabitFrequency frequency) {
		List<FieldError> errors = new ArrayList<>();
		String cleanName = checkName(name, errors);
		String cleanDescription = checkDescription(description, errors);
		if (frequency == null) {
			errors.add(new FieldError("frequency", "must not be null"));
		}
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
		this.name = cleanName;
		this.description = cleanDescription;
		this.frequency = frequency;
	}

	/** Trimmed, non-blank, at most 150 characters (BR-6). */
	private static String checkName(String name, List<FieldError> errors) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			errors.add(new FieldError("name", "must not be blank"));
		}
		else if (trimmed.length() > NAME_MAX) {
			errors.add(new FieldError("name", "must be at most " + NAME_MAX + " characters"));
		}
		return trimmed;
	}

	private static String checkDescription(String description, List<FieldError> errors) {
		if (description == null || description.isEmpty()) {
			return null;
		}
		if (description.length() > DESCRIPTION_MAX) {
			errors.add(new FieldError("description", "must be at most " + DESCRIPTION_MAX + " characters"));
		}
		return description;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public HabitFrequency getFrequency() {
		return frequency;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isActive() {
		return active;
	}

	public List<HabitCompletion> getCompletions() {
		return Collections.unmodifiableList(completions);
	}

}
