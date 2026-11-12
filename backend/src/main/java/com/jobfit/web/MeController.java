package com.jobfit.web;

import com.jobfit.service.MatchService;
import com.jobfit.service.MemberService;
import com.jobfit.web.Dtos.MatchResult;
import com.jobfit.web.Dtos.PostingSummary;
import com.jobfit.web.Dtos.SkillsRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 로그인 회원 전용 API. SecurityConfig 에서 /api/me/** 전체에 인증을 요구한다. */
@RestController
@RequestMapping("/api/me")
public class MeController {
    private final MemberService members;
    private final MatchService matches;

    public MeController(MemberService members, MatchService matches) {
        this.members = members;
        this.matches = matches;
    }

    @GetMapping("/skills")
    public List<String> mySkills(@AuthenticationPrincipal Long memberId) {
        return members.mySkills(memberId);
    }

    @PutMapping("/skills")
    public List<String> replaceSkills(@AuthenticationPrincipal Long memberId, @Valid @RequestBody SkillsRequest req) {
        return members.replaceSkills(memberId, req.skills());
    }

    @GetMapping("/matches")
    public List<MatchResult> matches(@AuthenticationPrincipal Long memberId,
                                     @RequestParam(defaultValue = "true") boolean famousOnly) {
        return matches.match(memberId, famousOnly);
    }

    @GetMapping("/bookmarks")
    public List<PostingSummary> bookmarks(@AuthenticationPrincipal Long memberId) {
        return members.myBookmarks(memberId);
    }

    @PutMapping("/bookmarks/{postingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addBookmark(@AuthenticationPrincipal Long memberId, @PathVariable Long postingId) {
        members.addBookmark(memberId, postingId);
    }

    @DeleteMapping("/bookmarks/{postingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(@AuthenticationPrincipal Long memberId, @PathVariable Long postingId) {
        members.removeBookmark(memberId, postingId);
    }
}
