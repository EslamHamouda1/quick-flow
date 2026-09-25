package com.quickflow.domain.plan;

/** A plan's computed state at one instant; {@code restSeconds} is null outside the plan window (BR-12). */
public record PlanProgress(PlanStatus status, int doneItems, int totalItems, int progressPercent, Long restSeconds) {
}
