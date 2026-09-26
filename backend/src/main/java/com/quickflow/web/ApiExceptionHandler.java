package com.quickflow.web;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.quickflow.domain.common.ConflictException;
import com.quickflow.domain.common.FieldError;
import com.quickflow.domain.common.NotFoundException;
import com.quickflow.domain.common.ValidationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps domain and request errors to RFC 9457 problem details (research R-9). */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ValidationException.class)
	ProblemDetail validation(ValidationException ex) {
		return badRequest(ex.getMessage(), ex.getErrors());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail beanValidation(MethodArgumentNotValidException ex) {
		List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
				.toList();
		return badRequest("Validation failed", errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadable(HttpMessageNotReadableException ex) {
		if (ex.getCause() instanceof JacksonException je && !je.getPath().isEmpty()) {
			String field = je.getPath().stream()
					.map(r -> r.getPropertyName() != null ? r.getPropertyName() : "[" + r.getIndex() + "]")
					.collect(Collectors.joining("."))
					.replace(".[", "[");
			return badRequest("Malformed request body", List.of(new FieldError(field, message(je))));
		}
		return badRequest("Malformed request body", List.of());
	}

	/** A query or path parameter that doesn't convert (bad enum, date, boolean or id), A-10. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail typeMismatch(MethodArgumentTypeMismatchException ex) {
		Class<?> type = ex.getRequiredType();
		String message = type != null && type.isEnum()
				? "must be one of " + Arrays.toString(type.getEnumConstants())
				: "invalid value";
		return badRequest("Invalid parameter", List.of(new FieldError(ex.getName(), message)));
	}

	/** A required query parameter that is absent (e.g. {@code tag} on removeTaskTag). */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail missingParameter(MissingServletRequestParameterException ex) {
		return badRequest("Invalid parameter", List.of(new FieldError(ex.getParameterName(), "must not be blank")));
	}

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, ex.getMessage());
	}

	private static String message(JacksonException ex) {
		if (ex instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
			return "must be one of " + Arrays.toString(ife.getTargetType().getEnumConstants());
		}
		return "invalid value";
	}

	private static ProblemDetail badRequest(String detail, List<FieldError> errors) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, detail);
		if (!errors.isEmpty()) {
			problem.setProperty("errors", errors);
		}
		return problem;
	}

	private static ProblemDetail problem(HttpStatus status, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		return problem;
	}

}
