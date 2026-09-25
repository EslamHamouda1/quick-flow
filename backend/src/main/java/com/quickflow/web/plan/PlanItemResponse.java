package com.quickflow.web.plan;

import com.quickflow.domain.plan.PlanItem;
import com.quickflow.domain.plan.PlanSourceType;
import com.quickflow.domain.plan.PlanView;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "PlanItem")
public record PlanItemResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) PlanSourceType sourceType,
		@Schema(requiredMode = RequiredMode.REQUIRED) Long sourceId,
		@Schema(requiredMode = RequiredMode.REQUIRED) String sourceTitle,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "The source was deleted (FR-07.6)")
		boolean sourceRemoved,
		@Schema(requiredMode = RequiredMode.REQUIRED) boolean done) {

	static PlanItemResponse from(PlanView.Item view) {
		PlanItem item = view.item();
		return new PlanItemResponse(item.getId(), item.getSourceType(), item.getSourceId(), view.sourceTitle(),
				view.sourceRemoved(), item.isDone());
	}

}
