package com.quickflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskQuery;
import com.quickflow.domain.task.TaskRepository;
import com.quickflow.domain.task.TaskSort;
import com.quickflow.domain.task.TaskSpecifications;
import com.quickflow.domain.task.TaskStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest
class TaskRepositoryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

	private static final Instant BASE = Instant.parse("2026-09-20T08:00:00Z");

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	@Autowired
	private TaskRepository repository;

	@Autowired
	private EntityManager em;

	private int created;

	private Task task(String title, TaskStatus status, TaskPriority priority, LocalDate dueDate) {
		Instant createdAt = BASE.plusSeconds(3600L * created++);
		return new Task(title, null, status, priority, dueDate, createdAt);
	}

	private Task task(String title) {
		return task(title, TaskStatus.TODO, TaskPriority.MEDIUM, null);
	}

	private Task tagged(String title, String... tags) {
		Task task = task(title);
		task.addTags(List.of(tags), NOW);
		return task;
	}

	private List<Task> saveAll(Task... tasks) {
		List<Task> saved = repository.saveAll(List.of(tasks));
		repository.flush();
		return saved;
	}

	private void flushAndClear() {
		em.flush();
		em.clear();
	}

	private static TaskQuery defaultQuery() {
		return new TaskQuery(null, null, null, null, null, false, null, TaskSort.CREATED_AT, Sort.Direction.DESC);
	}

	private static TaskQuery search(String q) {
		return new TaskQuery(q, null, null, null, null, false, null, TaskSort.CREATED_AT, Sort.Direction.DESC);
	}

	private static TaskQuery sorted(TaskSort sort, Sort.Direction direction) {
		return new TaskQuery(null, null, null, null, null, false, null, sort, direction);
	}

	private static TaskQuery byTag(String tag) {
		return new TaskQuery(null, null, null, null, null, false, tag, TaskSort.CREATED_AT, Sort.Direction.DESC);
	}

	private List<Task> list(TaskQuery query) {
		return repository.findAll(TaskSpecifications.matching(query));
	}

	private long tagRowCount(Long taskId) {
		Number count = (Number) em.createNativeQuery("select count(*) from task_tag where task_id = ?1")
				.setParameter(1, taskId)
				.getSingleResult();
		return count.longValue();
	}

	@Test
	@DisplayName("FR-02.1: title search ignores case and treats % and _ literally")
	void fr02_1_titleSearchIgnoresCase() {
		saveAll(task("Weekly REPORT"), task("report draft"), task("Groceries"), task("50% done"),
				task("500 done"));

		assertThat(list(search("report"))).extracting(Task::getTitle)
				.containsExactly("report draft", "Weekly REPORT");
		assertThat(list(search("50%"))).extracting(Task::getTitle).containsExactly("50% done");
	}

	@Test
	@DisplayName("FR-02.2: status and priority filters combine")
	void fr02_2_filtersCombine() {
		saveAll(task("todo high", TaskStatus.TODO, TaskPriority.HIGH, null),
				task("todo low", TaskStatus.TODO, TaskPriority.LOW, null),
				task("done high", TaskStatus.DONE, TaskPriority.HIGH, null),
				task("todo high 2", TaskStatus.TODO, TaskPriority.HIGH, null));

		TaskQuery query = new TaskQuery(null, TaskStatus.TODO, TaskPriority.HIGH, null, null, false, null,
				TaskSort.CREATED_AT, Sort.Direction.DESC);

		assertThat(list(query)).extracting(Task::getTitle).containsExactly("todo high 2", "todo high");
	}

	@Test
	@DisplayName("FR-02.2: due date range is inclusive and excludes tasks without a due date")
	void fr02_2_dueRangeInclusive() {
		saveAll(task("before", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 24)),
				task("from", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 25)),
				task("inside", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 27)),
				task("to", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 30)),
				task("after", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 10, 1)),
				task("no due", TaskStatus.TODO, TaskPriority.MEDIUM, null));

		TaskQuery query = new TaskQuery(null, null, null, LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 30),
				false, null, TaskSort.CREATED_AT, Sort.Direction.DESC);

		assertThat(list(query)).extracting(Task::getTitle).containsExactly("to", "inside", "from");
	}

	@Test
	@DisplayName("FR-02.3: sort by due date ascending puts tasks without a due date last")
	void fr02_3_sortDueDateNullsLastAsc() {
		saveAll(task("sep 30", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 30)),
				task("no due", TaskStatus.TODO, TaskPriority.MEDIUM, null),
				task("sep 25", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 25)),
				task("sep 28", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 28)));

		assertThat(list(sorted(TaskSort.DUE_DATE, Sort.Direction.ASC))).extracting(Task::getTitle)
				.containsExactly("sep 25", "sep 28", "sep 30", "no due");
	}

	@Test
	@DisplayName("FR-02.3: sort by due date descending still puts tasks without a due date last")
	void fr02_3_sortDueDateNullsLastDesc() {
		saveAll(task("sep 30", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 30)),
				task("no due", TaskStatus.TODO, TaskPriority.MEDIUM, null),
				task("sep 25", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 25)),
				task("sep 28", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 9, 28)));

		assertThat(list(sorted(TaskSort.DUE_DATE, Sort.Direction.DESC))).extracting(Task::getTitle)
				.containsExactly("sep 30", "sep 28", "sep 25", "no due");
	}

	@Test
	@DisplayName("FR-02.3: sort by creation time in both directions")
	void fr02_3_sortCreatedAtBothDirections() {
		Task first = task("first");
		Task second = task("second");
		Task third = task("third");
		saveAll(second, third, first);

		assertThat(list(sorted(TaskSort.CREATED_AT, Sort.Direction.DESC))).extracting(Task::getTitle)
				.containsExactly("third", "second", "first");
		assertThat(list(sorted(TaskSort.CREATED_AT, Sort.Direction.ASC))).extracting(Task::getTitle)
				.containsExactly("first", "second", "third");
	}

	@Test
	@DisplayName("BR-4, FR-01.6: archived tasks are excluded by default")
	void br4_archivedExcludedByDefault() {
		Task archived = task("archived");
		archived.archive(BASE.plusSeconds(86_400));
		saveAll(task("active"), archived);

		assertThat(list(defaultQuery())).extracting(Task::getTitle).containsExactly("active");
	}

	@Test
	@DisplayName("BR-4, FR-01.6: archived=true lists only archived tasks")
	void br4_archivedTrueListsArchived() {
		Task archived = task("archived");
		archived.archive(BASE.plusSeconds(86_400));
		saveAll(task("active"), archived);

		TaskQuery query = new TaskQuery(null, null, null, null, null, true, null, TaskSort.CREATED_AT,
				Sort.Direction.DESC);

		assertThat(list(query)).extracting(Task::getTitle).containsExactly("archived");
	}

	@Test
	@DisplayName("FR-01.7: overdue lists open, unarchived tasks due before today, by due date ascending")
	void fr01_7_overdueQuery() {
		Task archived = task("archived", TaskStatus.TODO, TaskPriority.MEDIUM, TODAY.minusDays(1));
		archived.archive(BASE.plusSeconds(86_400));
		saveAll(task("yesterday", TaskStatus.TODO, TaskPriority.MEDIUM, TODAY.minusDays(1)),
				task("today", TaskStatus.TODO, TaskPriority.MEDIUM, TODAY),
				task("done", TaskStatus.DONE, TaskPriority.MEDIUM, TODAY.minusDays(1)),
				archived,
				task("no due", TaskStatus.TODO, TaskPriority.MEDIUM, null),
				task("last week", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY.minusDays(7)));

		assertThat(repository.findOverdue(TODAY)).extracting(Task::getTitle)
				.containsExactly("last week", "yesterday");
	}

	@Test
	@DisplayName("BR-14: a deleted task is no longer returned")
	void br14_deletedTaskNotReturned() {
		Task doomed = repository.saveAndFlush(task("doomed", TaskStatus.TODO, TaskPriority.MEDIUM,
				TODAY.minusDays(1)));
		repository.saveAndFlush(task("kept"));
		Long id = doomed.getId();

		repository.delete(doomed);
		repository.flush();

		assertThat(list(defaultQuery())).extracting(Task::getTitle).containsExactly("kept");
		assertThat(repository.findOverdue(TODAY)).isEmpty();
		assertThat(repository.findById(id)).isEmpty();
	}

	@Test
	@DisplayName("FR-008: tags survive flush and clear and reload trimmed, lower case and sorted")
	void fr008_tagsSurviveReload() {
		Task task = repository.saveAndFlush(tagged("tagged", " Work ", "home"));
		Long id = task.getId();
		flushAndClear();

		Task reloaded = repository.findById(id).orElseThrow();

		assertThat(reloaded.getTags()).containsExactly("home", "work");
	}

	@Test
	@DisplayName("AC-US1-2: tag filter lists only tasks with that tag")
	void acUs1_2_tagFilterListsOnlyTaggedTasks() {
		saveAll(tagged("work 1", "work"), tagged("home 1", "home"), task("untagged"), tagged("work 2", "work"));
		flushAndClear();

		assertThat(list(byTag("work"))).extracting(Task::getTitle).containsExactly("work 2", "work 1");
	}

	@Test
	@DisplayName("BR-T2: tag filter ignores case and surrounding spaces")
	void brT2_tagFilterIgnoresCaseAndSpaces() {
		saveAll(tagged("work", "work"), tagged("home", "home"));
		flushAndClear();

		assertThat(list(byTag("WORK"))).extracting(Task::getTitle).containsExactly("work");
		assertThat(list(byTag(" work "))).extracting(Task::getTitle).containsExactly("work");
	}

	@Test
	@DisplayName("BR-T2: tag filter matches the whole tag, not a prefix")
	void brT2_tagFilterMatchesWholeTag() {
		saveAll(tagged("workshop", "workshop"), tagged("work", "work"));
		flushAndClear();

		assertThat(list(byTag("work"))).extracting(Task::getTitle).containsExactly("work");
	}

	@Test
	@DisplayName("AC-US1-2: a task with several tags is listed once")
	void acUs1_2_taskWithSeveralTagsListedOnce() {
		saveAll(tagged("many", "work", "urgent", "home"), tagged("one", "work"));
		flushAndClear();

		assertThat(list(byTag("work"))).extracting(Task::getTitle).containsExactly("one", "many");
	}

	@Test
	@DisplayName("AC-US1-2: tag filter combines with the status filter by AND")
	void acUs1_2_tagFilterCombinesWithStatus() {
		Task todoWork = task("todo work", TaskStatus.TODO, TaskPriority.MEDIUM, null);
		todoWork.addTags(List.of("work"), NOW);
		Task doneWork = task("done work", TaskStatus.DONE, TaskPriority.MEDIUM, null);
		doneWork.addTags(List.of("work"), NOW);
		Task todoHome = task("todo home", TaskStatus.TODO, TaskPriority.MEDIUM, null);
		todoHome.addTags(List.of("home"), NOW);
		saveAll(todoWork, doneWork, todoHome);
		flushAndClear();

		TaskQuery query = new TaskQuery(null, TaskStatus.TODO, null, null, null, false, "work",
				TaskSort.CREATED_AT, Sort.Direction.DESC);

		assertThat(list(query)).extracting(Task::getTitle).containsExactly("todo work");
	}

	@Test
	@DisplayName("FR-004, BR-4: tag filter excludes archived tasks by default")
	void fr004_tagFilterExcludesArchived() {
		Task archived = tagged("archived work", "work");
		archived.archive(NOW);
		saveAll(tagged("active work", "work"), archived);
		flushAndClear();

		assertThat(list(byTag("work"))).extracting(Task::getTitle).containsExactly("active work");
	}

	@Test
	@DisplayName("AC-US1-3: a removed tag no longer matches")
	void acUs1_3_removedTagNoLongerMatches() {
		Task task = repository.saveAndFlush(tagged("was work", "work", "home"));
		Long id = task.getId();
		flushAndClear();

		Task reloaded = repository.findById(id).orElseThrow();
		reloaded.removeTag("work", NOW);
		repository.saveAndFlush(reloaded);
		flushAndClear();

		assertThat(list(byTag("work"))).isEmpty();
		assertThat(list(byTag("home"))).extracting(Task::getTitle).containsExactly("was work");
	}

	@Test
	@DisplayName("BR-T4, BR-14: deleting a tagged task removes its tag rows and it is never listed by tag")
	void brT4_deletingTaggedTaskRemovesTagRows() {
		Task doomed = repository.saveAndFlush(tagged("doomed", "work", "home"));
		repository.saveAndFlush(tagged("kept", "work"));
		Long id = doomed.getId();
		flushAndClear();
		assertThat(tagRowCount(id)).isEqualTo(2);

		repository.delete(repository.findById(id).orElseThrow());
		flushAndClear();

		assertThat(tagRowCount(id)).isZero();
		assertThat(list(byTag("work"))).extracting(Task::getTitle).containsExactly("kept");
		assertThat(list(byTag("home"))).isEmpty();
	}

}
