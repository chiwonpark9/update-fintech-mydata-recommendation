package com.chiwonpark9.cardrecommendation.auth.application;

import java.time.Instant;
import java.util.List;

import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessToken;
import com.chiwonpark9.cardrecommendation.auth.security.JwtAccessTokenIssuer;
import com.chiwonpark9.cardrecommendation.auth.security.MemberPrincipal;
import com.chiwonpark9.cardrecommendation.auth.security.PartnerEmailPasswordAuthenticationToken;
import com.chiwonpark9.cardrecommendation.common.error.ApiErrorCode;
import com.chiwonpark9.cardrecommendation.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LoginServiceTest {

	private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
	private final JwtAccessTokenIssuer accessTokenIssuer = mock(JwtAccessTokenIssuer.class);
	private final LoginService loginService = new LoginService(authenticationManager, accessTokenIssuer);

	@Test
	void authenticatesCredentialsAndIssuesAccessToken() {
		MemberPrincipal principal = new MemberPrincipal(
				10L,
				20L,
				"woori-card",
				"user@example.com",
				"테스트 사용자"
		);
		Authentication authenticated = PartnerEmailPasswordAuthenticationToken.authenticated(
				principal,
				List.of(
						new SimpleGrantedAuthority("ROLE_PARTNER_ADMIN"),
						new SimpleGrantedAuthority("ROLE_CUSTOMER")
				)
		);
		JwtAccessToken accessToken = new JwtAccessToken(
				"signed-token",
				Instant.parse("2026-09-04T00:00:00Z"),
				Instant.parse("2026-09-04T00:15:00Z")
		);
		given(authenticationManager.authenticate(any())).willReturn(authenticated);
		given(accessTokenIssuer.issue(principal, authenticated.getAuthorities()))
				.willReturn(accessToken);

		LoginResult result = loginService.login(
				new LoginCommand("woori-card", "user@example.com", "correct-password")
		);

		assertThat(result.accessToken()).isEqualTo(accessToken);
		assertThat(result.member()).isEqualTo(principal);
		assertThat(result.roles()).containsExactly("CUSTOMER", "PARTNER_ADMIN");
		ArgumentCaptor<Authentication> requestCaptor = ArgumentCaptor.forClass(Authentication.class);
		verify(authenticationManager).authenticate(requestCaptor.capture());
		PartnerEmailPasswordAuthenticationToken request =
				(PartnerEmailPasswordAuthenticationToken) requestCaptor.getValue();
		assertThat(request.getPartnerKey()).isEqualTo("woori-card");
		assertThat(request.getPrincipal()).isEqualTo("user@example.com");
		assertThat(request.getCredentials()).isEqualTo("correct-password");
		String safeDescription = new LoginCommand(
				"woori-card",
				"user@example.com",
				"correct-password"
		).toString();
		assertThat(safeDescription)
				.contains("email=[PROTECTED]")
				.contains("password=[PROTECTED]")
				.doesNotContain("user@example.com")
				.doesNotContain("correct-password");
	}

	@Test
	void mapsEveryAuthenticationFailureToSafeApiError() {
		given(authenticationManager.authenticate(any()))
				.willThrow(new BadCredentialsException("internal reason"));

		assertThatThrownBy(() -> loginService.login(
				new LoginCommand("woori-card", "user@example.com", "wrong-password")
		))
				.isInstanceOfSatisfying(ApiException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ApiErrorCode.INVALID_CREDENTIALS))
				.hasMessage("로그인 정보가 올바르지 않습니다.");
		verify(accessTokenIssuer, never()).issue(any(), any());
	}
}
