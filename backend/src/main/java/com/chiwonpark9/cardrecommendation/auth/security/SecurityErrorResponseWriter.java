package com.chiwonpark9.cardrecommendation.auth.security;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.chiwonpark9.cardrecommendation.common.error.ApiErrorCode;
import com.chiwonpark9.cardrecommendation.common.error.ApiProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {

	private final ObjectMapper objectMapper;
	private final ApiProblemDetailFactory problemDetailFactory;

	public SecurityErrorResponseWriter(
			ObjectMapper objectMapper,
			ApiProblemDetailFactory problemDetailFactory
	) {
		this.objectMapper = objectMapper;
		this.problemDetailFactory = problemDetailFactory;
	}

	public void write(
			HttpServletRequest request,
			HttpServletResponse response,
			ApiErrorCode errorCode
	) throws IOException {
		ProblemDetail problem = problemDetailFactory.create(
				errorCode,
				URI.create(request.getRequestURI()),
				List.of()
		);

		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
