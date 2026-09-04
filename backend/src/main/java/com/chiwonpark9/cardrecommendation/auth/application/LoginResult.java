package com.chiwonpark9.cardrecommendation.auth.application;

import java.util.List;

import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessToken;
import com.chiwonpark9.cardrecommendation.auth.security.MemberPrincipal;

public record LoginResult(
		JwtAccessToken accessToken,
		MemberPrincipal member,
		List<String> roles
) {

	public LoginResult {
		roles = List.copyOf(roles);
	}
}
