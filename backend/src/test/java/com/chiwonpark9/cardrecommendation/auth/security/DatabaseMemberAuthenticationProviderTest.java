package com.chiwonpark9.cardrecommendation.auth.security;

import java.util.Optional;
import java.util.Set;

import com.chiwonpark9.cardrecommendation.auth.application.port.MemberCredentialsRepository;
import com.chiwonpark9.cardrecommendation.auth.domain.MemberCredentials;
import com.chiwonpark9.cardrecommendation.auth.domain.MemberRole;
import com.chiwonpark9.cardrecommendation.auth.domain.MemberStatus;
import com.chiwonpark9.cardrecommendation.tenant.domain.PartnerStatus;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseMemberAuthenticationProviderTest {

	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Test
	void authenticatesActiveMemberAndErasesRawCredentials() {
		MemberCredentials member = member(MemberStatus.ACTIVE, PartnerStatus.ACTIVE);
		DatabaseMemberAuthenticationProvider provider = providerReturning(Optional.of(member));
		PartnerEmailPasswordAuthenticationToken request =
				PartnerEmailPasswordAuthenticationToken.unauthenticated(
						" WOORI-CARD ",
						" USER@EXAMPLE.COM ",
						"correct-password"
				);

		Authentication result = provider.authenticate(request);

		assertThat(result.isAuthenticated()).isTrue();
		assertThat(result.getPrincipal()).isEqualTo(new MemberPrincipal(
				10L,
				20L,
				"woori-card",
				"user@example.com",
				"테스트 사용자"
		));
		assertThat(result.getAuthorities())
				.extracting("authority")
				.containsExactly("ROLE_CUSTOMER", "ROLE_PARTNER_ADMIN");
		assertThat(result.getCredentials()).isNull();
		assertThat(request.getCredentials()).isNull();
	}

	@Test
	void returnsSameFailureForMissingMemberAndWrongPassword() {
		DatabaseMemberAuthenticationProvider missingMemberProvider = providerReturning(Optional.empty());
		DatabaseMemberAuthenticationProvider wrongPasswordProvider =
				providerReturning(Optional.of(member(MemberStatus.ACTIVE, PartnerStatus.ACTIVE)));

		assertAuthenticationFailed(missingMemberProvider, "correct-password");
		assertAuthenticationFailed(wrongPasswordProvider, "wrong-password");
	}

	@Test
	void rejectsInactiveMemberOrPartner() {
		DatabaseMemberAuthenticationProvider lockedMemberProvider =
				providerReturning(Optional.of(member(MemberStatus.LOCKED, PartnerStatus.ACTIVE)));
		DatabaseMemberAuthenticationProvider suspendedPartnerProvider =
				providerReturning(Optional.of(member(MemberStatus.ACTIVE, PartnerStatus.SUSPENDED)));

		assertAuthenticationFailed(lockedMemberProvider, "correct-password");
		assertAuthenticationFailed(suspendedPartnerProvider, "correct-password");
	}

	@Test
	void supportsOnlyPartnerEmailPasswordAuthentication() {
		DatabaseMemberAuthenticationProvider provider = providerReturning(Optional.empty());

		assertThat(provider.supports(PartnerEmailPasswordAuthenticationToken.class)).isTrue();
		assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
	}

	private DatabaseMemberAuthenticationProvider providerReturning(Optional<MemberCredentials> member) {
		MemberCredentialsRepository repository = (partnerKey, email) -> {
			assertThat(partnerKey).isEqualTo("woori-card");
			assertThat(email).isEqualTo("user@example.com");
			return member;
		};
		return new DatabaseMemberAuthenticationProvider(repository, passwordEncoder);
	}

	private MemberCredentials member(MemberStatus memberStatus, PartnerStatus partnerStatus) {
		return new MemberCredentials(
				10L,
				20L,
				"woori-card",
				"user@example.com",
				passwordEncoder.encode("correct-password"),
				"테스트 사용자",
				memberStatus,
				partnerStatus,
				Set.of(MemberRole.PARTNER_ADMIN, MemberRole.CUSTOMER)
		);
	}

	private void assertAuthenticationFailed(
			DatabaseMemberAuthenticationProvider provider,
			String rawPassword
	) {
		PartnerEmailPasswordAuthenticationToken request =
				PartnerEmailPasswordAuthenticationToken.unauthenticated(
						"woori-card",
						"user@example.com",
						rawPassword
				);

		assertThatThrownBy(() -> provider.authenticate(request))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("Authentication failed");
		assertThat(request.getCredentials()).isNull();
	}
}
