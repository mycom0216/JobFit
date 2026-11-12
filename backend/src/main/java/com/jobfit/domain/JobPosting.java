package com.jobfit.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "job_posting",
        uniqueConstraints = @UniqueConstraint(name = "uk_posting_url", columnNames = "url"),
        indexes = {
                @Index(name = "idx_posting_tier", columnList = "tier"),
                @Index(name = "idx_posting_company", columnList = "company_id")
        })
public class JobPosting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Source source;

    @Column(nullable = false, length = 300)
    private String url;

    @Column(length = 300)
    private String conditions;

    @Column(length = 300)
    private String sector;

    @Column(length = 40)
    private String deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Tier tier;

    @Column(length = 200)
    private String note;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt = LocalDateTime.now();

    @ManyToMany
    @JoinTable(name = "posting_skill",
            joinColumns = @JoinColumn(name = "posting_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new LinkedHashSet<>();

    protected JobPosting() {
    }

    public JobPosting(Company company, String title, Source source, String url, String conditions,
                      String sector, String deadline, Tier tier, String note, String detail) {
        this.company = company;
        this.title = title;
        this.source = source;
        this.url = url;
        this.conditions = conditions;
        this.sector = sector;
        this.deadline = deadline;
        this.tier = tier;
        this.note = note;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public String getTitle() { return title; }
    public Source getSource() { return source; }
    public String getUrl() { return url; }
    public String getConditions() { return conditions; }
    public String getSector() { return sector; }
    public String getDeadline() { return deadline; }
    public Tier getTier() { return tier; }
    public String getNote() { return note; }
    public String getDetail() { return detail; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public Set<Skill> getSkills() { return skills; }
}
