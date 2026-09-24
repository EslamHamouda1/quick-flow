package com.quickflow.web.learning;

import java.util.List;

import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.learning.LearningCard;
import com.quickflow.domain.learning.LearningService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning-cards")
@Tag(name = "learning")
public class LearningCardController {

	private final LearningService learningService;

	private final TimeService timeService;

	public LearningCardController(LearningService learningService, TimeService timeService) {
		this.learningService = learningService;
		this.timeService = timeService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Cards with their milestones and notes, newest first")
	public List<LearningCardResponse> listLearningCards() {
		return learningService.list().stream().map(this::response).toList();
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public LearningCardResponse createLearningCard(@Valid @RequestBody LearningCardCreateRequest request) {
		return response(learningService.create(request.title(), request.description()));
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public LearningCardResponse getLearningCard(@PathVariable long id) {
		return response(learningService.get(id));
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public LearningCardResponse updateLearningCard(@PathVariable long id,
			@Valid @RequestBody LearningCardUpdateRequest request) {
		return response(learningService.update(id, request.title(), request.description(), request.status()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete the card, its milestones and notes permanently (BR-14)")
	public void deleteLearningCard(@PathVariable long id) {
		learningService.delete(id);
	}

	@PostMapping(path = "/{id}/milestones", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public MilestoneResponse addMilestone(@PathVariable long id, @Valid @RequestBody MilestoneCreateRequest request) {
		return MilestoneResponse.from(learningService.addMilestone(id, request.title(), request.targetDate()));
	}

	@PutMapping(path = "/{id}/milestones/{milestoneId}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Edit title/target date and set done or not done")
	public MilestoneResponse updateMilestone(@PathVariable long id, @PathVariable long milestoneId,
			@Valid @RequestBody MilestoneUpdateRequest request) {
		return MilestoneResponse.from(learningService.updateMilestone(id, milestoneId, request.title(),
				request.targetDate(), request.done()));
	}

	@DeleteMapping("/{id}/milestones/{milestoneId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMilestone(@PathVariable long id, @PathVariable long milestoneId) {
		learningService.deleteMilestone(id, milestoneId);
	}

	@PostMapping(path = "/{id}/notes", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public NoteResponse addNote(@PathVariable long id, @Valid @RequestBody NoteCreateRequest request) {
		return NoteResponse.from(learningService.addNote(id, request.text()), timeService);
	}

	@DeleteMapping("/{id}/notes/{noteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteNote(@PathVariable long id, @PathVariable long noteId) {
		learningService.deleteNote(id, noteId);
	}

	private LearningCardResponse response(LearningCard card) {
		return LearningCardResponse.from(card, timeService);
	}

}
