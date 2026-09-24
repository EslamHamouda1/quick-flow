package com.quickflow.domain.task;

import java.util.Arrays;

import com.quickflow.domain.common.ValidationException;

/** Sort keys of the task list, bound from the contract's lower-camel `sort` values (A-9). */
public enum TaskSort {

	CREATED_AT("createdAt"), DUE_DATE("dueDate");

	private final String param;

	TaskSort(String param) {
		this.param = param;
	}

	public String param() {
		return param;
	}

	public static TaskSort fromParam(String value) {
		return Arrays.stream(values())
				.filter(s -> s.param.equals(value))
				.findFirst()
				.orElseThrow(() -> new ValidationException("sort", "must be one of [createdAt, dueDate]"));
	}

}
