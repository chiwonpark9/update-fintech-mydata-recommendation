package com.chiwonpark9.cardrecommendation.common.error;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String DEFAULT_VALIDATION_REASON = "올바른 값을 입력해주세요.";
	private final ApiProblemDetailFactory problemDetailFactory;

	public GlobalExceptionHandler(ApiProblemDetailFactory problemDetailFactory) {
		this.problemDetailFactory = problemDetailFactory;
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Object> handleApiException(ApiException exception, WebRequest request) {
		return createErrorResponse(exception.getErrorCode(), HttpHeaders.EMPTY, request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
		log.error("Unhandled API exception for {}", request.getDescription(false), exception);
		return createErrorResponse(
				ApiErrorCode.INTERNAL_SERVER_ERROR,
				HttpHeaders.EMPTY,
				request,
				List.of()
		);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		List<ApiFieldError> fieldErrors = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::toApiFieldError)
				.sorted(fieldErrorComparator())
				.toList();

		return createErrorResponse(ApiErrorCode.INVALID_REQUEST, headers, request, fieldErrors);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		List<ApiFieldError> fieldErrors = exception.getParameterValidationResults()
				.stream()
				.flatMap(result -> result.getResolvableErrors()
						.stream()
						.map(error -> toApiFieldError(result, error)))
				.sorted(fieldErrorComparator())
				.toList();

		return createErrorResponse(ApiErrorCode.INVALID_REQUEST, headers, request, fieldErrors);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		return createErrorResponse(ApiErrorCode.MALFORMED_JSON, headers, request, List.of());
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		return createErrorResponse(ApiErrorCode.METHOD_NOT_ALLOWED, headers, request, List.of());
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		return createErrorResponse(ApiErrorCode.UNSUPPORTED_MEDIA_TYPE, headers, request, List.of());
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(
			NoResourceFoundException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		return createErrorResponse(ApiErrorCode.RESOURCE_NOT_FOUND, headers, request, List.of());
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception exception,
			Object body,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		return createErrorResponse(ApiErrorCode.from(status), headers, request, List.of());
	}

	private ResponseEntity<Object> createErrorResponse(
			ApiErrorCode errorCode,
			HttpHeaders headers,
			WebRequest request,
			List<ApiFieldError> fieldErrors
	) {
		ProblemDetail problem = problemDetailFactory.create(errorCode, requestUri(request), fieldErrors);

		return createResponseEntity(problem, headers, errorCode.status(), request);
	}

	private ApiFieldError toApiFieldError(FieldError error) {
		return new ApiFieldError(error.getField(), reason(error));
	}

	private ApiFieldError toApiFieldError(
			ParameterValidationResult result,
			MessageSourceResolvable error
	) {
		String parameterName = result.getMethodParameter().getParameterName();
		String field = parameterName != null
				? parameterName
				: "argument" + result.getMethodParameter().getParameterIndex();
		return new ApiFieldError(field, reason(error));
	}

	private String reason(MessageSourceResolvable error) {
		return error.getDefaultMessage() != null ? error.getDefaultMessage() : DEFAULT_VALIDATION_REASON;
	}

	private Comparator<ApiFieldError> fieldErrorComparator() {
		return Comparator.comparing(ApiFieldError::field).thenComparing(ApiFieldError::reason);
	}

	private URI requestUri(WebRequest request) {
		if (request instanceof ServletWebRequest servletWebRequest) {
			return URI.create(servletWebRequest.getRequest().getRequestURI());
		}
		return URI.create("/");
	}
}
