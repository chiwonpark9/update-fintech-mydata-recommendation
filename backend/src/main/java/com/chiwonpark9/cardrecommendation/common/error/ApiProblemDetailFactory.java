package com.chiwonpark9.cardrecommendation.common.error;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ApiProblemDetailFactory {

	private static final String PROBLEM_TYPE_PREFIX = "urn:mydata-card-recommendation:problem:";

	public ProblemDetail create(
			ApiErrorCode errorCode,
			URI instance,
			List<ApiFieldError> fieldErrors
	) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), errorCode.detail());
		problem.setType(problemType(errorCode));
		problem.setTitle(errorCode.title());
		problem.setInstance(instance);
		problem.setProperty("code", errorCode.code());
		problem.setProperty("timestamp", Instant.now().toString());
		problem.setProperty("fieldErrors", fieldErrors);
		return problem;
	}

	private URI problemType(ApiErrorCode errorCode) {
		return URI.create(
				PROBLEM_TYPE_PREFIX + errorCode.code().toLowerCase(Locale.ROOT).replace('_', '-')
		);
	}
}
