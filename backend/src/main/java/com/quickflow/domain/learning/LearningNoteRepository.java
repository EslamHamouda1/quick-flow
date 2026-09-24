package com.quickflow.domain.learning;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Explicit query, as in {@link LearningMilestoneRepository}. */
public interface LearningNoteRepository extends JpaRepository<LearningNote, Long> {

	/** Empty for a note of another card (BR-9). */
	@Query("select n from LearningNote n where n.id = :id and n.card.id = :cardId")
	Optional<LearningNote> findByIdAndCardId(@Param("id") Long id, @Param("cardId") Long cardId);

}
