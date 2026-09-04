package com.chiwonpark9.cardrecommendation;

import com.chiwonpark9.cardrecommendation.auth.security.MemberPrincipal;
import com.chiwonpark9.cardrecommendation.auth.security.PartnerEmailPasswordAuthenticationToken;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class CardRecommendationApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
			.withDatabaseName("mydata_card_test")
			.withUsername("test_user")
			.withPassword("test_password");

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoadsAndAppliesFlywayMigration() {
		String migrationMarker = jdbcTemplate.queryForObject(
				"SELECT metadata_value FROM service_metadata WHERE metadata_key = ?",
				String.class,
				"schema_initialized_by"
		);

		assertThat(migrationMarker).isEqualTo("flyway");
		assertThat(tableExists("partners")).isTrue();
		assertThat(tableExists("members")).isTrue();
		assertThat(tableExists("member_roles")).isTrue();
	}

	@Test
	void authenticatesMemberFromDatabaseWithinPartnerBoundary() {
		long partnerId = insertPartner("woori-card", "우리카드", "ACTIVE");
		long memberId = insertMember(
				partnerId,
				"user@example.com",
				"correct-password",
				"테스트 사용자",
				"ACTIVE"
		);
		insertRole(memberId, "CUSTOMER");
		insertRole(memberId, "PARTNER_ADMIN");
		PartnerEmailPasswordAuthenticationToken request =
				PartnerEmailPasswordAuthenticationToken.unauthenticated(
						"WOORI-CARD",
						"USER@EXAMPLE.COM",
						"correct-password"
				);

		Authentication result = authenticationManager.authenticate(request);

		assertThat(result.isAuthenticated()).isTrue();
		assertThat(result.getPrincipal()).isEqualTo(new MemberPrincipal(
				memberId,
				partnerId,
				"woori-card",
				"user@example.com",
				"테스트 사용자"
		));
		assertThat(result.getAuthorities())
				.extracting("authority")
				.containsExactly("ROLE_CUSTOMER", "ROLE_PARTNER_ADMIN");
		assertThat(request.getCredentials()).isNull();
	}

	@Test
	void rejectsSameEmailWhenItBelongsToAnotherPartner() {
		long firstPartnerId = insertPartner("first-card", "첫 번째 카드사", "ACTIVE");
		insertPartner("second-card", "두 번째 카드사", "ACTIVE");
		long memberId = insertMember(
				firstPartnerId,
				"same@example.com",
				"correct-password",
				"첫 번째 회원",
				"ACTIVE"
		);
		insertRole(memberId, "CUSTOMER");

		PartnerEmailPasswordAuthenticationToken request =
				PartnerEmailPasswordAuthenticationToken.unauthenticated(
						"second-card",
						"same@example.com",
						"correct-password"
				);

		assertThatThrownBy(() -> authenticationManager.authenticate(request))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("Authentication failed");
	}

	@Test
	void rejectsLockedMemberAndSuspendedPartner() {
		long activePartnerId = insertPartner("active-card", "활성 카드사", "ACTIVE");
		long lockedMemberId = insertMember(
				activePartnerId,
				"locked@example.com",
				"correct-password",
				"잠긴 회원",
				"LOCKED"
		);
		insertRole(lockedMemberId, "CUSTOMER");

		long suspendedPartnerId = insertPartner("suspended-card", "중지 카드사", "SUSPENDED");
		long activeMemberId = insertMember(
				suspendedPartnerId,
				"active@example.com",
				"correct-password",
				"활성 회원",
				"ACTIVE"
		);
		insertRole(activeMemberId, "CUSTOMER");

		assertAuthenticationFails("active-card", "locked@example.com", "correct-password");
		assertAuthenticationFails("suspended-card", "active@example.com", "correct-password");
	}

	@Test
	void persistsSaltedBcryptHashInsteadOfRawPassword() {
		long partnerId = insertPartner("hash-card", "해시 카드사", "ACTIVE");
		long firstMemberId = insertMember(
				partnerId,
				"first@example.com",
				"same-password",
				"첫 번째 회원",
				"ACTIVE"
		);
		long secondMemberId = insertMember(
				partnerId,
				"second@example.com",
				"same-password",
				"두 번째 회원",
				"ACTIVE"
		);

		String firstHash = passwordHash(firstMemberId);
		String secondHash = passwordHash(secondMemberId);

		assertThat(firstHash).startsWith("{bcrypt}$2");
		assertThat(firstHash).isNotEqualTo("same-password");
		assertThat(secondHash).isNotEqualTo(firstHash);
		assertThat(passwordEncoder.matches("same-password", firstHash)).isTrue();
		assertThat(passwordEncoder.matches("same-password", secondHash)).isTrue();
	}

	@Test
	void preventsCaseInsensitiveDuplicateEmailWithinSamePartner() {
		long partnerId = insertPartner("duplicate-card", "중복 검사 카드사", "ACTIVE");
		insertMember(
				partnerId,
				"duplicate@example.com",
				"correct-password",
				"첫 번째 회원",
				"ACTIVE"
		);

		assertThatThrownBy(() -> insertMember(
				partnerId,
				"DUPLICATE@EXAMPLE.COM",
				"another-password",
				"중복 회원",
				"ACTIVE"
		)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private boolean tableExists(String tableName) {
		Integer count = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name = ?
				""",
				Integer.class,
				tableName
		);
		return count != null && count == 1;
	}

	private long insertPartner(String partnerKey, String name, String status) {
		jdbcTemplate.update(
				"INSERT INTO partners (partner_key, name, status) VALUES (?, ?, ?)",
				partnerKey,
				name,
				status
		);
		return requiredId("SELECT id FROM partners WHERE partner_key = ?", partnerKey);
	}

	private long insertMember(
			long partnerId,
			String email,
			String rawPassword,
			String displayName,
			String status
	) {
		String passwordHash = passwordEncoder.encode(rawPassword);
		jdbcTemplate.update(
				"""
				INSERT INTO members (partner_id, email, password_hash, display_name, status)
				VALUES (?, ?, ?, ?, ?)
				""",
				partnerId,
				email,
				passwordHash,
				displayName,
				status
		);
		return requiredId(
				"SELECT id FROM members WHERE partner_id = ? AND email = ?",
				partnerId,
				email
		);
	}

	private void insertRole(long memberId, String roleCode) {
		jdbcTemplate.update(
				"INSERT INTO member_roles (member_id, role_code) VALUES (?, ?)",
				memberId,
				roleCode
		);
	}

	private long requiredId(String sql, Object... arguments) {
		Long id = jdbcTemplate.queryForObject(sql, Long.class, arguments);
		if (id == null) {
			throw new IllegalStateException("Expected generated database id");
		}
		return id;
	}

	private String passwordHash(long memberId) {
		String passwordHash = jdbcTemplate.queryForObject(
				"SELECT password_hash FROM members WHERE id = ?",
				String.class,
				memberId
		);
		if (passwordHash == null) {
			throw new IllegalStateException("Expected password hash");
		}
		return passwordHash;
	}

	private void assertAuthenticationFails(String partnerKey, String email, String password) {
		PartnerEmailPasswordAuthenticationToken request =
				PartnerEmailPasswordAuthenticationToken.unauthenticated(
						partnerKey,
						email,
						password
				);
		assertThatThrownBy(() -> authenticationManager.authenticate(request))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("Authentication failed");
	}

}
