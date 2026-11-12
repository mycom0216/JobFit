package com.jobfit.service;

import com.jobfit.domain.Company;
import com.jobfit.domain.JobPosting;
import com.jobfit.domain.Skill;
import com.jobfit.domain.Source;
import com.jobfit.domain.Tier;
import com.jobfit.repository.CompanyRepository;
import com.jobfit.repository.JobPostingRepository;
import com.jobfit.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 최초 기동 시 수집해 둔 공고(seed/postings.json)를 DB 에 적재한다.
 * 이미 공고가 있으면 아무것도 하지 않으므로 재기동해도 중복되지 않는다.
 */
@Component
public class SeedLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedLoader.class);

    private final CompanyRepository companies;
    private final JobPostingRepository postings;
    private final SkillRepository skills;
    private final ObjectMapper mapper;

    public SeedLoader(CompanyRepository companies, JobPostingRepository postings,
                      SkillRepository skills, ObjectMapper mapper) {
        this.companies = companies;
        this.postings = postings;
        this.skills = skills;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Map<String, Skill> skillByName = new HashMap<>();
        for (Skill s : skills.findAll()) {
            skillByName.put(s.getName(), s);
        }
        for (SkillDictionary.Entry e : SkillDictionary.entries()) {
            if (!skillByName.containsKey(e.name())) {
                skillByName.put(e.name(), skills.save(new Skill(e.name(), e.category())));
            }
        }
        if (postings.count() > 0) {
            return;
        }

        JsonNode root;
        try (InputStream in = new ClassPathResource("seed/postings.json").getInputStream()) {
            root = mapper.readTree(in);
        }
        Map<String, Company> companyByName = new HashMap<>();
        Set<String> seenUrls = new HashSet<>();
        List<JobPosting> batch = new ArrayList<>();
        for (JsonNode n : root) {
            String url = text(n, "url");
            if (!seenUrls.add(url)) {
                continue;
            }
            Tier tier = Tier.valueOf(text(n, "tier"));
            String name = text(n, "company");
            Company company = companyByName.computeIfAbsent(name,
                    k -> companies.save(new Company(k, tier != Tier.U, emptyToNull(text(n, "famousReason")))));
            JobPosting p = new JobPosting(company, cut(text(n, "title"), 300), Source.valueOf(text(n, "source")), url,
                    cut(text(n, "conditions"), 300), cut(text(n, "sector"), 300), cut(text(n, "deadline"), 40),
                    tier, emptyToNull(cut(text(n, "note"), 200)), emptyToNull(text(n, "detail")));
            String haystack = p.getTitle() + " " + p.getSector() + " " + (p.getDetail() == null ? "" : p.getDetail());
            for (String skillName : SkillDictionary.extract(haystack)) {
                p.getSkills().add(skillByName.get(skillName));
            }
            batch.add(p);
        }
        postings.saveAll(batch);
        log.info("시드 적재 완료: 공고 {}건, 기업 {}곳", batch.size(), companyByName.size());
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? "" : v.asString();
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
