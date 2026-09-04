package com.chiwonpark9.cardrecommendation.auth.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessToken;
import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessTokenService;
import com.chiwonpark9.cardrecommendation.auth.security.MemberPrincipal;
import com.chiwonpark9.cardrecommendation.auth.support.TestRsaKeys;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_ID;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_KEY;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.ROLES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
	private final JwtConfig jwtConfig = new JwtConfig();
	private final TestRsaKeys rsaKeys = TestRsaKeys.shared();

	@Test
	void issuesAndValidatesRs256AccessTokenWithMinimalClaims() {
		JwtProperties properties = properties("expected-audience", rsaKeys);
		JwtConfig.JwtKeyPair keyPair = jwtConfig.jwtKeyPair(properties);
		JwtAccessTokenService tokenService = new JwtAccessTokenService(
				jwtConfig.jwtEncoder(keyPair, properties),
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		MemberPrincipal principal = new MemberPrincipal(
				10L,
				20L,
				"woori-card",
				"private@example.com",
				"개인정보 이름"
		);

		JwtAccessToken accessToken = tokenService.issue(
				principal,
				List.of(
						new SimpleGrantedAuthority("ROLE_PARTNER_ADMIN"),
						new SimpleGrantedAuthority("ROLE_CUSTOMER")
				)
		);
		Jwt jwt = jwtConfig.jwtDecoder(keyPair, properties).decode(accessToken.value());

		assertThat(jwt.getHeaders())
				.containsEntry("alg", "RS256")
				.containsEntry("typ", "JWT")
				.containsEntry("kid", "test-key");
		assertThat(jwt.getIssuer().toString()).isEqualTo("https://mydata-card-recommendation.local");
		assertThat(jwt.getAudience()).containsExactly("expected-audience");
		assertThat(jwt.getSubject()).isEqualTo("10");
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getNotBefore()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(ACCESS_TOKEN_TTL));
		assertThat(jwt.getId()).isNotBlank();
		assertThat(jwt.<Number>getClaim(PARTNER_ID).longValue()).isEqualTo(20L);
		assertThat(jwt.getClaimAsString(PARTNER_KEY)).isEqualTo("woori-card");
		assertThat(jwt.getClaimAsStringList(ROLES))
				.containsExactly("CUSTOMER", "PARTNER_ADMIN");
		assertThat(jwt.getClaims()).doesNotContainKeys("email", "display_name", "password");
		assertThat(accessToken.expiresInSeconds()).isEqualTo(900L);
		assertThat(accessToken.toString())
				.contains("value=[PROTECTED]")
				.doesNotContain(accessToken.value());
	}

	@Test
	void rejectsTokenWhenAudienceDoesNotMatch() {
		JwtProperties issuerProperties = properties("issued-audience", rsaKeys);
		JwtConfig.JwtKeyPair keyPair = jwtConfig.jwtKeyPair(issuerProperties);
		JwtAccessTokenService tokenService = new JwtAccessTokenService(
				jwtConfig.jwtEncoder(keyPair, issuerProperties),
				issuerProperties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		JwtAccessToken token = tokenService.issue(
				new MemberPrincipal(10L, 20L, "woori-card", "user@example.com", "사용자"),
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
		);
		JwtProperties decoderProperties = properties("different-audience", rsaKeys);
		JwtDecoder decoder = jwtConfig.jwtDecoder(keyPair, decoderProperties);

		assertThatThrownBy(() -> decoder.decode(token.value()))
				.isInstanceOf(JwtValidationException.class)
				.hasMessageContaining("required audience");
	}

	@Test
	void rejectsTokenWhenIssuerDoesNotMatch() {
		JwtProperties unexpectedIssuer = properties(
				"https://unexpected-issuer.local",
				"expected-audience",
				rsaKeys
		);
		JwtConfig.JwtKeyPair keyPair = jwtConfig.jwtKeyPair(unexpectedIssuer);
		JwtAccessTokenService tokenService = new JwtAccessTokenService(
				jwtConfig.jwtEncoder(keyPair, unexpectedIssuer),
				unexpectedIssuer,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		JwtAccessToken token = tokenService.issue(
				new MemberPrincipal(10L, 20L, "woori-card", "user@example.com", "사용자"),
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
		);
		JwtProperties expectedIssuer = properties("expected-audience", rsaKeys);
		JwtDecoder decoder = jwtConfig.jwtDecoder(keyPair, expectedIssuer);

		assertThatThrownBy(() -> decoder.decode(token.value()))
				.isInstanceOf(JwtValidationException.class)
				.hasMessageContaining("iss claim");
	}

	@Test
	void rejectsExpiredAccessToken() {
		JwtProperties properties = properties("expected-audience", rsaKeys);
		JwtConfig.JwtKeyPair keyPair = jwtConfig.jwtKeyPair(properties);
		JwtAccessTokenService tokenService = new JwtAccessTokenService(
				jwtConfig.jwtEncoder(keyPair, properties),
				properties,
				Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC)
		);
		JwtAccessToken token = tokenService.issue(
				new MemberPrincipal(10L, 20L, "woori-card", "user@example.com", "사용자"),
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
		);

		assertThatThrownBy(() -> jwtConfig.jwtDecoder(keyPair, properties).decode(token.value()))
				.isInstanceOf(JwtValidationException.class)
				.hasMessageContaining("expired");
	}

	@Test
	void rejectsUnknownRoleInMemberClaims() {
		Jwt invalidClaims = Jwt.withTokenValue("test-token")
				.header("alg", "RS256")
				.claim(JwtClaimNames.SUB, "10")
				.claim(PARTNER_ID, 20L)
				.claim(PARTNER_KEY, "woori-card")
				.claim(ROLES, List.of("UNKNOWN_ROLE"))
				.build();

		assertThat(new JwtMemberClaimsValidator().validate(invalidClaims).hasErrors()).isTrue();
	}

	@Test
	void rejectsMismatchedRsaKeyPair() {
		TestRsaKeys otherKeys = TestRsaKeys.generate();
		JwtProperties mismatched = new JwtProperties(
				"https://mydata-card-recommendation.local",
				"expected-audience",
				ACCESS_TOKEN_TTL,
				"test-key",
				rsaKeys.publicKeyBase64(),
				otherKeys.privateKeyBase64()
		);

		assertThatThrownBy(() -> jwtConfig.jwtKeyPair(mismatched))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("JWT public and private keys do not form a pair");
	}

	@Test
	void rejectsRsaKeyShorterThan2048Bits() {
		TestRsaKeys shortKeys = TestRsaKeys.generate(1024);
		JwtProperties insecure = properties("expected-audience", shortKeys);

		assertThatThrownBy(() -> jwtConfig.jwtKeyPair(insecure))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("JWT RSA key must be at least 2048 bits");
	}

	@Test
	void rejectsAccessTokenTtlOutsideAllowedRange() {
		assertThatThrownBy(() -> new JwtProperties(
				"issuer",
				"audience",
				Duration.ofHours(2),
				"test-key",
				rsaKeys.publicKeyBase64(),
				rsaKeys.privateKeyBase64()
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 minute and 1 hour");
	}

	private JwtProperties properties(String audience, TestRsaKeys keys) {
		return properties("https://mydata-card-recommendation.local", audience, keys);
	}

	private JwtProperties properties(String issuer, String audience, TestRsaKeys keys) {
		return new JwtProperties(
				issuer,
				audience,
				ACCESS_TOKEN_TTL,
				"test-key",
				keys.publicKeyBase64(),
				keys.privateKeyBase64()
		);
	}
}
