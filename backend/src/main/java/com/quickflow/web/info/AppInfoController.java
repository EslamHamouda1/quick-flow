package com.quickflow.web.info;

import com.quickflow.domain.common.TimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app-info")
@Tag(name = "info")
public class AppInfoController {

	private final TimeService timeService;

	public AppInfoController(TimeService timeService) {
		this.timeService = timeService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "App time zone and the server's current time")
	public AppInfoResponse getAppInfo() {
		return new AppInfoResponse(timeService.zone().getId(), timeService.now());
	}

}
