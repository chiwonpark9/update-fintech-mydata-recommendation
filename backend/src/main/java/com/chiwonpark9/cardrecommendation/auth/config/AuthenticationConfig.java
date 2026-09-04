package com.chiwonpark9.cardrecommendation.auth.config;

import java.util.List;

import com.chiwonpark9.cardrecommendation.auth.security.DatabaseMemberAuthenticationProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class AuthenticationConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(
			DatabaseMemberAuthenticationProvider authenticationProvider
	) {
		return new ProviderManager(List.of(authenticationProvider));
	}
}
