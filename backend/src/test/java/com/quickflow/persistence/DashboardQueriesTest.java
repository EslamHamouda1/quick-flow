package com.quickflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.quickflow.domain.learning.LearningCard;
import com.quickflow.domain.learning.LearningCardRepository;
import com.quickflow.domain.learning.LearningMilestone;
import com.quickflow.domain.learning.LearningMilestoneRepository;
import com.quickflow.domain.learning.LearningStatus;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskRepository;
import com.quickflow.domain.task.TaskStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest
class DashboardQueriesTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

	private static final Instant BASE = Instant.parse("2026-09-20T08:00:00Z");

	private static final Instant FROM = Instant.parse("2026-09-24T00:00:00Z");

	private static final Instant TO = Instant.parse("2026-09-25T00:00:00Z");

	@Autowired
	private TaskRepository tasks;

	@Autowired
	private LearningCardRepository cards;

	@Autowired
	private LearningMilestoneRepository milestones;

	@Autowired
	private EntityManager em;

	private int created;

	private Task task(String title, TaskStatus status, LocalDate dueDate) {
		Instant createdAt = BASE.plusSeconds(3600L * created++);
		return tasks.save(new Task(title, null, status, TaskPriority.MEDIUM, dueDate, createdAt));
	}

	private Task completed(String title, Instant completedAt) {
		Task task = task(title, TaskStatus.TODO, null);
		task.complete(completedAt);
		return task;
	}

	private Task archived(Task task) {
		task.archive(BASE.plusSeconds(86_400));
		return task;
	}

	private void flushAndClear() {
		em.flush();
		em.clear();
	}

	private LearningCard card(String title, LearningStatus status) {
		LearningCard card = new LearningCard(title, null, BASE.plusSeconds(3600L * created++));
		card.update(title, null, status);
		return cards.save(card);
	}

	@Test
	@DisplayName("FR-09.1: tasks due on a date are that date only, any status, by id ascending")
	void fr09_1_dueOnDateReturnsOnlyThatDate() {
		Task first = task("today todo", TaskStatus.TODO, TODAY);
		task("yesterday", TaskStatus.TODO, TODAY.minusDays(1));
		Task second = task("today done", TaskStatus.DONE, TODAY);
		task("tomorrow", TaskStatus.TODO, TODAY.plusDays(1));
		Task third = task("today in progress", TaskStatus.IN_PROGRESS, TODAY);
		task("no due", TaskStatus.TODO, null);
		flushAndClear();

		assertThat(tasks.findByDueDateAndArchivedFalseOrderByIdAsc(TODAY)).extracting(Task::getId)
				.containsExactly(first.getId(), second.getId(), third.getId());
	}

	@Test
	@DisplayName("BR-4: tasks due on a date exclude archived tasks")
	void br4_dueOnDateExcludesArchived() {
		task("active", TaskStatus.TODO, TODAY);
		archived(task("archived", TaskStatus.TODO, TODAY));
		flushAndClear();

		assertThat(tasks.findByDueDateAndArchivedFalseOrderByIdAsc(TODAY)).extracting(Task::getTitle)
				.containsExactly("active");
	}

	@Test
	@DisplayName("FR-09.1: completed between includes from, excludes to, newest completion first")
	void fr09_1_completedBetweenIncludesFromExcludesTo() {
		completed("before", FROM.minusSeconds(1));
		completed("at from", FROM);
		completed("midday", FROM.plusSeconds(12 * 3600));
		completed("last second", TO.minusSeconds(1));
		completed("at to", TO);
		flushAndClear();

		assertThat(tasks.findCompletedBetween(FROM, TO)).extracting(Task::getTitle)
				.containsExactly("last second", "midday", "at from");
	}

	@Test
	@DisplayName("BR-4: completed between excludes archived tasks")
	void br4_completedBetweenExcludesArchived() {
		completed("active", FROM.plusSeconds(60));
		archived(completed("archived", FROM.plusSeconds(120)));
		flushAndClear();

		assertThat(tasks.findCompletedBetween(FROM, TO)).extracting(Task::getTitle).containsExactly("active");
	}

	@Test
	@DisplayName("FR-09.1: completed between returns only DONE tasks")
	void fr09_1_completedBetweenOnlyDone() {
		completed("done", FROM.plusSeconds(60));
		Task reopened = completed("reopened", FROM.plusSeconds(120));
		reopened.changeStatus(TaskStatus.TODO, FROM.plusSeconds(180));
		flushAndClear();

		assertThat(tasks.findById(reopened.getId()).orElseThrow().getCompletedAt()).isNull();
		assertThat(tasks.findCompletedBetween(FROM, TO)).extracting(Task::getTitle).containsExactly("done");
	}

	@Test
	@DisplayName("AC-US5-3: non-archived and done counts exclude archived tasks")
	void acUs5_3_countNonArchivedAndDone() {
		task("todo", TaskStatus.TODO, null);
		task("in progress", TaskStatus.IN_PROGRESS, null);
		task("done 1", TaskStatus.DONE, null);
		task("done 2", TaskStatus.DONE, null);
		archived(task("archived done", TaskStatus.DONE, null));
		archived(task("archived todo", TaskStatus.TODO, null));
		flushAndClear();

		assertThat(tasks.countByArchivedFalse()).isEqualTo(4);
		assertThat(tasks.countByArchivedFalseAndStatus(TaskStatus.DONE)).isEqualTo(2);
		assertThat(tasks.countByArchivedFalseAndStatus(TaskStatus.TODO)).isEqualTo(1);
	}

	@Test
	@DisplayName("BR-14: a deleted task is never listed or counted")
	void br14_deletedTaskNeverCounted() {
		task("kept", TaskStatus.TODO, null);
		Task doomed = completed("doomed", FROM.plusSeconds(60));
		task("doomed due", TaskStatus.TODO, TODAY);
		flushAndClear();

		tasks.delete(tasks.findById(doomed.getId()).orElseThrow());
		tasks.deleteAll(tasks.findByDueDateAndArchivedFalseOrderByIdAsc(TODAY));
		flushAndClear();

		assertThat(tasks.findCompletedBetween(FROM, TO)).isEmpty();
		assertThat(tasks.findByDueDateAndArchivedFalseOrderByIdAsc(TODAY)).isEmpty();
		assertThat(tasks.countByArchivedFalse()).isEqualTo(1);
		assertThat(tasks.countByArchivedFalseAndStatus(TaskStatus.DONE)).isZero();
	}

	@Test
	@DisplayName("AC-US5-5: learning cards are counted per status")
	void acUs5_5_cardsCountedPerStatus() {
		card("a", LearningStatus.NOT_STARTED);
		card("b", LearningStatus.IN_PROGRESS);
		card("c", LearningStatus.IN_PROGRESS);
		card("d", LearningStatus.COMPLETED);
		card("e", LearningStatus.COMPLETED);
		card("f", LearningStatus.COMPLETED);
		flushAndClear();

		assertThat(cards.countByStatus(LearningStatus.NOT_STARTED)).isEqualTo(1);
		assertThat(cards.countByStatus(LearningStatus.IN_PROGRESS)).isEqualTo(2);
		assertThat(cards.countByStatus(LearningStatus.COMPLETED)).isEqualTo(3);
	}

	@Test
	@DisplayName("AC-US5-5: milestones are counted done and total across all cards")
	void acUs5_5_milestonesDoneAndTotal() {
		LearningCard spring = card("Spring", LearningStatus.IN_PROGRESS);
		LearningCard kotlin = card("Kotlin", LearningStatus.NOT_STARTED);
		LearningMilestone m1 = spring.addMilestone("Chapter 1", TODAY);
		spring.addMilestone("Chapter 2", null);
		LearningMilestone m3 = kotlin.addMilestone("Basics", null);
		m1.update("Chapter 1", TODAY, true);
		m3.update("Basics", null, true);
		kotlin.addMilestone("Coroutines", null);
		flushAndClear();

		assertThat(milestones.countByDoneTrue()).isEqualTo(2);
		assertThat(milestones.count()).isEqualTo(4);
	}

	@Test
	@DisplayName("BR-14: milestones of a deleted card are never counted")
	void br14_deletedCardMilestonesNeverCounted() {
		LearningCard doomed = card("Doomed", LearningStatus.IN_PROGRESS);
		LearningCard kept = card("Kept", LearningStatus.IN_PROGRESS);
		doomed.addMilestone("Gone done", null).update("Gone done", null, true);
		doomed.addMilestone("Gone open", null);
		kept.addMilestone("Kept done", null).update("Kept done", null, true);
		flushAndClear();

		cards.delete(cards.findById(doomed.getId()).orElseThrow());
		flushAndClear();

		assertThat(milestones.countByDoneTrue()).isEqualTo(1);
		assertThat(milestones.count()).isEqualTo(1);
		assertThat(cards.countByStatus(LearningStatus.IN_PROGRESS)).isEqualTo(1);
	}

}
