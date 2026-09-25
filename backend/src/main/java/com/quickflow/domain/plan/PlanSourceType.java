package com.quickflow.domain.plan;

import io.swagger.v3.oas.annotations.media.Schema;

/** What a plan item references (FR-07.2). */
@Schema(enumAsRef = true)
public enum PlanSourceType {
	TASK, HABIT, LEARNING_RESOURCE
}
