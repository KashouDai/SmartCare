from __future__ import annotations

from pathlib import Path

from drug_rag.catalog import load_catalog
from drug_rag.embeddings import build_embeddings
from drug_rag.indexing.vector_index import _chroma_class
from drug_rag.ingestion.jsonl import read_knowledge_jsonl
from drug_rag.schemas import KnowledgeChunk


def build_knowledge_vector_index(
    jsonl_path: str | Path,
    persist_dir: str | Path = "data/chroma",
    collection: str = "knowledge_chunks",
    embedding_provider: str = "hash",
    embedding_model: str | None = None,
    dimensions: int | None = 512,
    embedding_base_url: str | None = None,
    embedding_api_key_env: str = "DASHSCOPE_API_KEY",
) -> int:
    """把通用 KnowledgeChunk JSONL 写入独立 Chroma collection。"""
    chunks = list(read_knowledge_jsonl(jsonl_path))
    if not chunks:
        raise ValueError(f"No knowledge chunks found in {jsonl_path}")

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
    ids = [chunk.chunk_id for chunk in chunks]
    vector_store.delete(ids=ids)
    vector_store.add_texts(
        texts=[chunk.text for chunk in chunks],
        metadatas=[_flat_metadata(chunk) for chunk in chunks],
        ids=ids,
    )
    if hasattr(vector_store, "persist"):
        vector_store.persist()
    return len(chunks)


def load_active_knowledge_stores(
    catalog_path: str | Path,
    domain: str,
    persist_dir: str | Path = "data/chroma",
    embedding_provider: str = "hash",
    embedding_model: str | None = None,
    dimensions: int | None = 512,
    embedding_base_url: str | None = None,
    embedding_api_key_env: str = "DASHSCOPE_API_KEY",
) -> dict[str, object]:
    """按 Catalog active 指针加载通用 chunk 和 summary collection。"""
    catalog = load_catalog(catalog_path)
    collection = catalog.active_collection(domain)
    if collection is None:
        raise ValueError(f"No active knowledge collection for domain: {domain}")

    embeddings = build_embeddings(
        provider=embedding_provider,
        model=embedding_model or collection.embedding_model,
        dimensions=dimensions,
        base_url=embedding_base_url,
        api_key_env=embedding_api_key_env,
    )
    Chroma = _chroma_class()
    stores = {
        "domain": domain,
        "version_id": collection.version_id,
        "chunks": Chroma(
            collection_name=collection.collection_name,
            persist_directory=str(persist_dir),
            embedding_function=embeddings,
        ),
    }
    if collection.summary_collection_name:
        stores["summaries"] = Chroma(
            collection_name=collection.summary_collection_name,
            persist_directory=str(persist_dir),
            embedding_function=embeddings,
        )
    return stores


def _flat_metadata(chunk: KnowledgeChunk) -> dict[str, object]:
    return {
        "chunk_id": chunk.chunk_id,
        "doc_id": chunk.doc_id,
        "domain": chunk.domain,
        "title": chunk.title,
        "version": chunk.version,
        "section": chunk.section,
        "source_file": chunk.source.source_file,
        "source_path": chunk.source.source_path,
        "page_start": chunk.source.page_start if chunk.source.page_start is not None else -1,
        "page_end": chunk.source.page_end if chunk.source.page_end is not None else -1,
        "chunk_index": chunk.chunk_index,
        **chunk.metadata,
    }
