package com.jobfit.repository;

import com.jobfit.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByNameIn(Collection<String> names);
}
