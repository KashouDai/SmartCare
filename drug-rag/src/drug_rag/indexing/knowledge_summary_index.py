from __future__ import annotations

from collections import defaultdict
from pathlib import Path

from drug_rag.embeddings import build_embeddings
from drug_rag.indexing.vector_index import _chroma_class
from drug_rag.ingestion.jsonl import read_knowledge_jsonl


def build_knowledge_summary_index(
    jsonl_path: str | Path,
    persist_dir: str | Path = "data/chroma",
    collection: str = "knowledge_summaries",
    embedding_provider: str = "hash",
    embedding_model: str | None = None,
    dimensions: int | None = 512,
    embedding_base_url: str | None = None,
    embedding_api_key_env: str = "DASHSCOPE_API_KEY",
) -> int:
    """按通用文档和章节构建摘要 collection。"""
    grouped = defaultdict(list)
    for chunk in read_knowledge_jsonl(jsonl_path):
        grouped[(chunk.doc_id, chunk.section)].append(chunk)
    if not grouped:
        raise ValueError(f"No knowledge chunks found in {jsonl_path}")

    texts = []
    metadatas = []
    ids = []
    for (doc_id, section), chunks in grouped.items():
        first = chunks[0]
        summary = _extractive_summary("\n".join(chunk.text for chunk in chunks))
        texts.append(f"{first.title} {section}\n{summary}")
        metadatas.append(
            {
                "doc_id": doc_id,
                "domain": first.domain,
                "title": first.title,
                "version": first.version,
                "section": section,
                "source_file": first.source.source_file,
                "summary_type": "section",
            }
        )
        ids.append(f"{doc_id}:summary:{section}")

    embeddings = build_embeddings(
        provider=embedding_provider,
        model=embedding_model,
        dimensions=dimensions,
        base_url=embedding_base_url,
        api_key_env=embedding_api_key_env,
    )
    Chroma = _chroma_class()
    Path(persist_dir).mkdir(parents=True, exist_ok=True)
    vector_store = Chroma(
        collection_name=collection,
        persist_directory=str(persist_dir),
        embedding_function=embeddings,
    )
    vector_store.delete(ids=ids)
    vector_store.add_texts(texts=texts, metadatas=metadatas, ids=ids)
    if hasattr(vector_store, "persist"):
        vector_store.persist()
    return len(texts)


def _extractive_summary(text: str, max_chars: int = 420) -> str:
    text = " ".join(line.strip() for line in text.splitlines() if line.strip())
    if len(text) <= max_chars:
        return text
    stop = max(text.rfind("。", 0, max_chars), text.rfind("；", 0, max_chars))
    return text[: stop + 1] if stop > max_chars * 0.5 else text[:max_chars].rstrip() + "..."
