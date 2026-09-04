package com.chiwonpark9.cardrecommendation.auth.api;

import com.chiwonpark9.cardrecommendation.auth.application.LoginCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "제휴사 키는 필수입니다.")
		@Size(max = 64, message = "제휴사 키는 64자 이하여야 합니다.")
		@Pattern(
				regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
				message = "제휴사 키 형식을 확인해주세요."
		)
		String partnerKey,
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식을 확인해주세요.")
		@Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
		String email,
		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, max = 128, message = "비밀번호는 8자 이상 128자 이하여야 합니다.")
		String password
) {

	LoginCommand toCommand() {
		return new LoginCommand(partnerKey, email, password);
	}

	@Override
	public String toString() {
		return "LoginRequest[partnerKey=" + partnerKey
				+ ", email=[PROTECTED]"
				+ ", password=[PROTECTED]]";
	}
}
