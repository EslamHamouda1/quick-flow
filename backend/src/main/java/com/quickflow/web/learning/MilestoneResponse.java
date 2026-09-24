package com.quickflow.web.learning;

import java.time.LocalDate;

import com.quickflow.domain.learning.LearningMilestone;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "Milestone")
public record MilestoneResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) Long cardId,
		@Schema(requiredMode = RequiredMode.REQUIRED) String title,
		@Schema(requiredMode = RequiredMode.REQUIRED) boolean done,
		@Schema(types = {"string", "null"}, format = "date") LocalDate targetDate) {

	static MilestoneResponse from(LearningMilestone milestone) {
		return new MilestoneResponse(milestone.getId(), milestone.getCardId(), milestone.getTitle(),
				milestone.isDone(), milestone.getTargetDate());
	}

}
