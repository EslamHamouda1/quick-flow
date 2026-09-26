package com.quickflow.domain.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "task", indexes = {
		@Index(name = "idx_task_archived_due_date", columnList = "archived, due_date"),
		@Index(name = "idx_task_status", columnList = "status") })
public class Task {

	static final int TITLE_MAX = 200;

	static final int DESCRIPTION_MAX = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = TITLE_MAX)
	private String title;

	@Column(length = DESCRIPTION_MAX)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskPriority priority;

	private LocalDate dueDate;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	private Instant completedAt;

	@Column(nullable = false)
	private boolean archived;

	protected Task() {
	}

	/** A new task; missing status and priority default to TODO and MEDIUM (FR-01.3). */
	public Task(String title, String description, TaskStatus status, TaskPriority priority, LocalDate dueDate,
			Instant now) {
		List<FieldError> errors = new ArrayList<>();
		String cleanTitle = checkTitle(title, errors);
		String cleanDescription = checkDescription(description, errors);
		throwIfAny(errors);
		this.title = cleanTitle;
		this.description = cleanDescription;
		this.status = TaskStatus.TODO;
		this.priority = priority != null ? priority : TaskPriority.MEDIUM;
		this.dueDate = dueDate;
		this.createdAt = now;
		this.archived = false;
		changeStatus(status != null ? status : TaskStatus.TODO, now);
	}

	/** Replaces the editable fields; never changes {@code archived} (A-3). */
	public void update(String title, String description, TaskStatus status, TaskPriority priority, LocalDate dueDate,
			Instant now) {
		List<FieldError> errors = new ArrayList<>();
		String cleanTitle = checkTitle(title, errors);
		String cleanDescription = checkDescription(description, errors);
		if (status == null) {
			errors.add(new FieldError("status", "must not be null"));
		}
		if (priority == null) {
			errors.add(new FieldError("priority", "must not be null"));
		}
		throwIfAny(errors);
		this.title = cleanTitle;
		this.description = cleanDescription;
		this.priority = priority;
		this.dueDate = dueDate;
		changeStatus(status, now);
	}

	/** Becoming DONE sets completedAt, leaving DONE clears it (BR-5, FR-01.4). */
	public void changeStatus(TaskStatus status, Instant now) {
		if (status == null) {
			throw new ValidationException("status", "must not be null");
		}
		if (status == TaskStatus.DONE) {
			if (this.status != TaskStatus.DONE) {
				completedAt = now;
			}
		}
		else {
			completedAt = null;
		}
		this.status = status;
		this.updatedAt = now;
	}

	public void complete(Instant now) {
		changeStatus(TaskStatus.DONE, now);
	}

	public void archive(Instant now) {
		archived = true;
		updatedAt = now;
	}

	public void restore(Instant now) {
		archived = false;
		updatedAt = now;
	}

	/** Due before today, not DONE and not archived (FR-01.7). */
	public boolean isOverdue(LocalDate today) {
		return dueDate != null && dueDate.isBefore(today) && status != TaskStatus.DONE && !archived;
	}

	private static String checkTitle(String title, List<FieldError> errors) {
		String trimmed = title == null ? "" : title.trim();
		if (trimmed.isEmpty()) {
			errors.add(new FieldError("title", "must not be blank"));
		}
		else if (trimmed.length() > TITLE_MAX) {
			errors.add(new FieldError("title", "must be at most " + TITLE_MAX + " characters"));
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

	private static void throwIfAny(List<FieldError> errors) {
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public TaskPriority getPriority() {
		return priority;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public boolean isArchived() {
		return archived;
	}

}
