package com.chiwonpark9.cardrecommendation.auth.domain;

import java.util.Set;

import com.chiwonpark9.cardrecommendation.tenant.domain.PartnerStatus;

public record MemberCredentials(
		long memberId,
		long partnerId,
		String partnerKey,
		String email,
		String passwordHash,
		String displayName,
		MemberStatus memberStatus,
		PartnerStatus partnerStatus,
		Set<MemberRole> roles
) {

	public MemberCredentials {
		roles = Set.copyOf(roles);
	}

	public boolean canAuthenticate() {
		return memberStatus == MemberStatus.ACTIVE
				&& partnerStatus == PartnerStatus.ACTIVE
				&& !roles.isEmpty();
	}
}
