package com.chiwonpark9.cardrecommendation.auth.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

public interface JwtAccessTokenIssuer {

	JwtAccessToken issue(
			MemberPrincipal principal,
			Collection<? extends GrantedAuthority> authorities
	);
}
