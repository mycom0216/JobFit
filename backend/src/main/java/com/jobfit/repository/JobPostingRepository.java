package com.jobfit.repository;

import com.jobfit.domain.JobPosting;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {

    @EntityGraph(attributePaths = {"company", "skills"})
    @Query("select p from JobPosting p where p.id = :id")
    Optional<JobPosting> findDetailById(@Param("id") Long id);

    /** 목록 조회 후 회사·기술을 한 번에 가져와 N+1 을 피한다. */
    @EntityGraph(attributePaths = {"company", "skills"})
    @Query("select distinct p from JobPosting p where p.id in :ids")
    List<JobPosting> findAllWithSkillsByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"company", "skills"})
    @Query("select distinct p from JobPosting p")
    List<JobPosting> findAllWithSkills();

    /** 기술별 공고 수 집계. famousOnly 가 true 면 유명기업 공고(A/B/C)만 센다. */
    @Query("""
            select s.name as name, s.category as category, count(p) as cnt
            from JobPosting p join p.skills s
            where (:famousOnly = false or p.tier <> com.jobfit.domain.Tier.U)
            group by s.name, s.category
            order by count(p) desc, s.name asc
            """)
    List<SkillCount> countBySkill(@Param("famousOnly") boolean famousOnly);

    interface SkillCount {
        String getName();

        String getCategory();

        long getCnt();
    }
}
