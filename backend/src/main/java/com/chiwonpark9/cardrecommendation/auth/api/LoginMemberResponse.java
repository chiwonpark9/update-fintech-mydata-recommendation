package com.chiwonpark9.cardrecommendation.auth.api;

import java.util.List;

import com.chiwonpark9.cardrecommendation.auth.application.LoginResult;

public record LoginMemberResponse(
		long memberId,
		long partnerId,
		String partnerKey,
		String email,
		String displayName,
		List<String> roles
) {

	public LoginMemberResponse {
		roles = List.copyOf(roles);
	}

	static LoginMemberResponse from(LoginResult result) {
		return new LoginMemberResponse(
				result.member().memberId(),
				result.member().partnerId(),
				result.member().partnerKey(),
				result.member().email(),
				result.member().displayName(),
				result.roles()
		);
	}
}
