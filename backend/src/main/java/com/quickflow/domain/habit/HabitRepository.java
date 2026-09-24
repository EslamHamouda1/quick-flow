package com.quickflow.domain.habit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<Habit, Long> {

	/** All habits, oldest first (A-8). */
	List<Habit> findAllByOrderByCreatedAtAscIdAsc();

	List<Habit> findByActiveOrderByCreatedAtAscIdAsc(boolean active);

}
