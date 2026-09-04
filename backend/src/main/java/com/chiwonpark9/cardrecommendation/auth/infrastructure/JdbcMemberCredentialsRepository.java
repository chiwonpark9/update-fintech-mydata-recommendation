package com.chiwonpark9.cardrecommendation.auth.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.chiwonpark9.cardrecommendation.auth.application.port.MemberCredentialsRepository;
import com.chiwonpark9.cardrecommendation.auth.domain.MemberCredentials;
import com.chiwonpark9.cardrecommendation.auth.domain.MemberRole;
import com.chiwonpark9.cardrecommendation.auth.domain.MemberStatus;
import com.chiwonpark9.cardrecommendation.tenant.domain.PartnerStatus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcMemberCredentialsRepository implements MemberCredentialsRepository {

	private static final String FIND_MEMBER_CREDENTIALS = """
			SELECT m.id,
			       m.partner_id,
			       p.partner_key,
			       m.email,
			       m.password_hash,
			       m.display_name,
			       m.status AS member_status,
			       p.status AS partner_status,
			       mr.role_code
			FROM members m
			JOIN partners p ON p.id = m.partner_id
			LEFT JOIN member_roles mr ON mr.member_id = m.id
			WHERE p.partner_key = ?
			  AND m.email = ?
			ORDER BY mr.role_code
			""";

	private final JdbcTemplate jdbcTemplate;

	public JdbcMemberCredentialsRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MemberCredentials> findByPartnerKeyAndEmail(String partnerKey, String email) {
		List<MemberCredentialRow> rows = jdbcTemplate.query(
				FIND_MEMBER_CREDENTIALS,
				this::mapRow,
				partnerKey,
				email
		);

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		MemberCredentialRow member = rows.get(0);
		Set<MemberRole> roles = new LinkedHashSet<>();
		for (MemberCredentialRow row : rows) {
			if (row.roleCode() != null) {
				roles.add(MemberRole.valueOf(row.roleCode()));
			}
		}

		return Optional.of(new MemberCredentials(
				member.memberId(),
				member.partnerId(),
				member.partnerKey(),
				member.email(),
				member.passwordHash(),
				member.displayName(),
				MemberStatus.valueOf(member.memberStatus()),
				PartnerStatus.valueOf(member.partnerStatus()),
				roles
		));
	}

	private MemberCredentialRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return new MemberCredentialRow(
				resultSet.getLong("id"),
				resultSet.getLong("partner_id"),
				resultSet.getString("partner_key"),
				resultSet.getString("email"),
				resultSet.getString("password_hash"),
				resultSet.getString("display_name"),
				resultSet.getString("member_status"),
				resultSet.getString("partner_status"),
				resultSet.getString("role_code")
		);
	}

	private record MemberCredentialRow(
			long memberId,
			long partnerId,
			String partnerKey,
			String email,
			String passwordHash,
			String displayName,
			String memberStatus,
			String partnerStatus,
			String roleCode
	) {
	}
}
