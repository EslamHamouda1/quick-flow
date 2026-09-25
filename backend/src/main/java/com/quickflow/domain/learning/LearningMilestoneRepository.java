package com.quickflow.domain.learning;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Explicit query: a derived {@code CardId} path would resolve to {@link LearningMilestone#getCardId()} instead of
 * {@code card.id}.
 */
public interface LearningMilestoneRepository extends JpaRepository<LearningMilestone, Long> {

	/** Empty for a milestone of another card (BR-9). */
	@Query("select m from LearningMilestone m where m.id = :id and m.card.id = :cardId")
	Optional<LearningMilestone> findByIdAndCardId(@Param("id") Long id, @Param("cardId") Long cardId);

	long countByDoneTrue();

}
