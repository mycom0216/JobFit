"""JobFit AI 서비스: 공고 RAG (BM25 검색 + 로컬 LLM 답변).

검색은 BM25 로 한다. 한글은 형태소 분석기 없이도 동작하도록 2글자 단위(bigram)와 영문 단어를 토큰으로 쓴다.
답변 생성은 Ollama 의 로컬 모델을 쓰며, 모델이 응답하지 않으면 검색된 공고 목록만으로 답한다.
"""
import json
import math
import os
import re
from collections import Counter
from pathlib import Path

import httpx
from fastapi import FastAPI
from pydantic import BaseModel, Field

BACKEND = os.getenv("JOBFIT_BACKEND", "http://localhost:8080")
OLLAMA = os.getenv("OLLAMA_URL", "http://localhost:11434")
MODEL = os.getenv("JOBFIT_LLM", "kanana2:3b")
SEED = Path(__file__).resolve().parent.parent / "backend/src/main/resources/seed/postings.json"
TOP_K = 5
USE_LLM = os.getenv("JOBFIT_USE_LLM") == "1"

app = FastAPI(title="JobFit AI")


def tokenize(text: str) -> list[str]:
    text = text.lower()
    tokens = re.findall(r"[a-z0-9#+.]{2,}", text)
    for chunk in re.findall(r"[가-힣]+", text):
        if len(chunk) == 1:
            tokens.append(chunk)
        tokens += [chunk[i:i + 2] for i in range(len(chunk) - 1)]
    return tokens


class Bm25:
    def __init__(self, docs: list[str], k1: float = 1.5, b: float = 0.75):
        self.k1, self.b = k1, b
        self.tf = [Counter(tokenize(d)) for d in docs]
        self.len = [sum(t.values()) for t in self.tf]
        self.avg = sum(self.len) / max(len(docs), 1)
        df = Counter(t for tf in self.tf for t in tf)
        n = len(docs)
        self.idf = {t: math.log(1 + (n - c + 0.5) / (c + 0.5)) for t, c in df.items()}

    def scores(self, query: str) -> list[float]:
        q = set(tokenize(query))
        out = []
        for tf, ln in zip(self.tf, self.len):
            s = 0.0
            for t in q:
                f = tf.get(t)
                if f:
                    s += self.idf[t] * f * (self.k1 + 1) / (f + self.k1 * (1 - self.b + self.b * ln / self.avg))
            out.append(s)
        return out


POSTINGS: list[dict] = []
INDEX: Bm25 | None = None


def load_postings() -> list[dict]:
    """백엔드 내보내기 API 를 우선 쓰고, 백엔드가 꺼져 있으면 시드 파일을 읽는다."""
    try:
        r = httpx.get(f"{BACKEND}/api/postings/export", timeout=20)
        r.raise_for_status()
        return [
            {"company": d["summary"]["company"], "title": d["summary"]["title"], "url": d["summary"]["url"],
             "famous": d["summary"]["famous"], "tier": d["summary"]["tier"], "skills": d["summary"]["skills"],
             "conditions": d["summary"]["conditions"] or "", "note": d["summary"]["note"] or "",
             "detail": d.get("detail") or ""}
            for d in r.json()
        ]
    except Exception:
        raw = json.loads(SEED.read_text(encoding="utf-8"))
        return [
            {"company": d["company"], "title": d["title"], "url": d["url"], "famous": d["tier"] != "U",
             "tier": d["tier"], "skills": [], "conditions": d["conditions"], "note": d["note"], "detail": d["detail"]}
            for d in raw
        ]


def doc_text(p: dict) -> str:
    # 제목과 기업명은 중요하므로 두 번 넣어 가중치를 준다
    head = f"{p['company']} {p['title']}"
    return f"{head} {head} {' '.join(p['skills'])} {p['conditions']} {p['note']} {p['detail'][:1500]}"


@app.on_event("startup")
def build_index():
    global POSTINGS, INDEX
    POSTINGS = load_postings()
    INDEX = Bm25([doc_text(p) for p in POSTINGS])


class AskRequest(BaseModel):
    question: str = Field(min_length=1, max_length=500)
    famous_only: bool = False


def retrieve(question: str, famous_only: bool) -> list[dict]:
    scores = INDEX.scores(question)
    ranked = sorted(range(len(POSTINGS)), key=lambda i: scores[i], reverse=True)
    hits = [POSTINGS[i] for i in ranked if scores[i] > 0 and (not famous_only or POSTINGS[i]["famous"])]
    return hits[:TOP_K]


def generate(question: str, hits: list[dict]) -> str | None:
    context = "\n\n".join(
        f"[{i + 1}] {h['company']} / {h['title']}\n조건: {h['conditions']}\n기술: {', '.join(h['skills']) or '미확인'}\n"
        f"비고: {h['note']}\n본문: {h['detail'][:600]}"
        for i, h in enumerate(hits)
    )
    prompt = (
        "당신은 신입 개발자 채용공고 안내자입니다. 아래 [공고]에 적힌 내용만 근거로 한국어로 간결하게 답하세요. "
        "공고에 없는 내용은 '공고에서 확인되지 않음'이라고 말하고, 답에 근거 번호([1] 등)를 붙이세요.\n\n"
        f"[공고]\n{context}\n\n[질문]\n{question}"
    )
    try:
        r = httpx.post(f"{OLLAMA}/api/chat", timeout=110, json={
            "model": MODEL, "stream": False, "options": {"temperature": 0.2, "num_predict": 400},
            "messages": [{"role": "user", "content": prompt}]})
        r.raise_for_status()
        text = r.json()["message"]["content"].strip()
        return text or None
    except Exception:
        return None


def extractive_answer(hits: list[dict]) -> str:
    """검색된 공고의 실제 필드만으로 답을 만든다 (지어내는 내용이 없다)."""
    lines = [f"질문과 관련도가 높은 공고 {len(hits)}건입니다."]
    for i, h in enumerate(hits, 1):
        skills = ", ".join(h["skills"]) if h["skills"] else "본문에서 기술 확인 안 됨"
        flag = f" ※{h['note']}" if h["note"] else ""
        lines.append(f"[{i}] {h['company']} · {h['title']}\n    조건: {h['conditions'] or '-'} / 요구 기술: {skills}{flag}")
    lines.append("신입 지원 가능 여부와 세부 자격은 원문에서 반드시 확인하세요.")
    return "\n".join(lines)


@app.get("/health")
def health():
    return {"status": "ok", "postings": len(POSTINGS), "model": MODEL}


@app.post("/ask")
def ask(req: AskRequest):
    hits = retrieve(req.question, req.famous_only)
    sources = [{"company": h["company"], "title": h["title"], "url": h["url"]} for h in hits]
    if not hits:
        return {"answer": "질문과 관련된 공고를 찾지 못했습니다. 기술명이나 기업명을 넣어 다시 물어보세요.",
                "sources": [], "fallback": False}
    # 로컬 3B 모델은 컨텍스트를 되풀이하는 일이 잦아 기본은 검색 결과 기반의 정형 답변을 쓴다.
    # JOBFIT_USE_LLM=1 이면 모델이 만든 문장을 앞에 덧붙인다.
    text = extractive_answer(hits)
    if USE_LLM:
        generated = generate(req.question, hits)
        if generated:
            text = generated + "\n\n" + text
    return {"answer": text, "sources": sources, "fallback": False}
