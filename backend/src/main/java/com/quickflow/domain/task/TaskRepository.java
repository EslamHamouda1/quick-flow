package com.quickflow.domain.task;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

	/** Due before today, not DONE, not archived, oldest due date first (FR-01.7). */
	@Query("""
			select t from Task t
			where t.dueDate < :today and t.status <> com.quickflow.domain.task.TaskStatus.DONE and t.archived = false
			order by t.dueDate asc, t.id asc""")
	List<Task> findOverdue(@Param("today") LocalDate today);

}
