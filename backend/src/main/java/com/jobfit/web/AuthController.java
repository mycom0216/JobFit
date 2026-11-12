package com.jobfit.web;

import com.jobfit.service.AiService;
import com.jobfit.service.AuthService;
import com.jobfit.web.Dtos.AiAnswer;
import com.jobfit.web.Dtos.AiAskRequest;
import com.jobfit.web.Dtos.LoginRequest;
import com.jobfit.web.Dtos.SignupRequest;
import com.jobfit.web.Dtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 없이 쓸 수 있는 쓰기 API: 회원가입·로그인·AI 질문. */
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    private final AiService ai;

    public AuthController(AuthService auth, AiService ai) {
        this.auth = auth;
        this.ai = ai;
    }

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest req) {
        return auth.signup(req);
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    /** 비로그인도 질문할 수 있다. 로그인 상태면 이력에 회원 id 가 남는다. */
    @PostMapping("/ai/ask")
    public AiAnswer ask(@AuthenticationPrincipal Long memberId, @Valid @RequestBody AiAskRequest req) {
        return ai.ask(memberId, req);
    }
}
