package com.quickflow.web.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.TimeService;
import com.quickflow.domain.common.ValidationException;
import com.quickflow.domain.plan.Plan;
import com.quickflow.domain.plan.PlanGroup;
import com.quickflow.domain.plan.PlanItem;
import com.quickflow.domain.plan.PlanItemRef;
import com.quickflow.domain.plan.PlanProgress;
import com.quickflow.domain.plan.PlanService;
import com.quickflow.domain.plan.PlanSourceType;
import com.quickflow.domain.plan.PlanStatus;
import com.quickflow.domain.plan.PlanView;

@WebMvcTest(PlanController.class)
@Import(PlanControllerTest.FixedTime.class)
class PlanControllerTest {

	private static final Instant NOW = Instant.parse("2026-09-24T09:00:00Z");

	private static final Instant START = Instant.parse("2026-09-24T09:00:00Z");

	private static final Instant END = Instant.parse("2026-09-24T11:00:00Z");

	private static final List<PlanItemRef> REFS = List.of(
			new PlanItemRef(PlanSourceType.TASK, 11L),
			new PlanItemRef(PlanSourceType.HABIT, 12L),
			new PlanItemRef(PlanSourceType.LEARNING_RESOURCE, 13L));

	private static final String CREATE_BODY = """
			{"title":"Weekly focus",
			 "items":[{"sourceType":"TASK","sourceId":11},{"sourceType":"HABIT","sourceId":12},
			          {"sourceType":"LEARNING_RESOURCE","sourceId":13}],
			 "estimatedDurationMinutes":120,
			 "startDateTime":"2026-09-24T12:00:00+03:00",
			 "endDateTime":"2026-09-24T14:00:00+03:00",
			 "priorityOrder":1}
			""";

	@TestConfiguration
	static class FixedTime {

		@Bean
		TimeService timeService() {
			return new TimeService(Clock.fixed(NOW, ZoneId.of("Africa/Cairo")));
		}

	}

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PlanService planService;

	/** Plan 5 with items 21 (TASK 11, done), 22 (HABIT 12), 23 (LEARNING_RESOURCE 13, source removed). */
	private static PlanView view(String title, int priorityOrder) {
		Plan plan = new Plan(title, 120, START, END, priorityOrder, REFS, NOW);
		ReflectionTestUtils.setField(plan, "id", 5L);
		List<PlanItem> planItems = plan.getItems();
		String[] titles = { "Write report", "Read 20 pages", "Learn Rust" };
		for (int i = 0; i < planItems.size(); i++) {
			ReflectionTestUtils.setField(planItems.get(i), "id", 21L + i);
			planItems.get(i).refreshTitle(titles[i]);
		}
		planItems.get(0).setDone(true);
		List<PlanView.Item> items = List.of(
				new PlanView.Item(planItems.get(0), "Write report", false),
				new PlanView.Item(planItems.get(1), "Read 20 pages", false),
				new PlanView.Item(planItems.get(2), "Learn Rust", true));
		return new PlanView(plan, items, new PlanProgress(PlanStatus.IN_PROGRESS, 1, 3, 33, 7200L));
	}

	private void givenAnyCreateReturnsView() {
		given(this.planService.create(anyString(), anyInt(), any(), any(), anyInt(), anyList()))
			.willReturn(view("Weekly focus", 1));
	}

	@Test
	@DisplayName("US4: POST /api/plans creates a plan and returns 201 with computed fields")
	void createPlanReturns201WithComputedFields() {
		given(this.planService.create(eq("Weekly focus"), eq(120), eq(START), eq(END), eq(1), eq(REFS)))
			.willReturn(view("Weekly focus", 1));

		var result = assertThat(this.mvc.post().uri("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY));
		result.hasStatus(201).hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(5);
		json.extractingPath("$.title").isEqualTo("Weekly focus");
		json.extractingPath("$.items").asArray().hasSize(3);
		json.extractingPath("$.items[0].id").isEqualTo(21);
		json.extractingPath("$.items[0].sourceType").isEqualTo("TASK");
		json.extractingPath("$.items[0].sourceId").isEqualTo(11);
		json.extractingPath("$.items[0].sourceTitle").isEqualTo("Write report");
		json.extractingPath("$.items[0].sourceRemoved").isEqualTo(false);
		json.extractingPath("$.items[0].done").isEqualTo(true);
		json.extractingPath("$.items[1].id").isEqualTo(22);
		json.extractingPath("$.items[1].sourceType").isEqualTo("HABIT");
		json.extractingPath("$.items[1].sourceId").isEqualTo(12);
		json.extractingPath("$.items[1].sourceTitle").isEqualTo("Read 20 pages");
		json.extractingPath("$.items[1].done").isEqualTo(false);
		json.extractingPath("$.items[2].id").isEqualTo(23);
		json.extractingPath("$.items[2].sourceType").isEqualTo("LEARNING_RESOURCE");
		json.extractingPath("$.items[2].sourceId").isEqualTo(13);
		json.extractingPath("$.items[2].sourceTitle").isEqualTo("Learn Rust");
		json.extractingPath("$.items[2].sourceRemoved").isEqualTo(true);
		json.extractingPath("$.items[2].done").isEqualTo(false);
		json.extractingPath("$.estimatedDurationMinutes").isEqualTo(120);
		json.extractingPath("$.startDateTime").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.endDateTime").isEqualTo("2026-09-24T14:00:00+03:00");
		json.extractingPath("$.createdAt").isEqualTo("2026-09-24T12:00:00+03:00");
		json.extractingPath("$.priorityOrder").isEqualTo(1);
		json.extractingPath("$.status").isEqualTo("IN_PROGRESS");
		json.extractingPath("$.doneItems").isEqualTo(1);
		json.extractingPath("$.totalItems").isEqualTo(3);
		json.extractingPath("$.progressPercent").isEqualTo(33);
		json.extractingPath("$.restSeconds").isEqualTo(7200);

		then(this.planService).should().create("Weekly focus", 120, START, END, 1, REFS);
	}

	@Test
	@DisplayName("BR-10: a plan without items is rejected with 400 and field items")
	void br10_emptyItemsIs400WithItemsField() {
		var result = assertThat(this.mvc.post().uri("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Weekly focus","items":[],"estimatedDurationMinutes":120,
						 "startDateTime":"2026-09-24T12:00:00+03:00","endDateTime":"2026-09-24T14:00:00+03:00",
						 "priorityOrder":1}
						"""));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[*].field").asArray().contains("items");

		verifyNoInteractions(this.planService);
	}

	@Test
	@DisplayName("BR-10: an item whose source doesn't exist is rejected with 400 and its sourceId field")
	void br10_unknownSourceIs400() {
		given(this.planService.create(anyString(), anyInt(), any(), any(), anyInt(), anyList()))
			.willThrow(new ValidationException("items[0].sourceId", "source not found"));

		var result = assertThat(this.mvc.post().uri("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("items[0].sourceId");
	}

	@Test
	@DisplayName("BR-11, FR-07.4: an end not after the start is rejected with 400 and field endDateTime")
	void br11_endNotAfterStartIs400WithEndDateTimeField() {
		given(this.planService.create(anyString(), anyInt(), any(), any(), anyInt(), anyList()))
			.willThrow(new ValidationException("endDateTime", "must be after startDateTime"));

		var result = assertThat(this.mvc.post().uri("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Weekly focus","items":[{"sourceType":"TASK","sourceId":11}],
						 "estimatedDurationMinutes":120,
						 "startDateTime":"2026-09-24T12:00:00+03:00","endDateTime":"2026-09-24T12:00:00+03:00",
						 "priorityOrder":1}
						"""));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("endDateTime");
	}

	@Test
	@DisplayName("US4: offset date-times in any offset reach the service as the same instant and return in the app zone")
	void offsetDateTimeParsedAndReturnedInAppZone() {
		givenAnyCreateReturnsView();

		var utc = assertThat(this.mvc.post().uri("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY.replace("\"startDateTime\":\"2026-09-24T12:00:00+03:00\"",
						"\"startDateTime\":\"2026-09-24T09:00:00Z\"")));
		utc.hasStatus(201);
		utc.bodyJson().extractingPath("$.startDateTime").isEqualTo("2026-09-24T12:00:00+03:00");

		var local = assertThat(this.mvc.post().uri("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY));
		local.hasStatus(201);
		local.bodyJson().extractingPath("$.startDateTime").isEqualTo("2026-09-24T12:00:00+03:00");

		ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
		then(this.planService).should(times(2))
			.create(anyString(), anyInt(), start.capture(), any(), anyInt(), anyList());
		assertThat(start.getAllValues()).containsExactly(START, START);
	}

	@Test
	@DisplayName("US4: a date-time without an offset is rejected with 400 and field startDateTime")
	void dateTimeWithoutOffsetIs400() {
		var result = assertThat(this.mvc.post().uri("/api/plans")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY.replace("\"startDateTime\":\"2026-09-24T12:00:00+03:00\"",
						"\"startDateTime\":\"2026-09-24T12:00:00\"")));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("startDateTime");

		verifyNoInteractions(this.planService);
	}

	@Test
	@DisplayName("US4: GET /api/plans?group=active lists the active plans")
	void listPlansGroupActive() {
		given(this.planService.list(PlanGroup.ACTIVE)).willReturn(List.of(view("Weekly focus", 1)));

		var result = assertThat(this.mvc.get().uri("/api/plans?group=active"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$[0].id").isEqualTo(5);
		result.bodyJson().extractingPath("$[0].items[0].id").isEqualTo(21);

		then(this.planService).should().list(PlanGroup.ACTIVE);
	}

	@Test
	@DisplayName("US4: GET /api/plans?group=completed lists the completed plans")
	void listPlansGroupCompleted() {
		given(this.planService.list(PlanGroup.COMPLETED)).willReturn(List.of());

		var result = assertThat(this.mvc.get().uri("/api/plans?group=completed"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$").asArray().isEmpty();

		then(this.planService).should().list(PlanGroup.COMPLETED);
	}

	@Test
	@DisplayName("US4: GET /api/plans without group lists all plans")
	void listPlansDefaultAll() {
		given(this.planService.list(PlanGroup.ALL)).willReturn(List.of(view("Weekly focus", 1)));

		var result = assertThat(this.mvc.get().uri("/api/plans"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$[0].id").isEqualTo(5);

		then(this.planService).should().list(PlanGroup.ALL);
	}

	@Test
	@DisplayName("US4: an unknown group is rejected with 400 and field group")
	void listPlansUnknownGroupIs400() {
		var result = assertThat(this.mvc.get().uri("/api/plans?group=foo"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("group");

		verifyNoInteractions(this.planService);
	}

	@Test
	@DisplayName("US4: GET /api/plans/{id} for an unknown id is 404 problem")
	void getPlanUnknownIs404() {
		given(this.planService.get(99L)).willThrow(new NotFoundException("Plan 99 not found"));

		var result = assertThat(this.mvc.get().uri("/api/plans/99"));
		result.hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.status").isEqualTo(404);
		result.bodyJson().extractingPath("$.detail").isEqualTo("Plan 99 not found");
	}

	@Test
	@DisplayName("US4: PUT /api/plans/{id} updates the plan and returns 200")
	void updatePlanReturns200() {
		given(this.planService.update(5L, "Weekly focus v2", 120, START, END, 2))
			.willReturn(view("Weekly focus v2", 2));

		var result = assertThat(this.mvc.put().uri("/api/plans/5")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Weekly focus v2","estimatedDurationMinutes":120,
						 "startDateTime":"2026-09-24T12:00:00+03:00","endDateTime":"2026-09-24T14:00:00+03:00",
						 "priorityOrder":2}
						"""));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		result.bodyJson().extractingPath("$.id").isEqualTo(5);
		result.bodyJson().extractingPath("$.title").isEqualTo("Weekly focus v2");
		result.bodyJson().extractingPath("$.priorityOrder").isEqualTo(2);
		result.bodyJson().extractingPath("$.items").asArray().hasSize(3);

		then(this.planService).should().update(5L, "Weekly focus v2", 120, START, END, 2);
	}

	@Test
	@DisplayName("US4: PUT /api/plans/{id}/items/{itemId} ticks the item and returns the plan")
	void setPlanItemDoneReturnsPlan() {
		given(this.planService.setItemDone(5L, 21L, true)).willReturn(view("Weekly focus", 1));

		var result = assertThat(this.mvc.put().uri("/api/plans/5/items/21")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"done\":true}"));
		result.hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
		var json = result.bodyJson();
		json.extractingPath("$.id").isEqualTo(5);
		json.extractingPath("$.items[0].id").isEqualTo(21);
		json.extractingPath("$.items[0].done").isEqualTo(true);
		json.extractingPath("$.doneItems").isEqualTo(1);
		json.extractingPath("$.totalItems").isEqualTo(3);
		json.extractingPath("$.progressPercent").isEqualTo(33);

		then(this.planService).should().setItemDone(5L, 21L, true);
	}

	@Test
	@DisplayName("US4: a missing done on plan item update is rejected with 400 and field done")
	void setPlanItemDoneMissingDoneIs400() {
		var result = assertThat(this.mvc.put().uri("/api/plans/5/items/21")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("done");

		verifyNoInteractions(this.planService);
	}

	@Test
	@DisplayName("BR-14: DELETE /api/plans/{id} returns 204 with no body")
	void deletePlanIs204() {
		var result = assertThat(this.mvc.delete().uri("/api/plans/5"));
		result.hasStatus(204);
		result.body().isEmpty();

		then(this.planService).should().delete(5L);
	}

}
