package com.jobfit.service;

import com.jobfit.domain.JobPosting;
import com.jobfit.domain.Member;
import com.jobfit.domain.Skill;
import com.jobfit.domain.Tier;
import com.jobfit.repository.JobPostingRepository;
import com.jobfit.repository.MemberRepository;
import com.jobfit.web.Dtos.MatchResult;
import com.jobfit.web.Dtos.PostingSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** 내 보유 기술과 공고 요구 기술을 비교해 적합도를 계산한다. */
@Service
@Transactional(readOnly = true)
public class MatchService {
    private static final int LIMIT = 20;

    private final MemberRepository members;
    private final JobPostingRepository postings;

    public MatchService(MemberRepository members, JobPostingRepository postings) {
        this.members = members;
        this.postings = postings;
    }

    public List<MatchResult> match(Long memberId, boolean famousOnly) {
        Member member = members.findWithSkillsById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다"));
        Set<String> mine = names(member.getSkills());
        if (mine.isEmpty()) {
            return List.of();
        }
        return postings.findAllWithSkills().stream()
                .filter(p -> !famousOnly || p.getTier() != Tier.U)
                .filter(p -> !p.getSkills().isEmpty())
                .map(p -> toResult(p, mine))
                .filter(r -> r.score() > 0)
                .sorted(Comparator.comparingInt(MatchResult::score).reversed()
                        .thenComparing(r -> -r.matched().size())
                        .thenComparing(r -> r.posting().tier()))
                .limit(LIMIT)
                .toList();
    }

    private static MatchResult toResult(JobPosting p, Set<String> mine) {
        Set<String> required = names(p.getSkills());
        Set<String> matched = new TreeSet<>(required);
        matched.retainAll(mine);
        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(mine);
        return new MatchResult(PostingSummary.from(p), score(mine, required), List.copyOf(matched), List.copyOf(missing));
    }

    /** 적합도(0~100) = 공고 요구 기술 중 내가 가진 기술의 비율. */
    static int score(Set<String> mine, Set<String> required) {
        if (required.isEmpty()) {
            return 0;
        }
        long hit = required.stream().filter(mine::contains).count();
        return (int) Math.round(hit * 100.0 / required.size());
    }

    private static Set<String> names(Set<Skill> skills) {
        return skills.stream().map(Skill::getName).collect(Collectors.toSet());
    }
}
