package com.jobfit.service;

import com.jobfit.domain.Member;
import com.jobfit.repository.MemberRepository;
import com.jobfit.security.JwtProvider;
import com.jobfit.web.Dtos.LoginRequest;
import com.jobfit.web.Dtos.SignupRequest;
import com.jobfit.web.Dtos.TokenResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final JwtProvider jwt;

    public AuthService(MemberRepository members, PasswordEncoder encoder, JwtProvider jwt) {
        this.members = members;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public TokenResponse signup(SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (members.existsByEmail(email)) {
            throw new DuplicateException("이미 가입된 이메일입니다");
        }
        Member m = members.save(new Member(email, encoder.encode(req.password()), req.nickname().trim()));
        return new TokenResponse(jwt.issue(m.getId()), m.getNickname());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        // 이메일 존재 여부를 알려주지 않도록 두 실패 경우의 메시지를 같게 한다
        Member m = members.findByEmail(req.email().trim().toLowerCase())
                .filter(found -> encoder.matches(req.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다"));
        return new TokenResponse(jwt.issue(m.getId()), m.getNickname());
    }

    public static class DuplicateException extends RuntimeException {
        public DuplicateException(String message) {
            super(message);
        }
    }
}
