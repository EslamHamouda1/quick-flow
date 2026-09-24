package com.quickflow.domain.habit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Explicit queries: a derived {@code HabitId} path would resolve to {@link HabitCompletion#getHabitId()} instead of
 * {@code habit.id}.
 */
public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, Long> {

	@Query("""
			select count(c) > 0 from HabitCompletion c
			where c.habit.id = :habitId and c.completionDate = :completionDate""")
	boolean existsByHabitIdAndCompletionDate(@Param("habitId") Long habitId,
			@Param("completionDate") LocalDate completionDate);

	/** Newest date first (A-8). */
	@Query("select c from HabitCompletion c where c.habit.id = :habitId order by c.completionDate desc")
	List<HabitCompletion> findByHabitIdOrderByCompletionDateDesc(@Param("habitId") Long habitId);

	@Query("select c from HabitCompletion c where c.habit.id = :habitId and c.completionDate = :completionDate")
	Optional<HabitCompletion> findByHabitIdAndCompletionDate(@Param("habitId") Long habitId,
			@Param("completionDate") LocalDate completionDate);

	@Query("select c.completionDate from HabitCompletion c where c.habit.id = :habitId")
	List<LocalDate> findDatesByHabitId(@Param("habitId") Long habitId);

}
