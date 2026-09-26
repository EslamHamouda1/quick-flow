package com.quickflow.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlanStatusPolicyTest {

	/** A two-hour window: 2026-09-25 08:00Z to 10:00Z. */
	private static final Instant START = Instant.parse("2026-09-25T08:00:00Z");
	private static final Instant END = Instant.parse("2026-09-25T10:00:00Z");

	private static final Instant BEFORE_START = START.minusSeconds(60);
	private static final Instant INSIDE = START.plusSeconds(30 * 60);
	private static final Instant AFTER_END = END.plusSeconds(60);

	@Test
	@DisplayName("FR-08.4: before the start the plan is not started")
	void fr08_4_beforeStartNotStarted() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 0, 3, BEFORE_START)).isEqualTo(PlanStatus.NOT_STARTED);
	}

	@Test
	@DisplayName("FR-08.4: before the start the plan is not started even with all items done")
	void fr08_4_beforeStartAllDoneStillNotStarted() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 3, 3, BEFORE_START)).isEqualTo(PlanStatus.NOT_STARTED);
	}

	@Test
	@DisplayName("FR-08.4: exactly at the start the plan is in progress")
	void fr08_4_atStartInProgress() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 0, 3, START)).isEqualTo(PlanStatus.IN_PROGRESS);
	}

	@Test
	@DisplayName("FR-08.4: inside the window with open items the plan is in progress")
	void fr08_4_insideWindowInProgress() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 2, 3, INSIDE)).isEqualTo(PlanStatus.IN_PROGRESS);
	}

	@Test
	@DisplayName("FR-08.4: inside the window with all items done the plan is completed")
	void fr08_4_insideWindowAllDoneCompleted() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 3, 3, INSIDE)).isEqualTo(PlanStatus.COMPLETED);
	}

	@Test
	@DisplayName("FR-08.4: exactly at the end the plan is completed")
	void fr08_4_atEndCompleted() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 0, 3, END)).isEqualTo(PlanStatus.COMPLETED);
	}

	@Test
	@DisplayName("FR-08.4: after the end the plan is completed")
	void fr08_4_afterEndCompleted() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 1, 3, AFTER_END)).isEqualTo(PlanStatus.COMPLETED);
	}

	@Test
	@DisplayName("FR-08.4: un-ticking an item before the end brings the plan back to in progress")
	void fr08_4_untickBeforeEndBackToInProgress() {
		assertThat(PlanStatusPolicy.statusAt(START, END, 3, 3, INSIDE)).isEqualTo(PlanStatus.COMPLETED);
		assertThat(PlanStatusPolicy.statusAt(START, END, 2, 3, INSIDE)).isEqualTo(PlanStatus.IN_PROGRESS);
	}

	@Test
	@DisplayName("BR-12, FR-08.2: there is no rest time before the start")
	void br12_restSecondsNullBeforeStart() {
		assertThat(PlanStatusPolicy.restSeconds(START, END, BEFORE_START)).isNull();
	}

	@Test
	@DisplayName("BR-12: at the start the rest time is the whole window")
	void br12_restSecondsAtStartIsFullWindow() {
		assertThat(PlanStatusPolicy.restSeconds(START, END, START)).isEqualTo(7200L);
	}

	@Test
	@DisplayName("BR-12, FR-08.2: inside the window the rest time is the seconds to the end")
	void br12_restSecondsInsideWindow() {
		assertThat(PlanStatusPolicy.restSeconds(START, END, INSIDE)).isEqualTo(5400L);
	}

	@Test
	@DisplayName("BR-12: there is no rest time exactly at the end")
	void br12_restSecondsNullAtEnd() {
		assertThat(PlanStatusPolicy.restSeconds(START, END, END)).isNull();
	}

	@Test
	@DisplayName("BR-12, FR-08.2: there is no rest time after the end")
	void br12_restSecondsNullAfterEnd() {
		assertThat(PlanStatusPolicy.restSeconds(START, END, AFTER_END)).isNull();
	}

	@Test
	@DisplayName("FR-08.3: 0 of 3 items done is 0 percent")
	void fr08_3_progressZeroOfThree() {
		assertThat(PlanStatusPolicy.progressPercent(0, 3)).isZero();
	}

	@Test
	@DisplayName("FR-08.3: 1 of 3 items done is 33 percent (rounded down)")
	void fr08_3_progressOneOfThreeIs33() {
		assertThat(PlanStatusPolicy.progressPercent(1, 3)).isEqualTo(33);
	}

	@Test
	@DisplayName("FR-08.3: 2 of 3 items done is 66 percent (rounded down)")
	void fr08_3_progressTwoOfThreeIs66() {
		assertThat(PlanStatusPolicy.progressPercent(2, 3)).isEqualTo(66);
	}

	@Test
	@DisplayName("FR-08.3: all items done is 100 percent")
	void fr08_3_progressAllIs100() {
		assertThat(PlanStatusPolicy.progressPercent(3, 3)).isEqualTo(100);
	}

	@Test
	@DisplayName("NFR-4: the same stored values and time always give the same status, rest time and progress")
	void nfr4_sameInputsSameResult() {
		for (Instant now : List.of(BEFORE_START, START, INSIDE, END, AFTER_END)) {
			assertThat(PlanStatusPolicy.statusAt(START, END, 1, 3, now))
					.isEqualTo(PlanStatusPolicy.statusAt(START, END, 1, 3, now));
			assertThat(PlanStatusPolicy.restSeconds(START, END, now))
					.isEqualTo(PlanStatusPolicy.restSeconds(START, END, now));
		}
		assertThat(PlanStatusPolicy.progressPercent(1, 3)).isEqualTo(PlanStatusPolicy.progressPercent(1, 3));
	}

	@Test
	@DisplayName("FR-08.3, FR-08.4, BR-12: the progress of a plan combines status, counts, percent and rest time")
	void progressOfPlanCombinesAll() {
		Plan plan = new Plan("Study day", 120, START, END, 1,
				List.of(new PlanItemRef(PlanSourceType.TASK, 1L),
						new PlanItemRef(PlanSourceType.HABIT, 2L),
						new PlanItemRef(PlanSourceType.LEARNING_RESOURCE, 3L)),
				BEFORE_START);
		plan.getItems().get(0).setDone(true);

		PlanProgress progress = PlanStatusPolicy.progress(plan, INSIDE);

		assertThat(progress).isEqualTo(new PlanProgress(PlanStatus.IN_PROGRESS, 1, 3, 33, 5400L));
	}

}
