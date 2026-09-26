package com.quickflow.domain.task;

import java.time.LocalDate;

import org.springframework.data.domain.Sort;

/** Search, filters and order of the task list (FR-02.1..3, BR-4, tag filter FR-004). */
public record TaskQuery(String q, TaskStatus status, TaskPriority priority, LocalDate dueFrom, LocalDate dueTo,
		boolean archived, String tag, TaskSort sort, Sort.Direction direction) {
}
