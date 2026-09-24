package com.quickflow.domain.learning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A milestone always belongs to exactly one card (BR-9). */
@Entity
@Table(name = "learning_milestone")
public class LearningMilestone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "learning_card_id", nullable = false)
	private LearningCard card;

	@Column(nullable = false, length = LearningCard.TITLE_MAX)
	private String title;

	@Column(nullable = false)
	private boolean done;

	/** Optional; a past date is allowed (A-3). */
	private LocalDate targetDate;

	protected LearningMilestone() {
	}

	/** A new milestone, not done (FR-06.1). */
	public LearningMilestone(LearningCard card, String title, LocalDate targetDate) {
		this.card = card;
		apply(title, targetDate, false);
	}

	public void update(String title, LocalDate targetDate, boolean done) {
		apply(title, targetDate, done);
	}

	private void apply(String title, LocalDate targetDate, boolean done) {
		List<FieldError> errors = new ArrayList<>();
		String cleanTitle = LearningCard.checkTitle(title, errors);
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
		this.title = cleanTitle;
		this.targetDate = targetDate;
		this.done = done;
	}

	public Long getId() {
		return id;
	}

	public LearningCard getCard() {
		return card;
	}

	public Long getCardId() {
		return card.getId();
	}

	public String getTitle() {
		return title;
	}

	public boolean isDone() {
		return done;
	}

	public LocalDate getTargetDate() {
		return targetDate;
	}

}
