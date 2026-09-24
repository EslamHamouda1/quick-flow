package com.quickflow.domain.habit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.quickflow.domain.common.ConflictException;
import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.common.ValidationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HabitService {

	private final HabitRepository habits;

	private final HabitCompletionRepository completions;

	private final TimeService timeService;

	public HabitService(HabitRepository habits, HabitCompletionRepository completions, TimeService timeService) {
		this.habits = habits;
		this.completions = completions;
		this.timeService = timeService;
	}

	public HabitView create(String name, String description, HabitFrequency frequency) {
		Habit habit = habits.save(new Habit(name, description, frequency, now()));
		return new HabitView(habit, HabitProgressCalculator.progress(frequency, List.of(), timeService.today()));
	}

	@Transactional(readOnly = true)
	public HabitView get(long id) {
		return view(find(id));
	}

	/** All habits when {@code active} is null, otherwise only active or inactive ones; oldest first (A-8). */
	@Transactional(readOnly = true)
	public List<HabitView> list(Boolean active) {
		List<Habit> found = active == null ? habits.findAllByOrderByCreatedAtAscIdAsc()
				: habits.findByActiveOrderByCreatedAtAscIdAsc(active);
		return found.stream().map(this::view).toList();
	}

	public HabitView update(long id, String name, String description, HabitFrequency frequency) {
		Habit habit = find(id);
		habit.update(name, description, frequency);
		return view(habit);
	}

	public HabitView activate(long id) {
		Habit habit = find(id);
		habit.activate();
		return view(habit);
	}

	public HabitView deactivate(long id) {
		Habit habit = find(id);
		habit.deactivate();
		return view(habit);
	}

	/** Removes the habit and, by cascade, its completions (BR-14). */
	public void delete(long id) {
		habits.delete(find(id));
	}

	/**
	 * Records a completion for {@code date} (today when null, FR-04.1); one per habit and date (BR-7), only for
	 * an active habit (FR-04.4). Check order per A-4.
	 */
	public HabitCompletion complete(long id, LocalDate date) {
		Habit habit = find(id);
		LocalDate today = timeService.today();
		LocalDate day = date != null ? date : today;
		if (day.isAfter(today)) {
			throw new ValidationException("date", "must not be in the future");
		}
		if (!habit.isActive()) {
			throw new ConflictException("Habit " + id + " is inactive");
		}
		if (completions.existsByHabitIdAndCompletionDate(id, day)) {
			throw alreadyComplete(id, day);
		}
		try {
			return completions.saveAndFlush(new HabitCompletion(habit, day, now()));
		}
		catch (DataIntegrityViolationException ex) {
			throw alreadyComplete(id, day);
		}
	}

	/** Removes the completion for {@code date} (FR-04.3); allowed on an inactive habit (A-5). */
	public void undo(long id, LocalDate date) {
		find(id);
		HabitCompletion completion = completions.findByHabitIdAndCompletionDate(id, date)
				.orElseThrow(() -> new NotFoundException("Habit " + id + " has no completion for " + date));
		completions.delete(completion);
	}

	@Transactional(readOnly = true)
	public List<HabitCompletion> completions(long id) {
		find(id);
		return completions.findByHabitIdOrderByCompletionDateDesc(id);
	}

	private Habit find(long id) {
		return habits.findById(id).orElseThrow(() -> new NotFoundException("Habit " + id + " not found"));
	}

	private HabitView view(Habit habit) {
		List<LocalDate> dates = completions.findDatesByHabitId(habit.getId());
		return new HabitView(habit,
				HabitProgressCalculator.progress(habit.getFrequency(), dates, timeService.today()));
	}

	private static ConflictException alreadyComplete(long id, LocalDate date) {
		return new ConflictException("Habit " + id + " is already complete for " + date);
	}

	private Instant now() {
		return timeService.now().toInstant();
	}

}
