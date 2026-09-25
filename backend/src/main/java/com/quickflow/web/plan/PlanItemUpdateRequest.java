package com.quickflow.web.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;

@Schema(name = "PlanItemUpdate")
public record PlanItemUpdateRequest(@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull Boolean done) {
}
