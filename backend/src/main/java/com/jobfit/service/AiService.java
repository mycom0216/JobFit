package com.jobfit.service;

import com.jobfit.domain.AiQueryLog;
import com.jobfit.repository.AiQueryLogRepository;
import com.jobfit.service.PostingService.SearchCondition;
import com.jobfit.web.Dtos.AiAnswer;
import com.jobfit.web.Dtos.AiAskRequest;
import com.jobfit.web.Dtos.AiSource;
import com.jobfit.web.Dtos.PostingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 서비스(FastAPI, RAG) 호출 담당.
 * AI 서비스가 꺼져 있거나 느려도 화면이 멈추지 않도록, 실패 시 DB 키워드 검색 결과로 대신 응답한다.
 */
@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestClient client;
    private final PostingService postingService;
    private final AiQueryLogRepository logs;

    public AiService(@Value("${jobfit.ai.base-url}") String baseUrl, PostingService postingService,
                     AiQueryLogRepository logs) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.postingService = postingService;
        this.logs = logs;
    }

    public AiAnswer ask(Long memberId, AiAskRequest req) {
        long start = System.currentTimeMillis();
        AiAnswer answer;
        try {
            AiAnswer remote = client.post().uri("/ask")
                    .body(Map.of("question", req.question(), "famous_only", req.famousOnly()))
                    .retrieve().body(AiAnswer.class);
            if (remote == null || remote.answer() == null) {
                throw new IllegalStateException("AI 서비스 응답이 비어 있습니다");
            }
            answer = new AiAnswer(remote.answer(), remote.sources() == null ? List.of() : remote.sources(), false);
        } catch (Exception e) {
            log.warn("AI 서비스 호출 실패, 키워드 검색으로 대체: {}", e.toString());
            answer = fallback(req);
        }
        logs.save(new AiQueryLog(memberId, req.question(), answer.answer(), answer.fallback(),
                System.currentTimeMillis() - start));
        return answer;
    }

    private AiAnswer fallback(AiAskRequest req) {
        // 질문에서 가장 긴 단어를 키워드로 삼는다
        String keyword = "";
        for (String w : req.question().split("[\\s,.?!]+")) {
            if (w.length() > keyword.length()) {
                keyword = w;
            }
        }
        List<PostingSummary> hits = postingService
                .search(new SearchCondition(keyword, null, null, null, req.famousOnly()), 0, 5).content();
        List<AiSource> sources = hits.stream().map(p -> new AiSource(p.company(), p.title(), p.url())).toList();
        String text = hits.isEmpty()
                ? "AI 서비스에 연결할 수 없고, '" + keyword + "' 로 찾은 공고도 없습니다."
                : "AI 서비스에 연결할 수 없어 '" + keyword + "' 키워드 검색 결과로 대신합니다.";
        return new AiAnswer(text, sources, true);
    }
}
