package com.quickflow.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-09-20T08:00:00Z");
	private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

	@Mock
	private TaskRepository repository;

	private TaskService service;

	@BeforeEach
	void setUp() {
		service = new TaskService(repository, new TimeService(Clock.fixed(NOW, CAIRO)));
	}

	private static Task existing(long id) {
		Task task = new Task("Old title", "Old description", TaskStatus.TODO, TaskPriority.LOW,
				LocalDate.of(2026, 9, 30), EARLIER);
		ReflectionTestUtils.setField(task, "id", id);
		return task;
	}

	@Test
	@DisplayName("FR-01.2: create sets createdAt and updatedAt from the clock")
	void fr01_2_createSetsCreatedAndUpdatedFromClock() {
		given(repository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0));

		Task task = service.create("Write report", "Quarterly", TaskStatus.TODO, TaskPriority.HIGH,
				LocalDate.of(2026, 9, 30));

		assertThat(task.getCreatedAt()).isEqualTo(NOW);
		assertThat(task.getUpdatedAt()).isEqualTo(NOW);
		assertThat(task.getTitle()).isEqualTo("Write report");
		verify(repository).save(task);
	}

	@Test
	@DisplayName("FR-01.2: update changes updatedAt to the clock and applies the fields")
	void fr01_2_updateChangesUpdatedAt() {
		Task task = existing(7L);
		given(repository.findById(7L)).willReturn(Optional.of(task));

		Task result = service.update(7L, "New title", "New description", TaskStatus.IN_PROGRESS,
				TaskPriority.HIGH, LocalDate.of(2026, 10, 1));

		assertThat(result.getUpdatedAt()).isEqualTo(NOW);
		assertThat(result.getCreatedAt()).isEqualTo(EARLIER);
		assertThat(result.getTitle()).isEqualTo("New title");
		assertThat(result.getDescription()).isEqualTo("New description");
		assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
		assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
		assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
	}

	@Test
	@DisplayName("BR-4: archive sets archived and updatedAt")
	void br4_archiveSetsArchived() {
		Task task = existing(7L);
		given(repository.findById(7L)).willReturn(Optional.of(task));

		Task result = service.archive(7L);

		assertThat(result.isArchived()).isTrue();
		assertThat(result.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("BR-4: restore clears archived and sets updatedAt")
	void br4_restoreClearsArchived() {
		Task task = existing(7L);
		task.archive(EARLIER);
		given(repository.findById(7L)).willReturn(Optional.of(task));

		Task result = service.restore(7L);

		assertThat(result.isArchived()).isFalse();
		assertThat(result.getUpdatedAt()).isEqualTo(NOW);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "get", "update", "delete", "complete", "archive", "restore" })
	@DisplayName("FR-01.2: an unknown id throws NotFoundException")
	void fr01_2_unknownIdNotFound(String operation) {
		given(repository.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> {
			switch (operation) {
				case "get" -> service.get(99L);
				case "update" -> service.update(99L, "Title", null, TaskStatus.TODO, TaskPriority.MEDIUM, null);
				case "delete" -> service.delete(99L);
				case "complete" -> service.complete(99L);
				case "archive" -> service.archive(99L);
				case "restore" -> service.restore(99L);
				default -> throw new IllegalArgumentException(operation);
			}
		}).isInstanceOf(NotFoundException.class).hasMessage("Task 99 not found");
	}

	@Test
	@DisplayName("FR-01.2: delete removes the existing task through the repository")
	void fr01_2_deleteRemovesExistingTask() {
		Task task = existing(7L);
		given(repository.findById(7L)).willReturn(Optional.of(task));

		service.delete(7L);

		verify(repository).delete(task);
	}

	@Test
	@DisplayName("FR-01.7: overdue queries the repository with today's date")
	void fr01_7_overdueUsesToday() {
		Task task = existing(7L);
		given(repository.findOverdue(LocalDate.of(2026, 9, 24))).willReturn(List.of(task));

		List<Task> result = service.overdue();

		assertThat(result).containsExactly(task);
		verify(repository).findOverdue(LocalDate.of(2026, 9, 24));
	}

}
