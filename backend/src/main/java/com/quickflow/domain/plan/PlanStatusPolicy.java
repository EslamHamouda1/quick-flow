package com.quickflow.domain.plan;

import java.time.Duration;
import java.time.Instant;

/** Pure functions of the stored plan values and the clock (research R-6, NFR-4). */
public final class PlanStatusPolicy {

	private PlanStatusPolicy() {
	}

	/** Not Started before start; Completed at/after end or once all items are done; else In Progress (FR-08.4). */
	public static PlanStatus statusAt(Instant start, Instant end, int done, int total, Instant now) {
		if (now.isBefore(start)) {
			return PlanStatus.NOT_STARTED;
		}
		if (!now.isBefore(end) || (total > 0 && done == total)) {
			return PlanStatus.COMPLETED;
		}
		return PlanStatus.IN_PROGRESS;
	}

	/** Whole seconds to end while {@code start <= now < end}, else null, whatever the status (BR-12, A-5). */
	public static Long restSeconds(Instant start, Instant end, Instant now) {
		if (now.isBefore(start) || !now.isBefore(end)) {
			return null;
		}
		return Duration.between(now, end).getSeconds();
	}

	/** floor(100 * done / total), 0 for no items (FR-08.3). */
	public static int progressPercent(int done, int total) {
		return total == 0 ? 0 : 100 * done / total;
	}

	public static PlanProgress progress(Plan plan, Instant now) {
		int done = plan.doneItems();
		int total = plan.totalItems();
		return new PlanProgress(statusAt(plan.getStartDateTime(), plan.getEndDateTime(), done, total, now), done,
				total, progressPercent(done, total),
				restSeconds(plan.getStartDateTime(), plan.getEndDateTime(), now));
	}

}
