package com.quickflow.web.habit;

import java.time.LocalDate;
import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.habit.HabitService;
import com.quickflow.domain.habit.HabitView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/habits")
@Tag(name = "habits")
public class HabitController {

	private final HabitService habitService;

	private final TimeService timeService;

	public HabitController(HabitService habitService, TimeService timeService) {
		this.habitService = habitService;
		this.timeService = timeService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Habits with progress, oldest first")
	public List<HabitResponse> listHabits(
			@Parameter(description = "Omit for all habits") @RequestParam(required = false) Boolean active) {
		return habitService.list(active).stream().map(this::response).toList();
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public HabitResponse createHabit(@Valid @RequestBody HabitWriteRequest request) {
		return response(habitService.create(request.name(), request.description(), request.frequency()));
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public HabitResponse getHabit(@PathVariable long id) {
		return response(habitService.get(id));
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public HabitResponse updateHabit(@PathVariable long id, @Valid @RequestBody HabitWriteRequest request) {
		return response(habitService.update(id, request.name(), request.description(), request.frequency()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete the habit and its completions permanently (BR-14)")
	public void deleteHabit(@PathVariable long id) {
		habitService.delete(id);
	}

	@PostMapping(path = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	public HabitResponse deactivateHabit(@PathVariable long id) {
		return response(habitService.deactivate(id));
	}

	@PostMapping(path = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
	public HabitResponse activateHabit(@PathVariable long id) {
		return response(habitService.activate(id));
	}

	@GetMapping(path = "/{id}/completions", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Completions, newest date first")
	public List<HabitCompletionResponse> listHabitCompletions(@PathVariable long id) {
		return habitService.completions(id).stream()
				.map(completion -> HabitCompletionResponse.from(completion, timeService))
				.toList();
	}

	/** No body, {@code {}} or a null date means today (A-4). */
	@PostMapping(path = "/{id}/completions", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Record a completion for a date (default today); one per habit and date (BR-7)")
	public HabitCompletionResponse completeHabit(@PathVariable long id,
			@RequestBody(required = false) HabitCompletionCreateRequest request) {
		LocalDate date = request != null ? request.date() : null;
		return HabitCompletionResponse.from(habitService.complete(id, date), timeService);
	}

	@DeleteMapping("/{id}/completions/{date}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void undoHabitCompletion(@PathVariable long id,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		habitService.undo(id, date);
	}

	private HabitResponse response(HabitView view) {
		return HabitResponse.from(view, timeService);
	}

}
