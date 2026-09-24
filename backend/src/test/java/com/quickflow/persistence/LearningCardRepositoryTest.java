package com.quickflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.quickflow.domain.learning.LearningCard;
import com.quickflow.domain.learning.LearningCardRepository;
import com.quickflow.domain.learning.LearningMilestone;
import com.quickflow.domain.learning.LearningMilestoneRepository;
import com.quickflow.domain.learning.LearningNote;
import com.quickflow.domain.learning.LearningNoteRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
class LearningCardRepositoryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

	private static final Instant BASE = Instant.parse("2026-09-20T08:00:00Z");

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	@Autowired
	private LearningCardRepository cards;

	@Autowired
	private LearningMilestoneRepository milestones;

	@Autowired
	private LearningNoteRepository notes;

	@Autowired
	private EntityManager em;

	private int created;

	private LearningCard card(String title) {
		Instant createdAt = BASE.plusSeconds(3600L * created++);
		return card(title, createdAt);
	}

	private LearningCard card(String title, Instant createdAt) {
		return cards.saveAndFlush(new LearningCard(title, null, createdAt));
	}

	@Test
	@DisplayName("BR-9: a milestone without a learning card is rejected")
	void br9_milestoneWithoutCardRejected() {
		assertThatThrownBy(() -> milestones.saveAndFlush(new LearningMilestone(null, "x", null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("BR-9: a milestone or note is found only through its own learning card")
	void br9_milestoneFoundOnlyByItsOwnCard() {
		LearningCard own = card("Spring");
		LearningCard other = card("Kotlin");
		LearningMilestone milestone = own.addMilestone("Read chapter 1", TODAY);
		LearningNote note = own.addNote("Beans are singletons by default", NOW);
		em.flush();
		Long milestoneId = milestone.getId();
		Long noteId = note.getId();
		em.clear();

		assertThat(milestones.findByIdAndCardId(milestoneId, own.getId())).get()
				.extracting(LearningMilestone::getTitle).isEqualTo("Read chapter 1");
		assertThat(milestones.findByIdAndCardId(milestoneId, other.getId())).isEmpty();

		assertThat(notes.findByIdAndCardId(noteId, own.getId())).get()
				.extracting(LearningNote::getText).isEqualTo("Beans are singletons by default");
		assertThat(notes.findByIdAndCardId(noteId, other.getId())).isEmpty();
	}

	@Test
	@DisplayName("BR-14: deleting a learning card deletes its milestones and notes")
	void br14_deletingCardDeletesMilestonesAndNotes() {
		LearningCard card = card("Spring");
		LearningMilestone m1 = card.addMilestone("Read chapter 1", TODAY);
		LearningMilestone m2 = card.addMilestone("Read chapter 2", null);
		LearningNote n1 = card.addNote("first", NOW);
		LearningNote n2 = card.addNote("second", NOW.plusSeconds(60));
		em.flush();
		Long id = card.getId();
		Long m1Id = m1.getId();
		Long m2Id = m2.getId();
		Long n1Id = n1.getId();
		Long n2Id = n2.getId();
		em.clear();

		cards.delete(cards.findById(id).orElseThrow());
		cards.flush();
		em.clear();

		assertThat(cards.findById(id)).isEmpty();
		assertThat(milestones.findById(m1Id)).isEmpty();
		assertThat(milestones.findById(m2Id)).isEmpty();
		assertThat(notes.findById(n1Id)).isEmpty();
		assertThat(notes.findById(n2Id)).isEmpty();
		assertThat(cards.count()).isZero();
		assertThat(milestones.count()).isZero();
		assertThat(notes.count()).isZero();
	}

	@Test
	@DisplayName("Orphan removal: removing a milestone from its card deletes the row")
	void orphanRemoval_removedMilestoneRowDeleted() {
		LearningCard card = card("Spring");
		card.addMilestone("Read chapter 1", TODAY);
		card.addMilestone("Read chapter 2", null);
		em.flush();
		Long id = card.getId();
		em.clear();

		LearningCard reloaded = cards.findById(id).orElseThrow();
		LearningMilestone removed = reloaded.getMilestones().get(0);
		Long removedId = removed.getId();
		reloaded.removeMilestone(removed);
		em.flush();
		em.clear();

		assertThat(milestones.findById(removedId)).isEmpty();
		assertThat(milestones.count()).isEqualTo(1);
		assertThat(cards.findById(id).orElseThrow().getMilestones()).extracting(LearningMilestone::getTitle)
				.containsExactly("Read chapter 2");
	}

	@Test
	@DisplayName("FR-06.3: a reloaded card counts its done and total milestones")
	void fr06_3_milestoneDoneAndTotalCounts() {
		LearningCard card = card("Spring");
		LearningMilestone m1 = card.addMilestone("Read chapter 1", TODAY);
		card.addMilestone("Read chapter 2", null);
		LearningMilestone m3 = card.addMilestone("Read chapter 3", TODAY.plusDays(7));
		m1.update("Read chapter 1", TODAY, true);
		m3.update("Read chapter 3", TODAY.plusDays(7), true);
		em.flush();
		Long id = card.getId();
		em.clear();

		LearningCard reloaded = cards.findById(id).orElseThrow();
		assertThat(reloaded.milestonesDone()).isEqualTo(2);
		assertThat(reloaded.milestonesTotal()).isEqualTo(3);

		LearningCard empty = card("Kotlin");
		em.flush();
		em.clear();
		LearningCard reloadedEmpty = cards.findById(empty.getId()).orElseThrow();
		assertThat(reloadedEmpty.milestonesDone()).isZero();
		assertThat(reloadedEmpty.milestonesTotal()).isZero();
	}

	@Test
	@DisplayName("FR-05.2: learning cards are listed newest first, ties broken by id descending")
	void fr05_2_listNewestFirst() {
		card("oldest", BASE);
		card("newest", BASE.plusSeconds(7200));
		card("middle", BASE.plusSeconds(3600));
		card("tie-first", BASE.plusSeconds(1800));
		card("tie-second", BASE.plusSeconds(1800));
		em.flush();
		em.clear();

		assertThat(cards.findAllByOrderByCreatedAtDescIdDesc()).extracting(LearningCard::getTitle)
				.containsExactly("newest", "middle", "tie-second", "tie-first", "oldest");
	}

	@Test
	@DisplayName("FR-06.2: a reloaded card has notes ordered by creation time and milestones by id")
	void fr06_2_notesOrderedByCreatedAt() {
		LearningCard card = card("Spring");
		card.addNote("third", NOW.plusSeconds(120));
		card.addNote("first", NOW);
		card.addNote("second", NOW.plusSeconds(60));
		LearningMilestone m1 = card.addMilestone("A", TODAY.plusDays(5));
		LearningMilestone m2 = card.addMilestone("B", TODAY);
		LearningMilestone m3 = card.addMilestone("C", null);
		em.flush();
		Long id = card.getId();
		em.clear();

		LearningCard reloaded = cards.findById(id).orElseThrow();
		assertThat(reloaded.getNotes()).extracting(LearningNote::getText)
				.containsExactly("first", "second", "third");
		assertThat(reloaded.getNotes()).extracting(LearningNote::getCreatedAt)
				.containsExactly(NOW, NOW.plusSeconds(60), NOW.plusSeconds(120));
		assertThat(reloaded.getMilestones()).extracting(LearningMilestone::getId)
				.containsExactly(m1.getId(), m2.getId(), m3.getId());
		assertThat(reloaded.getMilestones()).extracting(LearningMilestone::getTitle)
				.containsExactly("A", "B", "C");
	}

}
