package com.chiwonpark9.cardrecommendation.auth.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public final class PartnerEmailPasswordAuthenticationToken extends AbstractAuthenticationToken {

	private final String partnerKey;
	private final Object principal;
	private Object credentials;

	private PartnerEmailPasswordAuthenticationToken(
			String partnerKey,
			Object principal,
			Object credentials,
			Collection<? extends GrantedAuthority> authorities,
			boolean authenticated
	) {
		super(authorities);
		this.partnerKey = partnerKey;
		this.principal = principal;
		this.credentials = credentials;
		super.setAuthenticated(authenticated);
	}

	public static PartnerEmailPasswordAuthenticationToken unauthenticated(
			String partnerKey,
			String email,
			String rawPassword
	) {
		return new PartnerEmailPasswordAuthenticationToken(
				partnerKey,
				email,
				rawPassword,
				null,
				false
		);
	}

	public static PartnerEmailPasswordAuthenticationToken authenticated(
			MemberPrincipal principal,
			Collection<? extends GrantedAuthority> authorities
	) {
		return new PartnerEmailPasswordAuthenticationToken(
				principal.partnerKey(),
				principal,
				null,
				authorities,
				true
		);
	}

	public String getPartnerKey() {
		return partnerKey;
	}

	@Override
	public Object getCredentials() {
		return credentials;
	}

	@Override
	public Object getPrincipal() {
		return principal;
	}

	@Override
	public void setAuthenticated(boolean authenticated) {
		if (authenticated) {
			throw new IllegalArgumentException("Use the authenticated factory method");
		}
		super.setAuthenticated(false);
	}

	@Override
	public void eraseCredentials() {
		super.eraseCredentials();
		credentials = null;
	}
}
