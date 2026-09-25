package com.quickflow.domain.plan;

import java.util.Arrays;

import com.quickflow.domain.common.ValidationException;

/** Groups of the plan list, bound from the contract's lower-case `group` values (A-11). */
public enum PlanGroup {

	ACTIVE("active"), COMPLETED("completed"), ALL("all");

	private final String param;

	PlanGroup(String param) {
		this.param = param;
	}

	public String param() {
		return param;
	}

	public static PlanGroup fromParam(String value) {
		return Arrays.stream(values())
				.filter(g -> g.param.equals(value))
				.findFirst()
				.orElseThrow(() -> new ValidationException("group", "must be one of [active, completed, all]"));
	}

}
