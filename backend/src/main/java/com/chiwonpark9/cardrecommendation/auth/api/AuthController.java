package com.chiwonpark9.cardrecommendation.auth.api;

import com.chiwonpark9.cardrecommendation.auth.application.LoginResult;
import com.chiwonpark9.cardrecommendation.auth.application.LoginService;
import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final LoginService loginService;

	public AuthController(LoginService loginService) {
		this.loginService = loginService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginResult result = loginService.login(request.toCommand());
		return noStore(LoginResponse.from(result));
	}

	@GetMapping("/me")
	public ResponseEntity<CurrentMemberResponse> currentMember(
			@AuthenticationPrincipal Jwt jwt
	) {
		return noStore(CurrentMemberResponse.from(jwt));
	}

	private <T> ResponseEntity<T> noStore(T body) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.PRAGMA, "no-cache")
				.body(body);
	}
}
