package com.quickflow.web.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskQuery;
import com.quickflow.domain.task.TaskService;
import com.quickflow.domain.task.TaskSort;
import com.quickflow.domain.task.TaskStatus;

@WebMvcTest(TaskController.class)
@Import(TaskControllerTest.FixedTime.class)
class TaskControllerTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	@TestConfiguration
	static class FixedTime {

		@Bean
		TimeService timeService() {
			return new TimeService(Clock.fixed(NOW, ZoneId.of("Africa/Cairo")));
		}

	}

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TaskService taskService;

	private static Task task(String title, TaskStatus status, TaskPriority priority, LocalDate dueDate) {
		Task task = new Task(title, null, status, priority, dueDate, NOW);
		ReflectionTestUtils.setField(task, "id", 7L);
		return task;
	}

	@Test
	@DisplayName("US1: POST /api/tasks creates a task and returns 201 with the task")
	void createTaskReturns201Task() {
		Task created = new Task("Write report", "Q3 numbers", null, TaskPriority.HIGH, LocalDate.of(2026, 9, 30), NOW);
		ReflectionTestUtils.setField(created, "id", 7L);
		given(this.taskService.create("Write report", "Q3 numbers", null, TaskPriority.HIGH, LocalDate.of(2026, 9, 30)))
			.willReturn(created);

		var result = assertThat(this.mvc.post().uri("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Write report","description":"Q3 numbers","priority":"HIGH","dueDate":"2026-09-30"}
						"""));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(7);
		json.extractingPath("$.title").isEqualTo("Write report");
		json.extractingPath("$.description").isEqualTo("Q3 numbers");
		json.extractingPath("$.status").isEqualTo("TODO");
		json.extractingPath("$.priority").isEqualTo("HIGH");
		json.extractingPath("$.dueDate").isEqualTo("2026-09-30");
		json.extractingPath("$.createdAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.archived").isEqualTo(false);
		json.extractingPath("$.overdue").isEqualTo(false);

		then(this.taskService).should()
			.create("Write report", "Q3 numbers", null, TaskPriority.HIGH, LocalDate.of(2026, 9, 30));
	}

	@Test
	@DisplayName("BR-1: a blank title is rejected with 400 and field title")
	void br1_blankTitleIs400WithTitleField() {
		var result = assertThat(this.mvc.post().uri("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("title");

		verifyNoInteractions(this.taskService);
	}

	@Test
	@DisplayName("BR-3: an unknown status in the body is rejected with 400 and field status")
	void br3_unknownStatusIs400() {
		var result = assertThat(this.mvc.post().uri("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Write report\",\"status\":\"FOO\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("status");

		verifyNoInteractions(this.taskService);
	}

	@Test
	@DisplayName("BR-3: an unknown status query parameter is rejected with 400 and field status")
	void br3_unknownStatusQueryParamIs400() {
		var result = assertThat(this.mvc.get().uri("/api/tasks?status=FOO"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("status");

		verifyNoInteractions(this.taskService);
	}

	@Test
	@DisplayName("US1: an unknown sort parameter is rejected with 400 and field sort")
	void unknownSortIs400() {
		var result = assertThat(this.mvc.get().uri("/api/tasks?sort=title"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("sort");

		verifyNoInteractions(this.taskService);
	}

	@Test
	@DisplayName("US1: GET /api/tasks/{id} for an unknown id is 404 problem")
	void getTaskUnknownIs404() {
		given(this.taskService.get(99L)).willThrow(new NotFoundException("Task 99 not found"));

		var result = assertThat(this.mvc.get().uri("/api/tasks/99"));
		result.hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.status").isEqualTo(404);
		result.bodyJson().extractingPath("$.detail").isEqualTo("Task 99 not found");
	}

	@Test
	@DisplayName("US1: GET /api/tasks binds the default query (not archived, createdAt desc)")
	void listTasksBindsQueryWithDefaults() {
		TaskQuery expected = new TaskQuery(null, null, null, null, null, false, TaskSort.CREATED_AT,
				Sort.Direction.DESC);
		given(this.taskService.list(expected)).willReturn(List.of(task("Write report", null, null, null)));

		var result = assertThat(this.mvc.get().uri("/api/tasks"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$[0].id").isEqualTo(7);
		result.bodyJson().extractingPath("$[0].title").isEqualTo("Write report");

		then(this.taskService).should().list(expected);
	}

	@Test
	@DisplayName("US1: GET /api/tasks binds every query parameter")
	void listTasksBindsAllParams() {
		TaskQuery expected = new TaskQuery("rep", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 30), true, TaskSort.DUE_DATE, Sort.Direction.ASC);
		given(this.taskService.list(expected)).willReturn(List.of());

		var result = assertThat(this.mvc.get()
			.uri("/api/tasks?q=rep&status=IN_PROGRESS&priority=HIGH&dueFrom=2026-09-01&dueTo=2026-09-30"
					+ "&archived=true&sort=dueDate&direction=asc"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().isEqualTo("[]");

		then(this.taskService).should().list(expected);
	}

	@Test
	@DisplayName("FR-01.7: GET /api/tasks/overdue lists open tasks past their due date")
	void listOverdueTasks() {
		given(this.taskService.overdue())
			.willReturn(List.of(task("Late report", TaskStatus.TODO, null, LocalDate.of(2026, 9, 20))));

		var result = assertThat(this.mvc.get().uri("/api/tasks/overdue"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$[0].id").isEqualTo(7);
		result.bodyJson().extractingPath("$[0].dueDate").isEqualTo("2026-09-20");
		result.bodyJson().extractingPath("$[0].overdue").isEqualTo(true);
	}

	@Test
	@DisplayName("US1: PUT /api/tasks/{id} updates the task and returns 200")
	void updateTaskReturns200() {
		given(this.taskService.update(7L, "New title", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null))
			.willReturn(task("New title", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null));

		var result = assertThat(this.mvc.put().uri("/api/tasks/7")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"New title\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\"}"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$.id").isEqualTo(7);
		result.bodyJson().extractingPath("$.title").isEqualTo("New title");
		result.bodyJson().extractingPath("$.status").isEqualTo("IN_PROGRESS");
		result.bodyJson().extractingPath("$.priority").isEqualTo("HIGH");

		then(this.taskService).should()
			.update(7L, "New title", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null);
	}

	@Test
	@DisplayName("BR-4/BR-5: complete, archive and restore each return 200 with the task")
	void completeArchiveRestoreReturn200() {
		Task task = task("Write report", null, null, null);
		given(this.taskService.complete(7L)).willReturn(task);
		given(this.taskService.archive(7L)).willReturn(task);
		given(this.taskService.restore(7L)).willReturn(task);

		for (String action : List.of("complete", "archive", "restore")) {
			var result = assertThat(this.mvc.post().uri("/api/tasks/7/" + action));
			result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
			result.bodyJson().extractingPath("$.id").isEqualTo(7);
		}

		then(this.taskService).should().complete(7L);
		then(this.taskService).should().archive(7L);
		then(this.taskService).should().restore(7L);
	}

	@Test
	@DisplayName("US1: DELETE /api/tasks/{id} returns 204 with no body")
	void deleteTaskReturns204() {
		var result = assertThat(this.mvc.delete().uri("/api/tasks/7"));
		result.hasStatus(204);
		result.body().isEmpty();

		then(this.taskService).should().delete(7L);
	}

}
