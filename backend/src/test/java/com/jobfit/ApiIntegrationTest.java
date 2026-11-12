package com.jobfit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 포트로 서버를 띄워 HTTP 로 주요 흐름을 확인한다 (H2 + 시드 데이터). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {
    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        assertThat(m.find()).as("응답에 %s 필드가 있어야 한다: %s", name, json).isTrue();
        return m.group(1);
    }

    @Test
    void 유명기업_공고만_조회된다() throws Exception {
        HttpResponse<String> res = send("GET", "/api/postings?famousOnly=true&size=5", null, null);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("\"famous\":true").doesNotContain("\"tier\":\"U\"");
    }

    @Test
    void 기술_필터와_통계가_동작한다() throws Exception {
        HttpResponse<String> list = send("GET", "/api/postings?skill=Redis&size=3", null, null);
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("Redis");

        HttpResponse<String> stats = send("GET", "/api/stats/skills?famousOnly=true", null, null);
        assertThat(stats.statusCode()).isEqualTo(200);
        assertThat(stats.body()).contains("\"name\"").contains("\"count\"");
    }

    @Test
    void 없는_공고는_404_잘못된_파라미터는_400() throws Exception {
        assertThat(send("GET", "/api/postings/99999999", null, null).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/api/postings?source=NOPE", null, null).statusCode()).isEqualTo(400);
    }

    @Test
    void 가입_로그인_기술등록_매칭_북마크_흐름() throws Exception {
        String email = "tester" + System.nanoTime() + "@example.com";
        String signup = "{\"email\":\"" + email + "\",\"password\":\"test-pass-1234\",\"nickname\":\"테스터\"}";
        assertThat(send("POST", "/api/auth/signup", signup, null).statusCode()).isEqualTo(201);
        assertThat(send("POST", "/api/auth/signup", signup, null).statusCode()).as("중복 가입").isEqualTo(409);

        String wrong = "{\"email\":\"" + email + "\",\"password\":\"wrong-pass-0000\"}";
        assertThat(send("POST", "/api/auth/login", wrong, null).statusCode()).isEqualTo(401);

        HttpResponse<String> login = send("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"test-pass-1234\"}", null);
        assertThat(login.statusCode()).isEqualTo(200);
        String token = field(login.body(), "token");

        assertThat(send("GET", "/api/me/skills", null, null).statusCode()).as("토큰 없음").isEqualTo(401);
        assertThat(send("GET", "/api/me/skills", null, "not-a-token").statusCode()).as("위조 토큰").isEqualTo(401);

        HttpResponse<String> put = send("PUT", "/api/me/skills", "{\"skills\":[\"Java\",\"Spring Boot\",\"MySQL\",\"Redis\"]}", token);
        assertThat(put.statusCode()).isEqualTo(200);
        assertThat(send("PUT", "/api/me/skills", "{\"skills\":[\"없는기술\"]}", token).statusCode()).isEqualTo(400);

        HttpResponse<String> matches = send("GET", "/api/me/matches?famousOnly=false", null, token);
        assertThat(matches.statusCode()).isEqualTo(200);
        assertThat(matches.body()).contains("\"score\"").contains("\"matched\"");

        Matcher id = Pattern.compile("\"id\":(\\d+)").matcher(matches.body());
        assertThat(id.find()).isTrue();
        String postingId = id.group(1);
        assertThat(send("PUT", "/api/me/bookmarks/" + postingId, null, token).statusCode()).isEqualTo(204);
        assertThat(send("PUT", "/api/me/bookmarks/" + postingId, null, token).statusCode()).as("두 번 담아도 성공").isEqualTo(204);
        assertThat(send("GET", "/api/me/bookmarks", null, token).body()).contains("\"id\":" + postingId);
        assertThat(send("DELETE", "/api/me/bookmarks/" + postingId, null, token).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/me/bookmarks", null, token).body()).isEqualTo("[]");
    }

    @Test
    void AI_서비스가_꺼져_있어도_대체_응답을_준다() throws Exception {
        HttpResponse<String> res = send("POST", "/api/ai/ask", "{\"question\":\"백엔드 신입 공고 알려줘\",\"famousOnly\":false}", null);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("\"fallback\":true");
    }
}
