package com.quickflow.web.plan;

import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.plan.PlanGroup;
import com.quickflow.domain.plan.PlanService;
import com.quickflow.domain.plan.PlanView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "plans")
public class PlanController {

	private final PlanService planService;

	private final TimeService timeService;

	public PlanController(PlanService planService, TimeService timeService) {
		this.planService = planService;
		this.timeService = timeService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Plans with computed status, progress and rest time")
	public List<PlanResponse> listPlans(
			@Parameter(description = "active = NOT_STARTED or IN_PROGRESS (priorityOrder asc, then start asc); "
					+ "completed = COMPLETED (end desc); all = active then completed",
					schema = @Schema(allowableValues = {"active", "completed", "all"}, defaultValue = "all"))
			@RequestParam(defaultValue = "all") String group) {
		return planService.list(PlanGroup.fromParam(group)).stream().map(this::response).toList();
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PlanResponse createPlan(@Valid @RequestBody PlanCreateRequest request) {
		return response(planService.create(request.title(), request.estimatedDurationMinutes(),
				request.startDateTime().toInstant(), request.endDateTime().toInstant(), request.priorityOrder(),
				request.items().stream().map(PlanItemRefRequest::toRef).toList()));
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public PlanResponse getPlan(@PathVariable long id) {
		return response(planService.get(id));
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Edit title, estimated duration, start/end and priority order (items are unchanged)")
	public PlanResponse updatePlan(@PathVariable long id, @Valid @RequestBody PlanUpdateRequest request) {
		return response(planService.update(id, request.title(), request.estimatedDurationMinutes(),
				request.startDateTime().toInstant(), request.endDateTime().toInstant(), request.priorityOrder()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete the plan and its items; referenced sources are unchanged (BR-14)")
	public void deletePlan(@PathVariable long id) {
		planService.delete(id);
	}

	@PutMapping(path = "/{id}/items/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Mark a plan item done or not done; a TASK item marked done also completes the task; "
			+ "un-ticking and HABIT/LEARNING_RESOURCE items change no source (BR-13, Q1)")
	public PlanResponse setPlanItemDone(@PathVariable long id, @PathVariable long itemId,
			@Valid @RequestBody PlanItemUpdateRequest request) {
		return response(planService.setItemDone(id, itemId, request.done()));
	}

	private PlanResponse response(PlanView view) {
		return PlanResponse.from(view, timeService);
	}

}
