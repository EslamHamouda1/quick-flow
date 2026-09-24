package com.quickflow.web.learning;

import java.time.OffsetDateTime;
import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.learning.LearningCard;
import com.quickflow.domain.learning.LearningStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "LearningCard")
public record LearningCardResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) String title,
		@Schema(types = {"string", "null"}) String description,
		@Schema(requiredMode = RequiredMode.REQUIRED) LearningStatus status,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt,
		@Schema(requiredMode = RequiredMode.REQUIRED) List<MilestoneResponse> milestones,
		@Schema(requiredMode = RequiredMode.REQUIRED) List<NoteResponse> notes,
		@Schema(requiredMode = RequiredMode.REQUIRED) int milestonesDone,
		@Schema(requiredMode = RequiredMode.REQUIRED) int milestonesTotal) {

	static LearningCardResponse from(LearningCard card, TimeService time) {
		return new LearningCardResponse(card.getId(), card.getTitle(), card.getDescription(), card.getStatus(),
				time.toOffset(card.getCreatedAt()),
				card.getMilestones().stream().map(MilestoneResponse::from).toList(),
				card.getNotes().stream().map(note -> NoteResponse.from(note, time)).toList(),
				card.milestonesDone(), card.milestonesTotal());
	}

}
