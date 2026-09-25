package com.quickflow.domain.settings;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum DefaultPage {
	DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS
}
