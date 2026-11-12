package com.jobfit.domain;

import jakarta.persistence.*;

/** 채용 기업. famous=true 는 대중 인지도가 높은 기업 선별 목록에 포함됨을 뜻한다. */
@Entity
@Table(name = "company", uniqueConstraints = @UniqueConstraint(name = "uk_company_name", columnNames = "name"))
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean famous;

    @Column(name = "famous_reason", length = 120)
    private String famousReason;

    protected Company() {
    }

    public Company(String name, boolean famous, String famousReason) {
        this.name = name;
        this.famous = famous;
        this.famousReason = famousReason;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isFamous() { return famous; }
    public String getFamousReason() { return famousReason; }
}
