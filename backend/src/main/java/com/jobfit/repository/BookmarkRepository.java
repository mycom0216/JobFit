package com.jobfit.repository;

import com.jobfit.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByMemberIdAndPostingId(Long memberId, Long postingId);

    @Query("select b.posting.id from Bookmark b where b.member.id = :memberId order by b.createdAt desc")
    List<Long> findPostingIds(@Param("memberId") Long memberId);
}
