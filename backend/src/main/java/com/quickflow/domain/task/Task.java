package com.quickflow.domain.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "task", indexes = {
		@Index(name = "idx_task_archived_due_date", columnList = "archived, due_date"),
		@Index(name = "idx_task_status", columnList = "status") })
public class Task {

	static final int TITLE_MAX = 200;

	static final int DESCRIPTION_MAX = 2000;

	static final int TAG_MAX = 30;

	static final int TAGS_MAX = 10;

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

	/** Trimmed, lower case, unique per task (BR-T1, BR-T2, research R-1, R-2); deleted with the task (BR-T4). */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "task_tag", joinColumns = @JoinColumn(name = "task_id"),
			uniqueConstraints = @UniqueConstraint(columnNames = { "task_id", "tag" }),
			indexes = @Index(name = "idx_task_tag_tag", columnList = "tag"))
	@Column(name = "tag", nullable = false, length = TAG_MAX)
	private Set<String> tags = new TreeSet<>();

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

	/**
	 * Adds tags, all or nothing (FR-001, BR-T1, BR-T3); a tag already on the task is kept once (BR-T2, Q2).
	 * {@code updatedAt} changes only when the tag set does (R-8).
	 */
	public void addTags(List<String> newTags, Instant now) {
		List<FieldError> errors = new ArrayList<>();
		Set<String> merged = new TreeSet<>(tags);
		for (String tag : newTags) {
			String clean = normalize(tag);
			if (clean.isEmpty()) {
				errors.add(new FieldError("tags", "tag must not be blank"));
			}
			else if (clean.length() > TAG_MAX) {
				errors.add(new FieldError("tags", "must be at most " + TAG_MAX + " characters"));
			}
			else {
				merged.add(clean);
			}
		}
		if (errors.isEmpty() && merged.size() > TAGS_MAX) {
			errors.add(new FieldError("tags", "must have at most " + TAGS_MAX + " tags"));
		}
		throwIfAny(errors);
		if (merged.size() != tags.size()) {
			tags.addAll(merged);
			updatedAt = now;
		}
	}

	/** Removes one tag, ignoring case (FR-005, BR-T2); a tag the task doesn't have is a no-op (Q3). */
	public void removeTag(String tag, Instant now) {
		String clean = normalize(tag);
		if (clean.isEmpty()) {
			throw new ValidationException("tag", "must not be blank");
		}
		if (tags.remove(clean)) {
			updatedAt = now;
		}
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

	private static String normalize(String tag) {
		return tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
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

	/** Alphabetical, unmodifiable (FR-002). */
	public List<String> getTags() {
		return List.copyOf(new TreeSet<>(tags));
	}

}
