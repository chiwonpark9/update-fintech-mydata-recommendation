package com.chiwonpark9.cardrecommendation.auth.api;

import java.time.Instant;
import java.util.List;

import com.chiwonpark9.cardrecommendation.auth.application.LoginResult;
import com.chiwonpark9.cardrecommendation.auth.application.LoginService;
import com.chiwonpark9.cardrecommendation.auth.config.SecurityConfig;
import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessToken;
import com.chiwonpark9.cardrecommendation.auth.security.MemberPrincipal;
import com.chiwonpark9.cardrecommendation.auth.security.RestAccessDeniedHandler;
import com.chiwonpark9.cardrecommendation.auth.security.RestAuthenticationEntryPoint;
import com.chiwonpark9.cardrecommendation.auth.security.SecurityErrorResponseWriter;
import com.chiwonpark9.cardrecommendation.common.error.ApiErrorCode;
import com.chiwonpark9.cardrecommendation.common.error.ApiException;
import com.chiwonpark9.cardrecommendation.common.error.ApiProblemDetailFactory;
import com.chiwonpark9.cardrecommendation.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_ID;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_KEY;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.ROLES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
		SecurityConfig.class,
		GlobalExceptionHandler.class,
		ApiProblemDetailFactory.class,
		SecurityErrorResponseWriter.class,
		RestAuthenticationEntryPoint.class,
		RestAccessDeniedHandler.class
})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private LoginService loginService;
	@MockitoBean
	private JwtDecoder jwtDecoder;
	@MockitoBean
	private JwtAuthenticationConverter jwtAuthenticationConverter;

	@Test
	void logsInWithoutExistingAuthenticationAndPreventsResponseCaching() throws Exception {
		Instant issuedAt = Instant.parse("2026-09-04T00:00:00Z");
		Instant expiresAt = issuedAt.plusSeconds(900);
		MemberPrincipal member = new MemberPrincipal(
				10L,
				20L,
				"woori-card",
				"user@example.com",
				"테스트 사용자"
		);
		LoginResult loginResult = new LoginResult(
				new JwtAccessToken("signed-access-token", issuedAt, expiresAt),
				member,
				List.of("CUSTOMER", "PARTNER_ADMIN")
		);
		given(loginService.login(any())).willReturn(loginResult);
		assertThat(LoginResponse.from(loginResult).toString())
				.contains("accessToken=[PROTECTED]")
				.doesNotContain("signed-access-token");

		mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "partnerKey": "woori-card",
							  "email": "user@example.com",
							  "password": "correct-password"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").value("signed-access-token"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.expiresAt").value("2026-09-04T00:15:00Z"))
				.andExpect(jsonPath("$.member.memberId").value(10))
				.andExpect(jsonPath("$.member.partnerId").value(20))
				.andExpect(jsonPath("$.member.partnerKey").value("woori-card"))
				.andExpect(jsonPath("$.member.email").value("user@example.com"))
				.andExpect(jsonPath("$.member.displayName").value("테스트 사용자"))
				.andExpect(jsonPath("$.member.roles[0]").value("CUSTOMER"))
				.andExpect(jsonPath("$.member.roles[1]").value("PARTNER_ADMIN"));
	}

	@Test
	void returnsSortedFieldErrorsForInvalidLoginRequest() throws Exception {
		assertThat(new LoginRequest("woori-card", "user@example.com", "correct-password").toString())
				.doesNotContain("user@example.com")
				.doesNotContain("correct-password")
				.contains("email=[PROTECTED]")
				.contains("password=[PROTECTED]");

		mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "partnerKey": "invalid key!",
							  "email": "not-an-email",
							  "password": "short"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(3)))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
				.andExpect(jsonPath("$.fieldErrors[1].field").value("partnerKey"))
				.andExpect(jsonPath("$.fieldErrors[2].field").value("password"));
		verifyNoInteractions(loginService);
	}

	@Test
	void returnsSafeProblemDetailForInvalidCredentials() throws Exception {
		given(loginService.login(any()))
				.willThrow(new ApiException(ApiErrorCode.INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "partnerKey": "woori-card",
							  "email": "user@example.com",
							  "password": "wrong-password"
							}
							"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.detail").value("로그인 정보가 올바르지 않습니다."))
				.andExpect(jsonPath("$.fieldErrors", hasSize(0)));
	}

	@Test
	void returnsCurrentMemberFromValidatedJwtClaims() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").with(jwt().jwt(token -> token
					.subject("10")
					.claim(PARTNER_ID, 20L)
					.claim(PARTNER_KEY, "woori-card")
					.claim(ROLES, List.of("CUSTOMER", "PARTNER_ADMIN")))))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(jsonPath("$.memberId").value(10))
				.andExpect(jsonPath("$.partnerId").value(20))
				.andExpect(jsonPath("$.partnerKey").value("woori-card"))
				.andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
				.andExpect(jsonPath("$.roles[1]").value("PARTNER_ADMIN"));
	}
}
