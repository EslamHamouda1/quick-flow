package com.quickflow.domain.plan;

/** One source selected for a plan, as given by the caller (FR-07.2). */
public record PlanItemRef(PlanSourceType sourceType, Long sourceId) {
}
