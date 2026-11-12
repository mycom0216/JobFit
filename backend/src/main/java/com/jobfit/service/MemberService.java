package com.jobfit.service;

import com.jobfit.domain.Bookmark;
import com.jobfit.domain.JobPosting;
import com.jobfit.domain.Member;
import com.jobfit.domain.Skill;
import com.jobfit.repository.BookmarkRepository;
import com.jobfit.repository.JobPostingRepository;
import com.jobfit.repository.MemberRepository;
import com.jobfit.repository.SkillRepository;
import com.jobfit.web.Dtos.PostingSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 로그인 회원의 보유 기술과 관심 공고를 다룬다. */
@Service
@Transactional
public class MemberService {
    private final MemberRepository members;
    private final SkillRepository skills;
    private final JobPostingRepository postings;
    private final BookmarkRepository bookmarks;

    public MemberService(MemberRepository members, SkillRepository skills,
                         JobPostingRepository postings, BookmarkRepository bookmarks) {
        this.members = members;
        this.skills = skills;
        this.postings = postings;
        this.bookmarks = bookmarks;
    }

    @Transactional(readOnly = true)
    public List<String> mySkills(Long memberId) {
        return member(memberId).getSkills().stream().map(Skill::getName).sorted().toList();
    }

    public List<String> replaceSkills(Long memberId, List<String> names) {
        Member m = member(memberId);
        List<Skill> found = skills.findByNameIn(names);
        if (found.size() != names.stream().distinct().count()) {
            throw new IllegalArgumentException("사전에 없는 기술이 포함되어 있습니다");
        }
        m.getSkills().clear();
        m.getSkills().addAll(found);
        return found.stream().map(Skill::getName).sorted().toList();
    }

    /** 이미 담긴 공고면 그대로 둔다 (같은 요청을 두 번 보내도 결과가 같도록). */
    public void addBookmark(Long memberId, Long postingId) {
        if (bookmarks.findByMemberIdAndPostingId(memberId, postingId).isPresent()) {
            return;
        }
        JobPosting p = postings.findById(postingId)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다: " + postingId));
        bookmarks.save(new Bookmark(members.getReferenceById(memberId), p));
    }

    public void removeBookmark(Long memberId, Long postingId) {
        bookmarks.findByMemberIdAndPostingId(memberId, postingId).ifPresent(bookmarks::delete);
    }

    @Transactional(readOnly = true)
    public List<PostingSummary> myBookmarks(Long memberId) {
        List<Long> ids = bookmarks.findPostingIds(memberId);
        Map<Long, JobPosting> loaded = postings.findAllWithSkillsByIdIn(ids).stream()
                .collect(Collectors.toMap(JobPosting::getId, Function.identity()));
        return ids.stream().map(loaded::get).map(PostingSummary::from).toList();
    }

    private Member member(Long id) {
        return members.findWithSkillsById(id).orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다"));
    }
}
