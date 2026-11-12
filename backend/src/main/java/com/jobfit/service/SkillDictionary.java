package com.jobfit.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 공고 본문에서 기술 스택을 뽑아내는 사전.
 * 단순 포함 검색은 "Java" 가 "JavaScript" 에 걸리는 식의 오탐이 나므로, 기술마다 경계 조건을 가진 정규식을 둔다.
 */
public final class SkillDictionary {

    public record Entry(String name, String category, Pattern pattern) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static void add(String name, String category, String regex) {
        ENTRIES.add(new Entry(name, category, Pattern.compile(regex, Pattern.CASE_INSENSITIVE)));
    }

    static {
        // 언어
        add("Java", "언어", "(?<![A-Za-z])java(?!\\s*script)(?![A-Za-z])|자바(?!스크립트)");
        add("Kotlin", "언어", "kotlin|코틀린");
        add("Python", "언어", "python|파이썬");
        add("JavaScript", "언어", "java\\s*script|자바스크립트|(?<![A-Za-z])JS(?![A-Za-z])");
        add("TypeScript", "언어", "type\\s*script|타입스크립트");
        add("C#", "언어", "C#|(?<![A-Za-z])\\.NET(?![A-Za-z])|닷넷|WPF");
        add("C/C++", "언어", "C\\+\\+|(?<![A-Za-z])C언어|MFC");
        add("Go", "언어", "golang|(?<![A-Za-z])Go(?=\\s*(언어|lang))");
        // 백엔드
        add("Spring Boot", "백엔드", "spring(\\s*boot|\\s*framework)?|스프링");
        add("JPA", "백엔드", "(?<![A-Za-z])JPA(?![A-Za-z])|hibernate|MyBatis");
        add("Node.js", "백엔드", "node\\.?js|express|nest\\.?js");
        add("FastAPI", "백엔드", "fastapi|django|flask");
        add("REST API", "백엔드", "REST\\s*(ful)?\\s*API|(?<![A-Za-z])REST(?![A-Za-z])|GraphQL");
        // 데이터
        add("MySQL", "데이터", "mysql|mariadb");
        add("PostgreSQL", "데이터", "postgres(ql)?");
        add("MSSQL", "데이터", "ms-?sql|sql\\s*server");
        add("Oracle", "데이터", "oracle|오라클");
        add("Redis", "데이터", "redis|valkey");
        add("Elasticsearch", "데이터", "elastic\\s*search|opensearch");
        add("Kafka", "데이터", "kafka");
        add("SQL", "데이터", "(?<![A-Za-z-])SQL(?![A-Za-z])|RDBMS|쿼리");
        // 프론트
        add("React", "프론트", "react|next\\.?js");
        add("Vue", "프론트", "(?<![A-Za-z])vue(\\.?js)?(?![A-Za-z])");
        add("Flutter", "프론트", "flutter");
        add("Android", "프론트", "android|안드로이드");
        add("iOS", "프론트", "(?<![A-Za-z])iOS(?![A-Za-z])|swift");
        // 인프라
        add("Docker", "인프라", "docker|도커|container|컨테이너");
        add("Kubernetes", "인프라", "kubernetes|쿠버네티스|(?<![A-Za-z0-9])k8s");
        add("AWS", "인프라", "(?<![A-Za-z])AWS(?![A-Za-z])|클라우드|cloud");
        add("Linux", "인프라", "linux|리눅스");
        add("CI/CD", "인프라", "CI/?CD|jenkins|github\\s*actions");
        add("Git", "인프라", "(?<![A-Za-z])git(hub)?(?![A-Za-z])|형상관리");
        // AI
        add("LLM", "AI", "(?<![A-Za-z])LLM(?![A-Za-z])|GPT|생성형|AI\\s*agent|에이전트");
        add("RAG", "AI", "(?<![A-Za-z])RAG(?![A-Za-z])|vector\\s*DB|벡터|리랭킹|임베딩");
        add("PyTorch", "AI", "pytorch|tensorflow|딥러닝|deep\\s*learning");
        add("머신러닝", "AI", "머신러닝|machine\\s*learning|(?<![A-Za-z])ML(?![A-Za-z])|MLOps");
        add("컴퓨터비전", "AI", "컴퓨터\\s*비전|computer\\s*vision|영상처리|opencv");
        // 품질
        add("테스트 자동화", "품질", "테스트\\s*자동화|selenium|playwright|junit|(?<![A-Za-z])QA(?![A-Za-z])|단위\\s*테스트|유닛\\s*테스트");
    }

    private SkillDictionary() {
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    /** 텍스트에 등장하는 기술 이름을 사전 순서대로 돌려준다. */
    public static Set<String> extract(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        for (Entry e : ENTRIES) {
            if (e.pattern().matcher(text).find()) {
                found.add(e.name());
            }
        }
        return found;
    }
}
