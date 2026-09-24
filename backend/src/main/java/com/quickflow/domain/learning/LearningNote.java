package com.quickflow.domain.learning;

import java.time.Instant;

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

/** A note on one card (FR-06.2); notes are added and removed, never edited (A-6). */
@Entity
@Table(name = "learning_note")
public class LearningNote {

	static final int TEXT_MAX = 5000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "learning_card_id", nullable = false)
	private LearningCard card;

	@Column(nullable = false, length = TEXT_MAX)
	private String text;

	@Column(nullable = false)
	private Instant createdAt;

	protected LearningNote() {
	}

	/** Checked non-blank and at most 5,000 characters, stored as given (A-1). */
	public LearningNote(LearningCard card, String text, Instant now) {
		if (text == null || text.isBlank()) {
			throw new ValidationException("text", "must not be blank");
		}
		if (text.length() > TEXT_MAX) {
			throw new ValidationException("text", "must be at most " + TEXT_MAX + " characters");
		}
		this.card = card;
		this.text = text;
		this.createdAt = now;
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

	public String getText() {
		return text;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
