package com.chiwonpark9.cardrecommendation.auth.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public final class TestRsaKeys {

	private static final TestRsaKeys SHARED = generate();

	private final RSAPublicKey publicKey;
	private final RSAPrivateKey privateKey;

	private TestRsaKeys(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}

	public static TestRsaKeys shared() {
		return SHARED;
	}

	public static TestRsaKeys generate() {
		return generate(2048);
	}

	public static TestRsaKeys generate(int keySize) {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(keySize);
			KeyPair keyPair = generator.generateKeyPair();
			return new TestRsaKeys(
					(RSAPublicKey) keyPair.getPublic(),
					(RSAPrivateKey) keyPair.getPrivate()
			);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA is required for JWT tests", exception);
		}
	}

	public RSAPublicKey publicKey() {
		return publicKey;
	}

	public RSAPrivateKey privateKey() {
		return privateKey;
	}

	public String publicKeyBase64() {
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	public String privateKeyBase64() {
		return Base64.getEncoder().encodeToString(privateKey.getEncoded());
	}
}
