package com.chiwonpark9.cardrecommendation.auth.security;

import java.security.Principal;

public record MemberPrincipal(
		long memberId,
		long partnerId,
		String partnerKey,
		String email,
		String displayName
) implements Principal {

	@Override
	public String getName() {
		return email;
	}
}
