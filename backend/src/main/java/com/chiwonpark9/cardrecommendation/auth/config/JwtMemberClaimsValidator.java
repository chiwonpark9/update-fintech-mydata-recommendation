package com.chiwonpark9.cardrecommendation.auth.config;

import java.util.Collection;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.chiwonpark9.cardrecommendation.auth.domain.MemberRole;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_ID;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.PARTNER_KEY;
import static com.chiwonpark9.cardrecommendation.auth.security.AccessTokenClaimNames.ROLES;

final class JwtMemberClaimsValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_MEMBER_CLAIMS = new OAuth2Error(
			"invalid_token",
			"Required member claims are invalid",
			null
	);
	private static final Set<String> ALLOWED_ROLES = Arrays.stream(MemberRole.values())
			.map(MemberRole::name)
			.collect(Collectors.toUnmodifiableSet());

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (hasPositiveSubject(token)
				&& hasPositivePartnerId(token)
				&& hasPartnerKey(token)
				&& hasAllowedRoles(token)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(INVALID_MEMBER_CLAIMS);
	}

	private boolean hasPositiveSubject(Jwt token) {
		try {
			return Long.parseLong(token.getSubject()) > 0;
		} catch (NumberFormatException | NullPointerException exception) {
			return false;
		}
	}

	private boolean hasPositivePartnerId(Jwt token) {
		Object partnerId = token.getClaim(PARTNER_ID);
		return partnerId instanceof Number number && number.longValue() > 0;
	}

	private boolean hasPartnerKey(Jwt token) {
		Object partnerKey = token.getClaim(PARTNER_KEY);
		return partnerKey instanceof String value && !value.isBlank() && value.length() <= 64;
	}

	private boolean hasAllowedRoles(Jwt token) {
		Object roles = token.getClaim(ROLES);
		return roles instanceof Collection<?> values
				&& !values.isEmpty()
				&& values.stream().allMatch(value ->
						value instanceof String role && ALLOWED_ROLES.contains(role));
	}
}
