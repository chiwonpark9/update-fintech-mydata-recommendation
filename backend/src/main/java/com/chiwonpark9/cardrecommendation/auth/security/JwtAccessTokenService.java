package com.chiwonpark9.cardrecommendation.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.chiwonpark9.cardrecommendation.auth.config.JwtProperties;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_ID;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_KEY;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.ROLES;

@Service
public class JwtAccessTokenService implements JwtAccessTokenIssuer {

	private static final String ROLE_PREFIX = "ROLE_";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtAccessTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock jwtClock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = jwtClock;
	}

	@Override
	public JwtAccessToken issue(
			MemberPrincipal principal,
			Collection<? extends GrantedAuthority> authorities
	) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
		List<String> roles = roles(authorities);
		if (roles.isEmpty()) {
			throw new IllegalArgumentException("Access token requires at least one role");
		}

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(Long.toString(principal.memberId()))
				.audience(List.of(properties.audience()))
				.issuedAt(issuedAt)
				.notBefore(issuedAt)
				.expiresAt(expiresAt)
				.id(UUID.randomUUID().toString())
				.claim(PARTNER_ID, principal.partnerId())
				.claim(PARTNER_KEY, principal.partnerKey())
				.claim(ROLES, roles)
				.build();
		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
				.type("JWT")
				.keyId(properties.keyId())
				.build();

		String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
		return new JwtAccessToken(tokenValue, issuedAt, expiresAt);
	}

	private List<String> roles(Collection<? extends GrantedAuthority> authorities) {
		return authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith(ROLE_PREFIX))
				.map(authority -> authority.substring(ROLE_PREFIX.length()))
				.sorted()
				.toList();
	}
}
