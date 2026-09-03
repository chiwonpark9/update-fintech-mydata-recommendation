package com.chiwonpark9.cardrecommendation.common.error;

import java.util.Objects;

public class ApiException extends RuntimeException {

	private final ApiErrorCode errorCode;

	public ApiException(ApiErrorCode errorCode) {
		super(Objects.requireNonNull(errorCode).detail());
		this.errorCode = errorCode;
	}

	public ApiErrorCode getErrorCode() {
		return errorCode;
	}
}
