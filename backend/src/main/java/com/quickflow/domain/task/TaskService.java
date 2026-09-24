package com.quickflow.domain.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

	private final TaskRepository repository;

	private final TimeService timeService;

	public TaskService(TaskRepository repository, TimeService timeService) {
		this.repository = repository;
		this.timeService = timeService;
	}

	public Task create(String title, String description, TaskStatus status, TaskPriority priority,
			LocalDate dueDate) {
		return repository.save(new Task(title, description, status, priority, dueDate, now()));
	}

	@Transactional(readOnly = true)
	public Task get(long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Task " + id + " not found"));
	}

	@Transactional(readOnly = true)
	public List<Task> list(TaskQuery query) {
		return repository.findAll(TaskSpecifications.matching(query));
	}

	@Transactional(readOnly = true)
	public List<Task> overdue() {
		return repository.findOverdue(timeService.today());
	}

	public Task update(long id, String title, String description, TaskStatus status, TaskPriority priority,
			LocalDate dueDate) {
		Task task = get(id);
		task.update(title, description, status, priority, dueDate, now());
		return task;
	}

	public Task complete(long id) {
		Task task = get(id);
		task.complete(now());
		return task;
	}

	public Task archive(long id) {
		Task task = get(id);
		task.archive(now());
		return task;
	}

	public Task restore(long id) {
		Task task = get(id);
		task.restore(now());
		return task;
	}

	public void delete(long id) {
		repository.delete(get(id));
	}

	private Instant now() {
		return timeService.now().toInstant();
	}

}
