package com.jobfit.domain;

import jakarta.persistence.*;

/** 기술 스택 사전 항목 (예: Java, Spring Boot, MySQL). */
@Entity
@Table(name = "skill", uniqueConstraints = @UniqueConstraint(name = "uk_skill_name", columnNames = "name"))
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 20)
    private String category;

    protected Skill() {
    }

    public Skill(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
}
