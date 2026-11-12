package com.jobfit.service;

import com.jobfit.repository.JobPostingRepository;
import com.jobfit.web.Dtos.SkillStat;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {
    public static final String SKILL_STATS_CACHE = "skillStats";

    private final JobPostingRepository postings;

    public StatsService(JobPostingRepository postings) {
        this.postings = postings;
    }

    /**
     * 기술별 요구 공고 수. 전체 공고를 조인·집계하는 무거운 조회라서 캐시한다.
     * (local: 인메모리, prod: Redis TTL 10분)
     */
    @Cacheable(cacheNames = SKILL_STATS_CACHE, key = "#famousOnly")
    public List<SkillStat> skillStats(boolean famousOnly) {
        List<SkillStat> out = new ArrayList<>();
        postings.countBySkill(famousOnly)
                .forEach(c -> out.add(new SkillStat(c.getName(), c.getCategory(), c.getCnt())));
        return out;
    }
}
