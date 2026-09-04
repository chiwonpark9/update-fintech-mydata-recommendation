package com.chiwonpark9.cardrecommendation.auth.api;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_ID;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_KEY;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.ROLES;

public record CurrentMemberResponse(
		long memberId,
		long partnerId,
		String partnerKey,
		List<String> roles
) {

	public CurrentMemberResponse {
		roles = List.copyOf(roles);
	}

	static CurrentMemberResponse from(Jwt jwt) {
		Number partnerId = jwt.getClaim(PARTNER_ID);
		return new CurrentMemberResponse(
				Long.parseLong(jwt.getSubject()),
				partnerId.longValue(),
				jwt.getClaimAsString(PARTNER_KEY),
				jwt.getClaimAsStringList(ROLES)
		);
	}
}
