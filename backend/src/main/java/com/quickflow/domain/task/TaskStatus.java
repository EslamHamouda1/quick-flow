package com.quickflow.domain.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum TaskStatus {
	TODO, IN_PROGRESS, DONE
}
