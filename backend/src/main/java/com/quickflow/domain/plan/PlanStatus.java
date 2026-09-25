package com.quickflow.domain.plan;

import io.swagger.v3.oas.annotations.media.Schema;

/** Never stored: computed on read from the plan's times, its item flags and the clock (FR-08.4, NFR-4). */
@Schema(enumAsRef = true)
public enum PlanStatus {
	NOT_STARTED, IN_PROGRESS, COMPLETED
}
