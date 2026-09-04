package com.chiwonpark9.cardrecommendation.auth.config;

import com.chiwonpark9.cardrecommendation.auth.security.RestAccessDeniedHandler;
import com.chiwonpark9.cardrecommendation.auth.security.RestAuthenticationEntryPoint;
import com.chiwonpark9.cardrecommendation.auth.security.SecurityErrorResponseWriter;
import com.chiwonpark9.cardrecommendation.common.error.ApiProblemDetailFactory;
import com.chiwonpark9.cardrecommendation.system.api.HealthController;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({
		SecurityConfig.class,
		ApiProblemDetailFactory.class,
		SecurityErrorResponseWriter.class,
		RestAuthenticationEntryPoint.class,
		RestAccessDeniedHandler.class
})
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsPublicHealthRequestWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void returnsProblemDetailWhenAuthenticationIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/missing"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(header().doesNotExist("Location"))
				.andExpect(header().doesNotExist("Set-Cookie"))
				.andExpect(jsonPath("$.type")
						.value("urn:mydata-card-recommendation:problem:auth-authentication-required"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
				.andExpect(jsonPath("$.instance").value("/api/v1/missing"))
				.andExpect(jsonPath("$.code").value("AUTH_AUTHENTICATION_REQUIRED"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors", hasSize(0)));
	}

	@Test
	void keepsMvcNotFoundResponseForAuthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/v1/missing").with(user("test-user")))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"));
	}

	@Test
	void returnsProblemDetailWhenRoleIsInsufficient() throws Exception {
		mockMvc.perform(get("/actuator/info").with(user("test-user").roles("CUSTOMER")))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
				.andExpect(jsonPath("$.instance").value("/actuator/info"))
				.andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(0)));
	}
}
