package com.chiwonpark9.cardrecommendation.auth.api;

import java.time.Instant;

import com.chiwonpark9.cardrecommendation.auth.application.LoginResult;

public record LoginResponse(
		String tokenType,
		String accessToken,
		long expiresIn,
		Instant expiresAt,
		LoginMemberResponse member
) {

	static LoginResponse from(LoginResult result) {
		return new LoginResponse(
				"Bearer",
				result.accessToken().value(),
				result.accessToken().expiresInSeconds(),
				result.accessToken().expiresAt(),
				LoginMemberResponse.from(result)
		);
	}

	@Override
	public String toString() {
		return "LoginResponse[tokenType=" + tokenType
				+ ", accessToken=[PROTECTED]"
				+ ", expiresIn=" + expiresIn
				+ ", expiresAt=" + expiresAt
				+ ", member=" + member + "]";
	}
}
