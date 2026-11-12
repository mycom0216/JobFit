package com.jobfit.web;

import com.jobfit.domain.JobPosting;
import com.jobfit.domain.Skill;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

/** API 요청·응답 모델 모음. 엔티티를 그대로 노출하지 않기 위해 별도 레코드로 둔다. */
public final class Dtos {
    private Dtos() {
    }

    public record PostingSummary(Long id, String company, boolean famous, String famousReason, String title,
                                 String source, String url, String conditions, String deadline, String tier,
                                 String note, List<String> skills) {
        public static PostingSummary from(JobPosting p) {
            return new PostingSummary(p.getId(), p.getCompany().getName(), p.getCompany().isFamous(),
                    p.getCompany().getFamousReason(), p.getTitle(), p.getSource().name(), p.getUrl(),
                    p.getConditions(), p.getDeadline(), p.getTier().name(), p.getNote(),
                    p.getSkills().stream().map(Skill::getName).sorted().toList());
        }
    }

    public record PostingDetail(PostingSummary summary, String sector, String detail) {
        public static PostingDetail from(JobPosting p) {
            return new PostingDetail(PostingSummary.from(p), p.getSector(), p.getDetail());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    public record SkillStat(String name, String category, long count) implements Serializable {
    }

    public record MatchResult(PostingSummary posting, int score, List<String> matched, List<String> missing) {
    }

    public record SignupRequest(@NotBlank @Email @Size(max = 120) String email,
                                @NotBlank @Size(min = 8, max = 64) String password,
                                @NotBlank @Size(max = 30) String nickname) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record TokenResponse(String token, String nickname) {
    }

    public record SkillsRequest(@NotNull @Size(max = 50) List<String> skills) {
    }

    public record AiAskRequest(@NotBlank @Size(max = 500) String question, boolean famousOnly) {
    }

    public record AiSource(String company, String title, String url) {
    }

    public record AiAnswer(String answer, List<AiSource> sources, boolean fallback) {
    }

    public record ErrorResponse(String code, String message) {
    }
}
