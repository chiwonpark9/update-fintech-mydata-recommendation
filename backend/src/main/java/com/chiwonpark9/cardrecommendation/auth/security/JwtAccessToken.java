package com.chiwonpark9.cardrecommendation.auth.security;

import java.time.Duration;
import java.time.Instant;

public record JwtAccessToken(
		String value,
		Instant issuedAt,
		Instant expiresAt
) {

	public long expiresInSeconds() {
		return Duration.between(issuedAt, expiresAt).toSeconds();
	}

	@Override
	public String toString() {
		return "JwtAccessToken[value=[PROTECTED], issuedAt=" + issuedAt
				+ ", expiresAt=" + expiresAt + "]";
	}
}
