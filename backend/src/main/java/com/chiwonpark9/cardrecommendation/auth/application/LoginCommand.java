package com.chiwonpark9.cardrecommendation.auth.application;

public record LoginCommand(
		String partnerKey,
		String email,
		String password
) {

	@Override
	public String toString() {
		return "LoginCommand[partnerKey=" + partnerKey
				+ ", email=[PROTECTED]"
				+ ", password=[PROTECTED]]";
	}
}
