package com.quickflow.web.dashboard;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	private final TimeService timeService;

	public DashboardController(DashboardService dashboardService, TimeService timeService) {
		this.dashboardService = dashboardService;
		this.timeService = timeService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Summary computed from current data")
	public DashboardResponse getDashboard() {
		return DashboardResponse.from(dashboardService.summary(), timeService);
	}

}
