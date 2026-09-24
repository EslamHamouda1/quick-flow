package com.quickflow.web.learning;

import java.time.OffsetDateTime;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.learning.LearningNote;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "Note")
public record NoteResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = RequiredMode.REQUIRED) Long cardId,
		@Schema(requiredMode = RequiredMode.REQUIRED) String text,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime createdAt) {

	static NoteResponse from(LearningNote note, TimeService time) {
		return new NoteResponse(note.getId(), note.getCardId(), note.getText(), time.toOffset(note.getCreatedAt()));
	}

}
