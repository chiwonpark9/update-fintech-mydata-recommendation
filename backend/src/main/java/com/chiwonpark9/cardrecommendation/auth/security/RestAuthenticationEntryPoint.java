package com.chiwonpark9.cardrecommendation.auth.security;

import java.io.IOException;

import com.chiwonpark9.cardrecommendation.common.error.ApiErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityErrorResponseWriter errorResponseWriter;

	public RestAuthenticationEntryPoint(SecurityErrorResponseWriter errorResponseWriter) {
		this.errorResponseWriter = errorResponseWriter;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception
	) throws IOException, ServletException {
		errorResponseWriter.write(request, response, ApiErrorCode.AUTHENTICATION_REQUIRED);
	}
}
