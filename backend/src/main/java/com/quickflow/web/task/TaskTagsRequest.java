package com.quickflow.web.task;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Tags to add; blank and length rules apply after trimming, in the domain (BR-T1). */
@Schema(name = "TaskTags")
public record TaskTagsRequest(
		@Schema(requiredMode = RequiredMode.REQUIRED,
				description = "Each tag is trimmed first, then must be 1-30 characters (BR-T1); checked by the server, error field `tags`")
		@NotNull @Size(min = 1) List<String> tags) {
}
