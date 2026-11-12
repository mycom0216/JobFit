import main


def setup_module():
    main.POSTINGS = [
        {"company": "A사", "title": "Java Spring 백엔드 신입", "url": "u1", "famous": True, "tier": "A",
         "skills": ["Java", "Spring Boot"], "conditions": "서울", "note": "", "detail": "MySQL Redis 경험"},
        {"company": "B사", "title": "프론트엔드 React 개발자", "url": "u2", "famous": False, "tier": "U",
         "skills": ["React"], "conditions": "부산", "note": "", "detail": "TypeScript"},
        {"company": "C사", "title": "AI 개발자 RAG", "url": "u3", "famous": True, "tier": "A",
         "skills": ["RAG", "Python"], "conditions": "성남", "note": "", "detail": "LLM 벡터 검색"},
    ]
    main.INDEX = main.Bm25([main.doc_text(p) for p in main.POSTINGS])


def test_korean_and_english_tokens():
    t = main.tokenize("백엔드 Spring Boot")
    assert "백엔" in t and "엔드" in t and "spring" in t and "boot" in t


def test_retrieve_ranks_relevant_first():
    assert main.retrieve("스프링 백엔드 공고", False)[0]["url"] == "u1"
    assert main.retrieve("RAG 검색", False)[0]["url"] == "u3"


def test_famous_only_filters():
    urls = [h["url"] for h in main.retrieve("React 프론트엔드", True)]
    assert "u2" not in urls


def test_extractive_answer_uses_only_posting_fields():
    text = main.extractive_answer(main.retrieve("스프링 백엔드", False)[:1])
    assert "A사" in text and "Java, Spring Boot" in text and "서울" in text


def test_no_match_returns_empty():
    assert main.retrieve("zzzzqqqq", False) == []
