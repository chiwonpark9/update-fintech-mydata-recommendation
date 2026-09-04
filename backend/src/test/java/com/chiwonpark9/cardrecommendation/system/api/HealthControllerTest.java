package com.chiwonpark9.cardrecommendation.system.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.chiwonpark9.cardrecommendation.common.error.ApiProblemDetailFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiProblemDetailFactory.class)
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsApplicationHealth() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.service").value("mydata-card-recommendation-api"));
	}

	@Test
	void returnsProblemDetailForUnknownApiPath() throws Exception {
		mockMvc.perform(get("/api/v1/missing"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"))
				.andExpect(jsonPath("$.instance").value("/api/v1/missing"));
	}
}
