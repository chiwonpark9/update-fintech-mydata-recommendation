package com.chiwonpark9.cardrecommendation.auth.config;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	private static final int MINIMUM_RSA_KEY_SIZE = 2048;

	@Bean
	Clock jwtClock() {
		return Clock.systemUTC();
	}

	@Bean
	JwtKeyPair jwtKeyPair(JwtProperties properties) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
					new X509EncodedKeySpec(decode(properties.publicKeyBase64()))
			);
			RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
					new PKCS8EncodedKeySpec(decode(properties.privateKeyBase64()))
			);

			if (publicKey.getModulus().bitLength() < MINIMUM_RSA_KEY_SIZE) {
				throw new IllegalStateException("JWT RSA key must be at least 2048 bits");
			}
			if (!publicKey.getModulus().equals(privateKey.getModulus())) {
				throw new IllegalStateException("JWT public and private keys do not form a pair");
			}

			return new JwtKeyPair(publicKey, privateKey);
		} catch (IllegalArgumentException | ClassCastException
				| NoSuchAlgorithmException | InvalidKeySpecException exception) {
			throw new IllegalStateException("JWT RSA key configuration is invalid", exception);
		}
	}

	@Bean
	JwtEncoder jwtEncoder(JwtKeyPair keyPair, JwtProperties properties) {
		return NimbusJwtEncoder.withKeyPair(keyPair.publicKey(), keyPair.privateKey())
				.algorithm(SignatureAlgorithm.RS256)
				.jwkPostProcessor(key -> key.keyID(properties.keyId()))
				.build();
	}

	@Bean
	JwtDecoder jwtDecoder(JwtKeyPair keyPair, JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keyPair.publicKey())
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.validateType(true)
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(properties.issuer()),
				new JwtAudienceValidator(properties.audience()),
				new JwtMemberClaimsValidator()
		));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}

	private byte[] decode(String encodedKey) {
		return Base64.getDecoder().decode(encodedKey);
	}

	record JwtKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
	}
}
