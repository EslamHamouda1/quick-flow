package com.quickflow.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickflow.domain.common.ConflictException;
import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ApiExceptionHandlerTest.TestController.class)
@Import({ApiExceptionHandlerTest.TestController.class, ApiExceptionHandler.class})
class ApiExceptionHandlerTest {

	@Autowired
	MockMvcTester mvc;

	enum Level { LOW, HIGH }

	record Body(@NotBlank String title, Level level) {}

	@RestController
	@RequestMapping("/test")
	static class TestController {

		@GetMapping("/validation")
		void validation() {
			throw new ValidationException("title", "must not be blank");
		}

		@GetMapping("/not-found")
		void notFound() {
			throw new NotFoundException("Task 7 not found");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ConflictException("Already completed");
		}

		@PostMapping("/body")
		Body body(@Valid @RequestBody Body body) {
			return body;
		}
	}

	@Test
	void validationExceptionIs400ProblemWithErrors() {
		var result = assertThat(mvc.get().uri("/test/validation"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
		result.bodyJson().extractingPath("$.status").isEqualTo(400);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("title");
		result.bodyJson().extractingPath("$.errors[0].message").isEqualTo("must not be blank");
	}

	@Test
	void notFoundIs404Problem() {
		var result = assertThat(mvc.get().uri("/test/not-found"));
		result.hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.title").isEqualTo("Not Found");
		result.bodyJson().extractingPath("$.status").isEqualTo(404);
		result.bodyJson().extractingPath("$.detail").isEqualTo("Task 7 not found");
	}

	@Test
	void conflictIs409Problem() {
		var result = assertThat(mvc.get().uri("/test/conflict"));
		result.hasStatus(409).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.title").isEqualTo("Conflict");
		result.bodyJson().extractingPath("$.status").isEqualTo(409);
	}

	@Test
	void malformedJsonIs400Problem() {
		var result = assertThat(mvc.post().uri("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": "));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
		result.bodyJson().extractingPath("$.status").isEqualTo(400);
	}

	@Test
	void unknownEnumValueIs400ProblemWithField() {
		var result = assertThat(mvc.post().uri("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"a\",\"level\":\"MEDIUM\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
		result.bodyJson().extractingPath("$.status").isEqualTo(400);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("level");
	}

	@Test
	void beanValidationIs400ProblemWithErrors() {
		var result = assertThat(mvc.post().uri("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"\"}"));
		result.hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		result.bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
		result.bodyJson().extractingPath("$.status").isEqualTo(400);
		result.bodyJson().extractingPath("$.errors[0].field").isEqualTo("title");
	}
}
