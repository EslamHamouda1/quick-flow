package com.quickflow.domain.learning;

import java.time.Instant;
import java.time.LocalDate;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_card")
public class LearningCard {

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
	private LearningStatus status;

	@Column(nullable = false)
	private Instant createdAt;

	/** Removed together with the card (BR-14); creation order (A-5). */
	@OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<LearningMilestone> milestones = new ArrayList<>();

	/** Removed together with the card (BR-14); oldest first (A-5). */
	@OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt, id")
	private List<LearningNote> notes = new ArrayList<>();

	protected LearningCard() {
	}

	/** A new card, Not Started (FR-05.1). */
	public LearningCard(String title, String description, Instant now) {
		apply(title, description, LearningStatus.NOT_STARTED);
		this.createdAt = now;
	}

	/** Replaces the editable fields; never changes {@code createdAt}, milestones or notes (A-2). */
	public void update(String title, String description, LearningStatus status) {
		apply(title, description, status);
	}

	public LearningMilestone addMilestone(String title, LocalDate targetDate) {
		LearningMilestone milestone = new LearningMilestone(this, title, targetDate);
		milestones.add(milestone);
		return milestone;
	}

	public LearningNote addNote(String text, Instant now) {
		LearningNote note = new LearningNote(this, text, now);
		notes.add(note);
		return note;
	}

	/** The row is deleted by orphan removal. */
	public void removeMilestone(LearningMilestone milestone) {
		milestones.remove(milestone);
	}

	/** The row is deleted by orphan removal. */
	public void removeNote(LearningNote note) {
		notes.remove(note);
	}

	public int milestonesDone() {
		return (int) milestones.stream().filter(LearningMilestone::isDone).count();
	}

	public int milestonesTotal() {
		return milestones.size();
	}

	private void apply(String title, String description, LearningStatus status) {
		List<FieldError> errors = new ArrayList<>();
		String cleanTitle = checkTitle(title, errors);
		String cleanDescription = checkDescription(description, errors);
		if (status == null) {
			errors.add(new FieldError("status", "must not be null"));
		}
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
		this.title = cleanTitle;
		this.description = cleanDescription;
		this.status = status;
	}

	/** Trimmed, non-blank (BR-8), at most 200 characters (A-1); shared with milestone titles. */
	static String checkTitle(String title, List<FieldError> errors) {
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

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public LearningStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<LearningMilestone> getMilestones() {
		return Collections.unmodifiableList(milestones);
	}

	public List<LearningNote> getNotes() {
		return Collections.unmodifiableList(notes);
	}

}
