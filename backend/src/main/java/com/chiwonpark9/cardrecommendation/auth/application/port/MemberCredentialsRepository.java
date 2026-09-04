package com.chiwonpark9.cardrecommendation.auth.application.port;

import java.util.Optional;

import com.chiwonpark9.cardrecommendation.auth.domain.MemberCredentials;

public interface MemberCredentialsRepository {

	Optional<MemberCredentials> findByPartnerKeyAndEmail(String partnerKey, String email);
}
