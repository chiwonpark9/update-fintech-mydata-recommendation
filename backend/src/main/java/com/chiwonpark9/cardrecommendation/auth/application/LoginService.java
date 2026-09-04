package com.chiwonpark9.cardrecommendation.auth.application;

import java.util.List;

import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessToken;
import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessTokenIssuer;
import com.chiwonpark9.cardrecommendation.auth.security.MemberPrincipal;
import com.chiwonpark9.cardrecommendation.auth.security.PartnerEmailPasswordAuthenticationToken;
import com.chiwonpark9.cardrecommendation.common.error.ApiErrorCode;
import com.chiwonpark9.cardrecommendation.common.error.ApiException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

	private static final String ROLE_PREFIX = "ROLE_";

	private final AuthenticationManager authenticationManager;
	private final JwtAccessTokenIssuer accessTokenIssuer;

	public LoginService(
			AuthenticationManager authenticationManager,
			JwtAccessTokenIssuer accessTokenIssuer
	) {
		this.authenticationManager = authenticationManager;
		this.accessTokenIssuer = accessTokenIssuer;
	}

	public LoginResult login(LoginCommand command) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					PartnerEmailPasswordAuthenticationToken.unauthenticated(
							command.partnerKey(),
							command.email(),
							command.password()
					)
			);
			MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
			JwtAccessToken accessToken = accessTokenIssuer.issue(
					principal,
					authentication.getAuthorities()
			);
			return new LoginResult(accessToken, principal, roles(authentication));
		} catch (AuthenticationException exception) {
			throw new ApiException(ApiErrorCode.INVALID_CREDENTIALS);
		}
	}

	private List<String> roles(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith(ROLE_PREFIX))
				.map(authority -> authority.substring(ROLE_PREFIX.length()))
				.sorted()
				.toList();
	}
}
