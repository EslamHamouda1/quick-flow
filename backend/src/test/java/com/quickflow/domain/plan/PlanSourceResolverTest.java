package com.quickflow.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickflow.domain.habit.Habit;
import com.quickflow.domain.habit.HabitRepository;
import com.quickflow.domain.learning.LearningCard;
import com.quickflow.domain.learning.LearningCardRepository;
import com.quickflow.domain.task.Task;
import com.quickflow.domain.task.TaskRepository;

@ExtendWith(MockitoExtension.class)
class PlanSourceResolverTest {

	@Mock
	private TaskRepository tasks;

	@Mock
	private HabitRepository habits;

	@Mock
	private LearningCardRepository cards;

	private PlanSourceResolver resolver;

	@BeforeEach
	void setUp() {
		this.resolver = new PlanSourceResolver(this.tasks, this.habits, this.cards);
	}

	@Test
	@DisplayName("BR-10: a TASK source resolves to the task title")
	void br10_taskSourceTitleResolved() {
		Task task = mock(Task.class);
		given(task.getTitle()).willReturn("Write report");
		given(this.tasks.findById(1L)).willReturn(Optional.of(task));

		assertThat(this.resolver.title(PlanSourceType.TASK, 1L)).contains("Write report");
	}

	@Test
	@DisplayName("BR-10: a HABIT source resolves to the habit name")
	void br10_habitSourceNameResolved() {
		Habit habit = mock(Habit.class);
		given(habit.getName()).willReturn("Read 20 pages");
		given(this.habits.findById(2L)).willReturn(Optional.of(habit));

		assertThat(this.resolver.title(PlanSourceType.HABIT, 2L)).contains("Read 20 pages");
	}

	@Test
	@DisplayName("BR-10: a LEARNING_RESOURCE source resolves to the card title")
	void br10_learningSourceTitleResolved() {
		LearningCard card = mock(LearningCard.class);
		given(card.getTitle()).willReturn("Spring in Action");
		given(this.cards.findById(3L)).willReturn(Optional.of(card));

		assertThat(this.resolver.title(PlanSourceType.LEARNING_RESOURCE, 3L)).contains("Spring in Action");
	}

	@Test
	@DisplayName("BR-10, FR-07.6: a source that no longer exists resolves to empty")
	void br10_missingSourceIsEmpty() {
		given(this.tasks.findById(4L)).willReturn(Optional.empty());
		given(this.habits.findById(4L)).willReturn(Optional.empty());
		given(this.cards.findById(4L)).willReturn(Optional.empty());

		assertThat(this.resolver.title(PlanSourceType.TASK, 4L)).isEmpty();
		assertThat(this.resolver.title(PlanSourceType.HABIT, 4L)).isEmpty();
		assertThat(this.resolver.title(PlanSourceType.LEARNING_RESOURCE, 4L)).isEmpty();
	}

}
