package com.quickflow.domain.learning;

import java.time.LocalDate;
import java.util.List;

import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cards returned from here have their milestones and notes loaded, so callers can read them after the transaction
 * (open-in-view is off).
 */
@Service
@Transactional
public class LearningService {

	private final LearningCardRepository cards;

	private final LearningMilestoneRepository milestones;

	private final LearningNoteRepository notes;

	private final TimeService timeService;

	public LearningService(LearningCardRepository cards, LearningMilestoneRepository milestones,
			LearningNoteRepository notes, TimeService timeService) {
		this.cards = cards;
		this.milestones = milestones;
		this.notes = notes;
		this.timeService = timeService;
	}

	public LearningCard create(String title, String description) {
		return loaded(cards.save(new LearningCard(title, description, timeService.now().toInstant())));
	}

	@Transactional(readOnly = true)
	public LearningCard get(long id) {
		return loaded(find(id));
	}

	/** Newest first (A-5). */
	@Transactional(readOnly = true)
	public List<LearningCard> list() {
		return cards.findAllByOrderByCreatedAtDescIdDesc().stream().map(LearningService::loaded).toList();
	}

	public LearningCard update(long id, String title, String description, LearningStatus status) {
		LearningCard card = find(id);
		card.update(title, description, status);
		return loaded(card);
	}

	/** Removes the card and, by cascade, its milestones and notes (BR-14). */
	public void delete(long id) {
		cards.delete(find(id));
	}

	public LearningMilestone addMilestone(long cardId, String title, LocalDate targetDate) {
		LearningCard card = find(cardId);
		return milestones.save(card.addMilestone(title, targetDate));
	}

	public LearningMilestone updateMilestone(long cardId, long milestoneId, String title, LocalDate targetDate,
			boolean done) {
		find(cardId);
		LearningMilestone milestone = findMilestone(cardId, milestoneId);
		milestone.update(title, targetDate, done);
		return milestone;
	}

	public void deleteMilestone(long cardId, long milestoneId) {
		LearningCard card = find(cardId);
		card.removeMilestone(findMilestone(cardId, milestoneId));
	}

	public LearningNote addNote(long cardId, String text) {
		LearningCard card = find(cardId);
		return notes.save(card.addNote(text, timeService.now().toInstant()));
	}

	public void deleteNote(long cardId, long noteId) {
		LearningCard card = find(cardId);
		LearningNote note = notes.findByIdAndCardId(noteId, cardId)
				.orElseThrow(() -> new NotFoundException("Note " + noteId + " not found on learning card " + cardId));
		card.removeNote(note);
	}

	private LearningCard find(long id) {
		return cards.findById(id).orElseThrow(() -> new NotFoundException("Learning card " + id + " not found"));
	}

	/** A milestone of another card is reported like a missing one (BR-9, A-4). */
	private LearningMilestone findMilestone(long cardId, long milestoneId) {
		return milestones.findByIdAndCardId(milestoneId, cardId)
				.orElseThrow(() -> new NotFoundException(
						"Milestone " + milestoneId + " not found on learning card " + cardId));
	}

	/** {@code size()} goes through the unmodifiable view to the lazy collection and loads it. */
	private static LearningCard loaded(LearningCard card) {
		card.getMilestones().size();
		card.getNotes().size();
		return card;
	}

}
