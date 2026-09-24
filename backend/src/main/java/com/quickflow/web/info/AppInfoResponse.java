package com.quickflow.web.info;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "AppInfo")
public record AppInfoResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED, examples = "Africa/Cairo") String timeZone,
		@Schema(requiredMode = RequiredMode.REQUIRED) OffsetDateTime now) {
}
