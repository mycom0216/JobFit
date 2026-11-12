package com.jobfit.service;

import com.jobfit.domain.JobPosting;
import com.jobfit.domain.Source;
import com.jobfit.domain.Tier;
import com.jobfit.repository.JobPostingRepository;
import com.jobfit.web.Dtos.PageResponse;
import com.jobfit.web.Dtos.PostingDetail;
import com.jobfit.web.Dtos.PostingSummary;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostingService {
    private static final int MAX_SIZE = 100;

    private final JobPostingRepository postings;

    public PostingService(JobPostingRepository postings) {
        this.postings = postings;
    }

    /** 검색 조건. 값이 비어 있는 항목은 조건에서 제외한다. */
    public record SearchCondition(String keyword, String skill, Source source, Tier tier, boolean famousOnly) {
    }

    public PageResponse<PostingSummary> search(SearchCondition cond, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE),
                Sort.by("tier").ascending().and(Sort.by("id").ascending()));
        Page<JobPosting> result = postings.findAll(toSpec(cond), pageable);

        // 페이지에 담긴 id 만 다시 조회해 회사·기술을 한 번에 가져온다 (컬렉션 페치 조인 + 페이징 경고 회피)
        List<Long> ids = result.getContent().stream().map(JobPosting::getId).toList();
        Map<Long, JobPosting> loaded = postings.findAllWithSkillsByIdIn(ids).stream()
                .collect(Collectors.toMap(JobPosting::getId, Function.identity()));
        List<PostingSummary> content = ids.stream().map(loaded::get).map(PostingSummary::from).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public PostingDetail get(Long id) {
        return postings.findDetailById(id).map(PostingDetail::from)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다: " + id));
    }

    /** AI 서비스가 색인을 만들 때 쓰는 전체 내보내기. */
    public List<PostingDetail> exportAll() {
        return postings.findAllWithSkills().stream().map(PostingDetail::from).toList();
    }

    static Specification<JobPosting> toSpec(SearchCondition c) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (c.keyword() != null && !c.keyword().isBlank()) {
                String like = "%" + c.keyword().trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.join("company", JoinType.INNER).get("name")), like)));
            }
            if (c.skill() != null && !c.skill().isBlank()) {
                ps.add(cb.equal(cb.lower(root.join("skills", JoinType.INNER).get("name")), c.skill().trim().toLowerCase()));
            }
            if (c.source() != null) {
                ps.add(cb.equal(root.get("source"), c.source()));
            }
            if (c.tier() != null) {
                ps.add(cb.equal(root.get("tier"), c.tier()));
            }
            if (c.famousOnly()) {
                ps.add(cb.notEqual(root.get("tier"), Tier.U));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}
