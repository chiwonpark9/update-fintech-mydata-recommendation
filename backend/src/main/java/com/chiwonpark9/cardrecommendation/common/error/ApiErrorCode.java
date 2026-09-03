package com.chiwonpark9.cardrecommendation.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ApiErrorCode {

	INVALID_REQUEST(
			"COMMON_INVALID_REQUEST",
			HttpStatus.BAD_REQUEST,
			"Invalid Request",
			"요청 값을 확인해주세요."
	),
	MALFORMED_JSON(
			"COMMON_MALFORMED_JSON",
			HttpStatus.BAD_REQUEST,
			"Malformed JSON",
			"요청 본문을 읽을 수 없습니다."
	),
	RESOURCE_NOT_FOUND(
			"COMMON_RESOURCE_NOT_FOUND",
			HttpStatus.NOT_FOUND,
			"Resource Not Found",
			"요청한 리소스를 찾을 수 없습니다."
	),
	METHOD_NOT_ALLOWED(
			"COMMON_METHOD_NOT_ALLOWED",
			HttpStatus.METHOD_NOT_ALLOWED,
			"Method Not Allowed",
			"지원하지 않는 HTTP 메서드입니다."
	),
	UNSUPPORTED_MEDIA_TYPE(
			"COMMON_UNSUPPORTED_MEDIA_TYPE",
			HttpStatus.UNSUPPORTED_MEDIA_TYPE,
			"Unsupported Media Type",
			"지원하지 않는 요청 형식입니다."
	),
	INTERNAL_SERVER_ERROR(
			"COMMON_INTERNAL_SERVER_ERROR",
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Internal Server Error",
			"요청을 처리하는 중 문제가 발생했습니다."
	);

	private final String code;
	private final HttpStatus status;
	private final String title;
	private final String detail;

	ApiErrorCode(String code, HttpStatus status, String title, String detail) {
		this.code = code;
		this.status = status;
		this.title = title;
		this.detail = detail;
	}

	public String code() {
		return code;
	}

	public HttpStatus status() {
		return status;
	}

	public String title() {
		return title;
	}

	public String detail() {
		return detail;
	}

	public static ApiErrorCode from(HttpStatusCode status) {
		return switch (status.value()) {
			case 404 -> RESOURCE_NOT_FOUND;
			case 405 -> METHOD_NOT_ALLOWED;
			case 415 -> UNSUPPORTED_MEDIA_TYPE;
			default -> status.is5xxServerError() ? INTERNAL_SERVER_ERROR : INVALID_REQUEST;
		};
	}
}
