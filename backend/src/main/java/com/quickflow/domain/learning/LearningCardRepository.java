package com.quickflow.domain.learning;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningCardRepository extends JpaRepository<LearningCard, Long> {

	/** All cards, newest first (A-5). */
	List<LearningCard> findAllByOrderByCreatedAtDescIdDesc();

	long countByStatus(LearningStatus status);

}
