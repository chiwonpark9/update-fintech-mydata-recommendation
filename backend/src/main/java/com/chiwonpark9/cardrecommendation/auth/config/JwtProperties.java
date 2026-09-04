package com.chiwonpark9.cardrecommendation.auth.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotBlank String audience,
		@NotNull Duration accessTokenTtl,
		@NotBlank String keyId,
		@NotBlank String publicKeyBase64,
		@NotBlank String privateKeyBase64
) {

	private static final Duration MINIMUM_ACCESS_TOKEN_TTL = Duration.ofMinutes(1);
	private static final Duration MAXIMUM_ACCESS_TOKEN_TTL = Duration.ofHours(1);

	public JwtProperties {
		if (accessTokenTtl != null
				&& (accessTokenTtl.compareTo(MINIMUM_ACCESS_TOKEN_TTL) < 0
				|| accessTokenTtl.compareTo(MAXIMUM_ACCESS_TOKEN_TTL) > 0)) {
			throw new IllegalArgumentException(
					"JWT access token TTL must be between 1 minute and 1 hour"
			);
		}
	}
}
