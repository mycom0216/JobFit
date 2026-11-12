package com.jobfit.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillAndMatchTest {

    @Test
    void 자바와_자바스크립트를_구분한다() {
        assertThat(SkillDictionary.extract("JavaScript, React 기반 프론트엔드")).contains("JavaScript", "React").doesNotContain("Java");
        assertThat(SkillDictionary.extract("Java/Spring Boot 백엔드 개발")).contains("Java", "Spring Boot").doesNotContain("JavaScript");
    }

    @Test
    void 한글_표기와_별칭도_인식한다() {
        assertThat(SkillDictionary.extract("코틀린, 스프링, 쿠버네티스 경험자 우대"))
                .contains("Kotlin", "Spring Boot", "Kubernetes");
        assertThat(SkillDictionary.extract("RAG 파이프라인 구축 (Vector DB·리랭킹)")).contains("RAG");
    }

    @Test
    void 빈_본문은_기술이_없다() {
        assertThat(SkillDictionary.extract("")).isEmpty();
        assertThat(SkillDictionary.extract(null)).isEmpty();
    }

    @Test
    void 적합도는_요구기술_대비_보유비율이다() {
        assertThat(MatchService.score(Set.of("Java", "MySQL"), Set.of("Java", "MySQL", "Redis", "Docker"))).isEqualTo(50);
        assertThat(MatchService.score(Set.of("Java"), Set.of("Java"))).isEqualTo(100);
        assertThat(MatchService.score(Set.of("Python"), Set.of("Java"))).isZero();
        assertThat(MatchService.score(Set.of("Java"), Set.of())).isZero();
    }
}
