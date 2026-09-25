package com.quickflow.domain.plan;

import java.util.List;

/** A plan with its computed progress and item views, built inside the service transaction (open-in-view is off). */
public record PlanView(Plan plan, List<Item> items, PlanProgress progress) {

	/** An item with the title to show and whether its source was deleted (FR-07.6, A-4). */
	public record Item(PlanItem item, String sourceTitle, boolean sourceRemoved) {
	}

}
