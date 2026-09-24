package com.quickflow.web.task;

import java.time.LocalDate;
import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.common.ValidationException;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskPriority;
import com.quickflow.domain.task.TaskQuery;
import com.quickflow.domain.task.TaskService;
import com.quickflow.domain.task.TaskSort;
import com.quickflow.domain.task.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "tasks")
public class TaskController {

	private final TaskService taskService;

	private final TimeService timeService;

	public TaskController(TaskService taskService, TimeService timeService) {
		this.taskService = taskService;
		this.timeService = timeService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List tasks (search, filters, sort; archived excluded by default, BR-4)")
	public List<TaskResponse> listTasks(
			@Parameter(description = "Case-insensitive substring of the title") @RequestParam(required = false) String q,
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) TaskPriority priority,
			@Parameter(description = "Inclusive") @RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
			@Parameter(description = "Inclusive") @RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
			@RequestParam(defaultValue = "false") boolean archived,
			@Parameter(schema = @Schema(allowableValues = {"createdAt", "dueDate"}, defaultValue = "createdAt"))
			@RequestParam(defaultValue = "createdAt") String sort,
			@Parameter(schema = @Schema(allowableValues = {"asc", "desc"}, defaultValue = "desc"))
			@RequestParam(defaultValue = "desc") String direction) {
		TaskQuery query = new TaskQuery(q, status, priority, dueFrom, dueTo, archived, TaskSort.fromParam(sort),
				direction(direction));
		return responses(taskService.list(query));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse createTask(@Valid @RequestBody TaskCreateRequest request) {
		return response(taskService.create(request.title(), request.description(), request.status(),
				request.priority(), request.dueDate()));
	}

	@GetMapping(path = "/overdue", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Tasks due before today, not Done, not archived (FR-01.7)")
	public List<TaskResponse> listOverdueTasks() {
		return responses(taskService.overdue());
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public TaskResponse getTask(@PathVariable long id) {
		return response(taskService.get(id));
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Replace the editable fields (status DONE sets completedAt, leaving DONE clears it)")
	public TaskResponse updateTask(@PathVariable long id, @Valid @RequestBody TaskUpdateRequest request) {
		return response(taskService.update(id, request.title(), request.description(), request.status(),
				request.priority(), request.dueDate()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete permanently (BR-14)")
	public void deleteTask(@PathVariable long id) {
		taskService.delete(id);
	}

	@PostMapping(path = "/{id}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Mark completed (status DONE, BR-5)")
	public TaskResponse completeTask(@PathVariable long id) {
		return response(taskService.complete(id));
	}

	@PostMapping(path = "/{id}/archive", produces = MediaType.APPLICATION_JSON_VALUE)
	public TaskResponse archiveTask(@PathVariable long id) {
		return response(taskService.archive(id));
	}

	@PostMapping(path = "/{id}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
	public TaskResponse restoreTask(@PathVariable long id) {
		return response(taskService.restore(id));
	}

	private static Sort.Direction direction(String value) {
		return switch (value) {
			case "asc" -> Sort.Direction.ASC;
			case "desc" -> Sort.Direction.DESC;
			default -> throw new ValidationException("direction", "must be one of [asc, desc]");
		};
	}

	private TaskResponse response(Task task) {
		return TaskResponse.from(task, timeService);
	}

	private List<TaskResponse> responses(List<Task> tasks) {
		return tasks.stream().map(this::response).toList();
	}

}
