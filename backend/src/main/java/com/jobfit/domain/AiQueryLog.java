package com.jobfit.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** AI 질의 이력. 답변 품질 점검과 사용량 파악에 쓴다. */
@Entity
@Table(name = "ai_query_log", indexes = @Index(name = "idx_ailog_created", columnList = "created_at"))
public class AiQueryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private boolean fallback;

    @Column(name = "elapsed_ms", nullable = false)
    private long elapsedMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AiQueryLog() {
    }

    public AiQueryLog(Long memberId, String question, String answer, boolean fallback, long elapsedMs) {
        this.memberId = memberId;
        this.question = question;
        this.answer = answer;
        this.fallback = fallback;
        this.elapsedMs = elapsedMs;
    }

    public Long getId() { return id; }
}
