package com.quickflow.domain.learning;

import io.swagger.v3.oas.annotations.media.Schema;

/** Set by the user only; milestones never change it (spec A-6). */
@Schema(enumAsRef = true)
public enum LearningStatus {
	NOT_STARTED, IN_PROGRESS, COMPLETED
}
