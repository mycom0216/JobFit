package com.jobfit.web;

import com.jobfit.domain.Source;
import com.jobfit.domain.Tier;
import com.jobfit.service.PostingService;
import com.jobfit.service.PostingService.SearchCondition;
import com.jobfit.service.StatsService;
import com.jobfit.web.Dtos.PageResponse;
import com.jobfit.web.Dtos.PostingDetail;
import com.jobfit.web.Dtos.PostingSummary;
import com.jobfit.web.Dtos.SkillStat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PostingController {
    private final PostingService postings;
    private final StatsService stats;

    public PostingController(PostingService postings, StatsService stats) {
        this.postings = postings;
        this.stats = stats;
    }

    @GetMapping("/postings")
    public PageResponse<PostingSummary> search(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String skill,
                                               @RequestParam(required = false) Source source,
                                               @RequestParam(required = false) Tier tier,
                                               @RequestParam(defaultValue = "false") boolean famousOnly,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return postings.search(new SearchCondition(keyword, skill, source, tier, famousOnly), page, size);
    }

    @GetMapping("/postings/{id}")
    public PostingDetail get(@PathVariable Long id) {
        return postings.get(id);
    }

    /** AI 서비스 색인용 전체 내보내기. */
    @GetMapping("/postings/export")
    public List<PostingDetail> export() {
        return postings.exportAll();
    }

    @GetMapping("/stats/skills")
    public List<SkillStat> skillStats(@RequestParam(defaultValue = "false") boolean famousOnly) {
        return stats.skillStats(famousOnly);
    }
}
