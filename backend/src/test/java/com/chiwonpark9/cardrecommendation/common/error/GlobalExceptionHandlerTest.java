package com.chiwonpark9.cardrecommendation.common.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestErrorController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void returnsFieldErrorsForInvalidRequestBody() throws Exception {
		mockMvc.perform(post("/test/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "",
							  "quantity": 0
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type")
						.value("urn:mydata-card-recommendation:problem:common-invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.instance").value("/test/validation"))
				.andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors", hasSize(2)))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
				.andExpect(jsonPath("$.fieldErrors[0].reason").value("이름은 필수입니다."))
				.andExpect(jsonPath("$.fieldErrors[1].field").value("quantity"))
				.andExpect(jsonPath("$.fieldErrors[1].reason").value("수량은 1 이상이어야 합니다."));
	}

	@Test
	void hidesParserDetailsForMalformedJson() throws Exception {
		mockMvc.perform(post("/test/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_MALFORMED_JSON"))
				.andExpect(jsonPath("$.detail").value("요청 본문을 읽을 수 없습니다."))
				.andExpect(jsonPath("$.fieldErrors", hasSize(0)))
				.andExpect(content().string(not(containsString("JsonEOFException"))));
	}

	@Test
	void mapsUnsupportedContentTypeToStableCode() throws Exception {
		mockMvc.perform(post("/test/validation")
					.contentType(MediaType.TEXT_PLAIN)
					.content("name=test"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("COMMON_UNSUPPORTED_MEDIA_TYPE"))
				.andExpect(jsonPath("$.status").value(415));
	}

	@Test
	void returnsFieldErrorForInvalidRequestParameter() throws Exception {
		mockMvc.perform(get("/test/quantity").param("quantity", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"))
				.andExpect(jsonPath("$.fieldErrors[0].reason").value("수량은 1 이상이어야 합니다."));
	}

	@Test
	void mapsExpectedApiExceptionToStableCode() throws Exception {
		mockMvc.perform(get("/test/resource"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("요청한 리소스를 찾을 수 없습니다."));
	}

	@Test
	void mapsUnsupportedMethodToProblemDetail() throws Exception {
		mockMvc.perform(post("/test/resource"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("COMMON_METHOD_NOT_ALLOWED"))
				.andExpect(jsonPath("$.status").value(405));
	}

	@Test
	void hidesUnexpectedExceptionDetails() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("COMMON_INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.detail").value("요청을 처리하는 중 문제가 발생했습니다."))
				.andExpect(content().string(not(containsString("sensitive internal detail"))));
	}

	@RestController
	@RequestMapping("/test")
	static class TestErrorController {

		@PostMapping("/validation")
		void validate(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/resource")
		void resource() {
			throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND);
		}

		@GetMapping("/quantity")
		void quantity(
				@RequestParam
				@Min(value = 1, message = "수량은 1 이상이어야 합니다.") int quantity
		) {
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("sensitive internal detail");
		}
	}

	record TestRequest(
			@NotBlank(message = "이름은 필수입니다.") String name,
			@Min(value = 1, message = "수량은 1 이상이어야 합니다.") int quantity
	) {
	}
}
