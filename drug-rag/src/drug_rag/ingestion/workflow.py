from __future__ import annotations

from pathlib import Path

from drug_rag.catalog import CollectionEntry, Catalog, publish_collection
from drug_rag.indexing.knowledge_vector_index import build_knowledge_vector_index
from drug_rag.indexing.knowledge_summary_index import build_knowledge_summary_index
from drug_rag.ingestion.batch import GenericBatchReport, parse_document_batch


def build_and_publish_knowledge_version(
    input_dir: str | Path,
    work_dir: str | Path,
    catalog_path: str | Path,
    domain: str,
    version_id: str,
    collection_name: str,
    summary_collection_name: str | None = None,
    version: str = "1.0",
    metadata: dict[str, object] | None = None,
    embedding_provider: str = "hash",
    embedding_model: str = "hash",
    dimensions: int | None = 512,
) -> tuple[GenericBatchReport, Catalog]:
    """构建一个通用知识域版本，索引成功后再发布到 Catalog。"""
    work_path = Path(work_dir)
    jsonl_path = work_path / f"{version_id}.jsonl"
    report = parse_document_batch(
        input_dir=input_dir,
        out_jsonl=jsonl_path,
        domain=domain,
        version=version,
        metadata=metadata,
    )
    if report.failed_count:
        raise ValueError(f"Cannot publish version with failed documents: {version_id}")
    if report.chunk_count == 0:
        raise ValueError(f"Cannot publish empty knowledge version: {version_id}")

    build_knowledge_vector_index(
        jsonl_path=jsonl_path,
        persist_dir=work_path / "chroma",
        collection=collection_name,
        embedding_provider=embedding_provider,
        embedding_model=embedding_model,
        dimensions=dimensions,
    )
    summary_collection_name = summary_collection_name or f"{collection_name}_summaries"
    build_knowledge_summary_index(
        jsonl_path=jsonl_path,
        persist_dir=work_path / "chroma",
        collection=summary_collection_name,
        embedding_provider=embedding_provider,
        embedding_model=embedding_model,
        dimensions=dimensions,
    )
    collection = CollectionEntry(
        version_id=version_id,
        domain=domain,
        collection_name=collection_name,
        summary_collection_name=summary_collection_name,
        embedding_model=embedding_model,
        status="published",
        document_count=report.doc_count,
        chunk_count=report.chunk_count,
    )
    catalog = publish_collection(catalog_path, collection)
    return report, catalog
