package com.quickflow.domain.plan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.common.ValidationException;
import com.quickflow.domain.task.TaskService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanService {

	/** Active plans: priority order, then start, then id (A-9). */
	private static final Comparator<PlanView> ACTIVE_ORDER = Comparator
			.comparingInt((PlanView v) -> v.plan().getPriorityOrder())
			.thenComparing(v -> v.plan().getStartDateTime())
			.thenComparing(v -> v.plan().getId(), Comparator.nullsLast(Comparator.naturalOrder()));

	/** Completed plans: latest end first, then id desc (A-9). */
	private static final Comparator<PlanView> COMPLETED_ORDER = Comparator
			.comparing((PlanView v) -> v.plan().getEndDateTime()).reversed()
			.thenComparing(v -> v.plan().getId(), Comparator.nullsLast(Comparator.<Long>reverseOrder()));

	private final PlanRepository plans;

	private final PlanSourceResolver sources;

	private final TaskService taskService;

	private final TimeService timeService;

	public PlanService(PlanRepository plans, PlanSourceResolver sources, TaskService taskService,
			TimeService timeService) {
		this.plans = plans;
		this.sources = sources;
		this.taskService = taskService;
		this.timeService = timeService;
	}

	/** Field errors and unknown or deleted sources (BR-10) are reported together (A-3). */
	public PlanView create(String title, int estimatedDurationMinutes, Instant startDateTime, Instant endDateTime,
			int priorityOrder, List<PlanItemRef> items) {
		List<FieldError> errors = new ArrayList<>();
		Plan plan = null;
		try {
			plan = new Plan(title, estimatedDurationMinutes, startDateTime, endDateTime, priorityOrder, items, now());
		}
		catch (ValidationException ex) {
			errors.addAll(ex.getErrors());
		}
		List<String> titles = new ArrayList<>();
		if (items != null) {
			for (int i = 0; i < items.size(); i++) {
				PlanItemRef ref = items.get(i);
				if (ref == null || ref.sourceType() == null || ref.sourceId() == null) {
					titles.add(null);
					continue;
				}
				Optional<String> sourceTitle = sources.title(ref.sourceType(), ref.sourceId());
				if (sourceTitle.isEmpty()) {
					errors.add(new FieldError("items[" + i + "].sourceId",
							"must reference an existing " + ref.sourceType()));
				}
				titles.add(sourceTitle.orElse(null));
			}
		}
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
		for (int i = 0; i < titles.size(); i++) {
			plan.getItems().get(i).refreshTitle(titles.get(i));
		}
		return view(plans.save(plan), true);
	}

	@Transactional(readOnly = true)
	public PlanView get(long id) {
		return view(find(id), false);
	}

	@Transactional(readOnly = true)
	public List<PlanView> list(PlanGroup group) {
		List<PlanView> all = plans.findAll().stream().map(plan -> view(plan, false)).toList();
		List<PlanView> active = all.stream()
				.filter(v -> v.progress().status() != PlanStatus.COMPLETED)
				.sorted(ACTIVE_ORDER)
				.toList();
		List<PlanView> completed = all.stream()
				.filter(v -> v.progress().status() == PlanStatus.COMPLETED)
				.sorted(COMPLETED_ORDER)
				.toList();
		return switch (group) {
			case ACTIVE -> active;
			case COMPLETED -> completed;
			case ALL -> {
				List<PlanView> both = new ArrayList<>(active);
				both.addAll(completed);
				yield both;
			}
		};
	}

	public PlanView update(long id, String title, int estimatedDurationMinutes, Instant startDateTime,
			Instant endDateTime, int priorityOrder) {
		Plan plan = find(id);
		plan.update(title, estimatedDurationMinutes, startDateTime, endDateTime, priorityOrder);
		return view(plan, true);
	}

	/** Removes the plan and, by cascade, its items; the referenced sources are unchanged (BR-14). */
	public void delete(long id) {
		plans.delete(find(id));
	}

	/**
	 * A TASK item marked done completes its task while the task exists; un-ticking and HABIT / LEARNING_RESOURCE
	 * items change no source (BR-13, FR-07.5, A-7).
	 */
	public PlanView setItemDone(long id, long itemId, boolean done) {
		Plan plan = find(id);
		PlanItem item = plan.findItem(itemId)
				.orElseThrow(() -> new NotFoundException("Plan item " + itemId + " not found on plan " + id));
		item.setDone(done);
		if (done && item.getSourceType() == PlanSourceType.TASK
				&& sources.title(PlanSourceType.TASK, item.getSourceId()).isPresent()) {
			taskService.complete(item.getSourceId());
		}
		return view(plan, true);
	}

	private Plan find(long id) {
		return plans.findById(id).orElseThrow(() -> new NotFoundException("Plan " + id + " not found"));
	}

	/**
	 * Shows each source's current title while it exists, else the stored snapshot with {@code sourceRemoved};
	 * write operations also update the snapshot (A-4).
	 */
	private PlanView view(Plan plan, boolean refreshSnapshot) {
		List<PlanView.Item> items = plan.getItems().stream().map(item -> {
			Optional<String> current = sources.title(item.getSourceType(), item.getSourceId());
			if (current.isEmpty()) {
				return new PlanView.Item(item, item.getSourceTitle(), true);
			}
			if (refreshSnapshot) {
				item.refreshTitle(current.get());
			}
			return new PlanView.Item(item, current.get(), false);
		}).toList();
		return new PlanView(plan, items, PlanStatusPolicy.progress(plan, now()));
	}

	private Instant now() {
		return timeService.now().toInstant();
	}

}
