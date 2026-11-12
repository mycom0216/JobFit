package com.jobfit.repository;

import com.jobfit.domain.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "skills")
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findWithSkillsById(@Param("id") Long id);
}
