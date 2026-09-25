package com.quickflow.web.plan;

import com.quickflow.domain.plan.PlanItemRef;
import com.quickflow.domain.plan.PlanSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;

@Schema(name = "PlanItemRef")
public record PlanItemRefRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull PlanSourceType sourceType,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull Long sourceId) {

	PlanItemRef toRef() {
		return new PlanItemRef(sourceType, sourceId);
	}

}
