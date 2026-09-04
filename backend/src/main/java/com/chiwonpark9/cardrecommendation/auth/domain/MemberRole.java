package com.chiwonpark9.cardrecommendation.auth.domain;

public enum MemberRole {

	CUSTOMER,
	PARTNER_ADMIN,
	PLATFORM_ADMIN;

	public String authority() {
		return "ROLE_" + name();
	}
}
