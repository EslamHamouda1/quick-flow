package com.quickflow.domain.plan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.ValidationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** A Todo Plan; its status, progress and rest time are computed on read ({@link PlanStatusPolicy}). */
@Entity
@Table(name = "plan")
public class Plan {

	static final int TITLE_MAX = 200;

	static final int DURATION_MIN = 1;

	static final int DURATION_MAX = 100_000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = TITLE_MAX)
	private String title;

	@Column(nullable = false)
	private int estimatedDurationMinutes;

	@Column(nullable = false)
	private Instant startDateTime;

	@Column(nullable = false)
	private Instant endDateTime;

	/** 1 is the highest; not unique (FR-07.1). */
	@Column(nullable = false)
	private int priorityOrder;

	@Column(nullable = false)
	private Instant createdAt;

	/** Removed together with the plan (BR-14); in the order given on create (A-9). */
	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<PlanItem> items = new ArrayList<>();

	protected Plan() {
	}

	/** A new plan with one not-done item per distinct source (FR-07.1, FR-07.2, BR-10, BR-11). */
	public Plan(String title, int estimatedDurationMinutes, Instant startDateTime, Instant endDateTime,
			int priorityOrder, List<PlanItemRef> items, Instant now) {
		List<FieldError> errors = new ArrayList<>();
		String cleanTitle = checkFields(title, estimatedDurationMinutes, startDateTime, endDateTime, priorityOrder,
				errors);
		checkItems(items, errors);
		throwIfAny(errors);
		assign(cleanTitle, estimatedDurationMinutes, startDateTime, endDateTime, priorityOrder);
		this.createdAt = now;
		for (PlanItemRef ref : items) {
			this.items.add(new PlanItem(this, ref.sourceType(), ref.sourceId()));
		}
	}

	/** Replaces the editable fields; never changes items, their flags or {@code createdAt} (FR-07.3, A-6). */
	public void update(String title, int estimatedDurationMinutes, Instant startDateTime, Instant endDateTime,
			int priorityOrder) {
		List<FieldError> errors = new ArrayList<>();
		String cleanTitle = checkFields(title, estimatedDurationMinutes, startDateTime, endDateTime, priorityOrder,
				errors);
		throwIfAny(errors);
		assign(cleanTitle, estimatedDurationMinutes, startDateTime, endDateTime, priorityOrder);
	}

	public Optional<PlanItem> findItem(long itemId) {
		return items.stream().filter(item -> item.getId() != null && item.getId() == itemId).findFirst();
	}

	public int doneItems() {
		return (int) items.stream().filter(PlanItem::isDone).count();
	}

	public int totalItems() {
		return items.size();
	}

	private void assign(String title, int estimatedDurationMinutes, Instant startDateTime, Instant endDateTime,
			int priorityOrder) {
		this.title = title;
		this.estimatedDurationMinutes = estimatedDurationMinutes;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.priorityOrder = priorityOrder;
	}

	/** Returns the trimmed title (A-1). */
	private static String checkFields(String title, int estimatedDurationMinutes, Instant startDateTime,
			Instant endDateTime, int priorityOrder, List<FieldError> errors) {
		String trimmed = title == null ? "" : title.trim();
		if (trimmed.isEmpty()) {
			errors.add(new FieldError("title", "must not be blank"));
		}
		else if (trimmed.length() > TITLE_MAX) {
			errors.add(new FieldError("title", "must be at most " + TITLE_MAX + " characters"));
		}
		if (estimatedDurationMinutes < DURATION_MIN || estimatedDurationMinutes > DURATION_MAX) {
			errors.add(new FieldError("estimatedDurationMinutes",
					"must be between " + DURATION_MIN + " and " + DURATION_MAX));
		}
		if (startDateTime == null) {
			errors.add(new FieldError("startDateTime", "must not be null"));
		}
		if (endDateTime == null) {
			errors.add(new FieldError("endDateTime", "must not be null"));
		}
		else if (startDateTime != null && !endDateTime.isAfter(startDateTime)) {
			errors.add(new FieldError("endDateTime", "must be after startDateTime"));
		}
		if (priorityOrder < 1) {
			errors.add(new FieldError("priorityOrder", "must be at least 1"));
		}
		return trimmed;
	}

	/** At least one item (BR-10), each complete, no (type, id) twice (FR-07.2, A-3). */
	private static void checkItems(List<PlanItemRef> items, List<FieldError> errors) {
		if (items == null || items.isEmpty()) {
			errors.add(new FieldError("items", "must contain at least one item"));
			return;
		}
		Set<PlanItemRef> seen = new HashSet<>();
		for (int i = 0; i < items.size(); i++) {
			PlanItemRef ref = items.get(i);
			if (ref == null || ref.sourceType() == null) {
				errors.add(new FieldError("items[" + i + "].sourceType", "must not be null"));
			}
			if (ref == null || ref.sourceId() == null) {
				errors.add(new FieldError("items[" + i + "].sourceId", "must not be null"));
			}
			if (ref != null && ref.sourceType() != null && ref.sourceId() != null && !seen.add(ref)) {
				errors.add(new FieldError("items[" + i + "]", "must not repeat a selected item"));
			}
		}
	}

	private static void throwIfAny(List<FieldError> errors) {
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public int getEstimatedDurationMinutes() {
		return estimatedDurationMinutes;
	}

	public Instant getStartDateTime() {
		return startDateTime;
	}

	public Instant getEndDateTime() {
		return endDateTime;
	}

	public int getPriorityOrder() {
		return priorityOrder;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<PlanItem> getItems() {
		return Collections.unmodifiableList(items);
	}

}
