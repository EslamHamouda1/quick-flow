package com.quickflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import jakarta.persistence.EntityManager;

@DataJpaTest
class SchemaIndexesTest {

	@Autowired
	private EntityManager em;

	@SuppressWarnings("unchecked")
	private List<String> indexedColumns(String table, String index) {
		return em.createNativeQuery("""
				select c.column_name from information_schema.index_columns c
				where lower(c.table_name) = ?1 and lower(c.index_name) = ?2
				order by c.ordinal_position
				""")
			.setParameter(1, table)
			.setParameter(2, index)
			.getResultList()
			.stream()
			.map(name -> name.toString().toLowerCase())
			.toList();
	}

	@SuppressWarnings("unchecked")
	private List<String> uniqueConstraintColumns(String table) {
		return em.createNativeQuery("""
				select k.column_name from information_schema.table_constraints t
				join information_schema.key_column_usage k
				  on k.constraint_schema = t.constraint_schema and k.constraint_name = t.constraint_name
				where lower(t.table_name) = ?1 and t.constraint_type = 'UNIQUE'
				order by k.ordinal_position
				""")
			.setParameter(1, table)
			.getResultList()
			.stream()
			.map(name -> name.toString().toLowerCase())
			.toList();
	}

	@Test
	@DisplayName("NFR-1: task has an index on (archived, due_date) for the default task list")
	void nfr1_taskArchivedDueDateIndexExists() {
		assertThat(indexedColumns("task", "idx_task_archived_due_date")).containsExactly("archived", "due_date");
	}

	@Test
	@DisplayName("NFR-1: task has an index on status for the status filter")
	void nfr1_taskStatusIndexExists() {
		assertThat(indexedColumns("task", "idx_task_status")).containsExactly("status");
	}

	@Test
	@DisplayName("NFR-1: plan_item has an index on plan_id")
	void nfr1_planItemPlanIdIndexExists() {
		assertThat(indexedColumns("plan_item", "idx_plan_item_plan_id")).containsExactly("plan_id");
	}

	@Test
	@DisplayName("BR-7, NFR-1: habit_completion has a unique constraint on (habit_id, completion_date)")
	void br7_habitCompletionUniqueConstraintExists() {
		assertThat(uniqueConstraintColumns("habit_completion")).containsExactly("habit_id", "completion_date");
	}

}
