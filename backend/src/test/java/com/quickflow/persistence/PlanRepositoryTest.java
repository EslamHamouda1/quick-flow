package com.quickflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.quickflow.domain.plan.Plan;
import com.quickflow.domain.plan.PlanItem;
import com.quickflow.domain.plan.PlanItemRef;
import com.quickflow.domain.plan.PlanRepository;
import com.quickflow.domain.plan.PlanSourceType;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskRepository;
import com.quickflow.domain.task.TaskStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

@DataJpaTest
class PlanRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	private static final Instant START = Instant.parse("2026-09-25T06:00:00Z");

	private static final Instant END = Instant.parse("2026-09-25T10:00:00Z");

	@Autowired
	private PlanRepository plans;

	@Autowired
	private TaskRepository tasks;

	@Autowired
	private EntityManager em;

	private Plan plan(String title, List<PlanItemRef> refs) {
		Plan plan = new Plan(title, 90, START, END, 1, refs, NOW);
		for (PlanItem item : plan.getItems()) {
			item.refreshTitle("Source " + item.getSourceType() + " " + item.getSourceId());
		}
		return plans.saveAndFlush(plan);
	}

	private long planItemRows() {
		return ((Number) em.createNativeQuery("select count(*) from plan_item").getSingleResult()).longValue();
	}

	@Test
	@DisplayName("FR-07.2: a plan's items are saved and loaded with the plan, in order")
	void fr07_2_itemsSavedAndLoadedWithPlan() {
		Plan saved = plan("  Morning focus  ", List.of(new PlanItemRef(PlanSourceType.TASK, 11L),
				new PlanItemRef(PlanSourceType.HABIT, 22L), new PlanItemRef(PlanSourceType.LEARNING_RESOURCE, 33L)));
		Long id = saved.getId();
		em.flush();
		em.clear();

		Plan reloaded = plans.findById(id).orElseThrow();
		assertThat(reloaded.getTitle()).isEqualTo("Morning focus");
		assertThat(reloaded.getEstimatedDurationMinutes()).isEqualTo(90);
		assertThat(reloaded.getStartDateTime()).isEqualTo(START);
		assertThat(reloaded.getEndDateTime()).isEqualTo(END);
		assertThat(reloaded.getPriorityOrder()).isEqualTo(1);
		assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
		assertThat(reloaded.getItems())
				.extracting(PlanItem::getSourceType, PlanItem::getSourceId, PlanItem::getSourceTitle, PlanItem::isDone)
				.containsExactly(tuple(PlanSourceType.TASK, 11L, "Source TASK 11", false),
						tuple(PlanSourceType.HABIT, 22L, "Source HABIT 22", false),
						tuple(PlanSourceType.LEARNING_RESOURCE, 33L, "Source LEARNING_RESOURCE 33", false));
		assertThat(reloaded.getItems()).allSatisfy(item -> {
			assertThat(item.getId()).isNotNull();
			assertThat(item.getPlanId()).isEqualTo(id);
		});
		assertThat(reloaded.totalItems()).isEqualTo(3);
		assertThat(reloaded.doneItems()).isZero();
		assertThat(planItemRows()).isEqualTo(3);
	}

	@Test
	@DisplayName("FR-07.2: the same source twice in one plan is rejected by the unique constraint")
	void fr07_2_duplicateSourceInPlanRejectedByConstraint() {
		Plan saved = plan("Morning focus", List.of(new PlanItemRef(PlanSourceType.TASK, 11L)));
		Long id = saved.getId();
		em.flush();
		em.clear();

		assertThatThrownBy(() -> {
			em.createNativeQuery(
					"insert into plan_item (plan_id, source_type, source_id, source_title, done) values (?, ?, ?, ?, false)")
					.setParameter(1, id)
					.setParameter(2, PlanSourceType.TASK.name())
					.setParameter(3, 11L)
					.setParameter(4, "duplicate")
					.executeUpdate();
			em.flush();
		}).isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
	}

	@Test
	@DisplayName("FR-07.2: the same source may be referenced by two different plans")
	void fr07_2_sameSourceInTwoPlansAllowed() {
		Plan first = plan("First", List.of(new PlanItemRef(PlanSourceType.TASK, 11L)));
		Plan second = plan("Second", List.of(new PlanItemRef(PlanSourceType.TASK, 11L)));
		em.flush();
		em.clear();

		assertThat(plans.findById(first.getId()).orElseThrow().getItems())
				.extracting(PlanItem::getSourceType, PlanItem::getSourceId)
				.containsExactly(tuple(PlanSourceType.TASK, 11L));
		assertThat(plans.findById(second.getId()).orElseThrow().getItems())
				.extracting(PlanItem::getSourceType, PlanItem::getSourceId)
				.containsExactly(tuple(PlanSourceType.TASK, 11L));
		assertThat(planItemRows()).isEqualTo(2);
	}

	@Test
	@DisplayName("BR-14: deleting a plan deletes its items")
	void br14_deletingPlanDeletesItems() {
		Plan saved = plan("Morning focus", List.of(new PlanItemRef(PlanSourceType.TASK, 11L),
				new PlanItemRef(PlanSourceType.HABIT, 22L)));
		Plan other = plan("Other", List.of(new PlanItemRef(PlanSourceType.HABIT, 22L)));
		Long id = saved.getId();
		em.flush();
		em.clear();
		assertThat(planItemRows()).isEqualTo(3);

		plans.delete(plans.findById(id).orElseThrow());
		plans.flush();
		em.clear();

		assertThat(plans.findById(id)).isEmpty();
		assertThat(((Number) em.createNativeQuery("select count(*) from plan_item where plan_id = ?")
				.setParameter(1, id).getSingleResult()).longValue()).isZero();
		assertThat(planItemRows()).isEqualTo(1);
		assertThat(plans.count()).isEqualTo(1);
		assertThat(plans.findById(other.getId()).orElseThrow().getItems()).hasSize(1);
	}

	@Test
	@DisplayName("FR-07.6: a plan item survives the deletion of its source (no foreign key to source tables)")
	void fr07_6_itemSurvivesSourceDeletion() {
		Task task = tasks.saveAndFlush(new Task("Write report", null, TaskStatus.TODO, TaskPriority.MEDIUM, null, NOW));
		Long taskId = task.getId();
		Plan plan = new Plan("Morning focus", 90, START, END, 1,
				List.of(new PlanItemRef(PlanSourceType.TASK, taskId)), NOW);
		plan.getItems().get(0).refreshTitle("Write report");
		Long id = plans.saveAndFlush(plan).getId();
		em.flush();
		em.clear();

		tasks.delete(tasks.findById(taskId).orElseThrow());
		tasks.flush();
		em.clear();

		assertThat(tasks.findById(taskId)).isEmpty();
		assertThat(plans.findById(id).orElseThrow().getItems())
				.extracting(PlanItem::getSourceType, PlanItem::getSourceId, PlanItem::getSourceTitle, PlanItem::isDone)
				.containsExactly(tuple(PlanSourceType.TASK, taskId, "Write report", false));
		assertThat(planItemRows()).isEqualTo(1);
	}

}
