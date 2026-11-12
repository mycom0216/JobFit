package com.jobfit.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookmark",
        uniqueConstraints = @UniqueConstraint(name = "uk_bookmark_member_posting", columnNames = {"member_id", "posting_id"}))
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false)
    private JobPosting posting;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Bookmark() {
    }

    public Bookmark(Member member, JobPosting posting) {
        this.member = member;
        this.posting = posting;
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public JobPosting getPosting() { return posting; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
