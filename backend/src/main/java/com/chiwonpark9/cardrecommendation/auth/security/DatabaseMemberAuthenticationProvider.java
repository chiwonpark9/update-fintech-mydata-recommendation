package com.chiwonpark9.cardrecommendation.auth.security;

import java.util.List;
import java.util.Locale;

import com.chiwonpark9.cardrecommendation.auth.application.port.MemberCredentialsRepository;
import com.chiwonpark9.cardrecommendation.auth.domain.MemberCredentials;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMemberAuthenticationProvider implements AuthenticationProvider {

	private static final String AUTHENTICATION_FAILED = "Authentication failed";

	private final MemberCredentialsRepository memberCredentialsRepository;
	private final PasswordEncoder passwordEncoder;
	private final String dummyPasswordHash;

	public DatabaseMemberAuthenticationProvider(
			MemberCredentialsRepository memberCredentialsRepository,
			PasswordEncoder passwordEncoder
	) {
		this.memberCredentialsRepository = memberCredentialsRepository;
		this.passwordEncoder = passwordEncoder;
		this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing-equalization");
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		PartnerEmailPasswordAuthenticationToken request =
				(PartnerEmailPasswordAuthenticationToken) authentication;

		String partnerKey = normalize(stringValue(request.getPartnerKey()));
		String email = normalize(stringValue(request.getPrincipal()));
		String rawPassword = stringValue(request.getCredentials());

		try {
			MemberCredentials member = memberCredentialsRepository
					.findByPartnerKeyAndEmail(partnerKey, email)
					.orElse(null);

			String passwordHash = member == null ? dummyPasswordHash : member.passwordHash();
			boolean passwordMatches = passwordEncoder.matches(rawPassword, passwordHash);

			if (member == null || !passwordMatches || !member.canAuthenticate()) {
				throw new BadCredentialsException(AUTHENTICATION_FAILED);
			}

			MemberPrincipal principal = new MemberPrincipal(
					member.memberId(),
					member.partnerId(),
					member.partnerKey(),
					member.email(),
					member.displayName()
			);
			List<SimpleGrantedAuthority> authorities = member.roles().stream()
					.map(role -> new SimpleGrantedAuthority(role.authority()))
					.sorted((left, right) -> left.getAuthority().compareTo(right.getAuthority()))
					.toList();

			return PartnerEmailPasswordAuthenticationToken.authenticated(principal, authorities);
		} finally {
			request.eraseCredentials();
		}
	}

	@Override
	public boolean supports(Class<?> authenticationType) {
		return PartnerEmailPasswordAuthenticationToken.class.isAssignableFrom(authenticationType);
	}

	private String normalize(String value) {
		return value.strip().toLowerCase(Locale.ROOT);
	}

	private String stringValue(Object value) {
		return value instanceof String string ? string : "";
	}
}
