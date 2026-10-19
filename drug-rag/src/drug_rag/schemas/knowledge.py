from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any

from drug_rag.schemas.document import DrugChunk, DrugDocument


@dataclass(slots=True)
class KnowledgeSource:
    source_file: str
    source_path: str = ""
    page_start: int | None = None
    page_end: int | None = None

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass(slots=True)
class KnowledgeDocument:
    doc_id: str
    domain: str
    title: str
    version: str = "1.0"
    source_type: str = "file"
    metadata: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_drug_document(cls, document: DrugDocument) -> KnowledgeDocument:
        return cls(
            doc_id=document.doc_id,
            domain="drug",
            title=document.source_file,
            metadata=document.to_metadata(),
        )


@dataclass(slots=True)
class KnowledgeChunk:
    chunk_id: str
    doc_id: str
    domain: str
    title: str
    text: str
    section: str
    source: KnowledgeSource
    version: str = "1.0"
    chunk_index: int = 0
    metadata: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        payload = asdict(self)
        payload["source"] = self.source.to_dict()
        return payload

    @classmethod
    def from_drug_chunk(cls, chunk: DrugChunk) -> KnowledgeChunk:
        return cls(
            chunk_id=chunk.chunk_id,
            doc_id=chunk.doc_id,
            domain="drug",
            title=chunk.source.source_file,
            text=chunk.text,
            section=chunk.section,
            source=KnowledgeSource(
                source_file=chunk.source.source_file,
                source_path=chunk.source.source_path,
                page_start=chunk.source.page_start,
                page_end=chunk.source.page_end,
            ),
            chunk_index=chunk.chunk_index,
            metadata={
                "drug_name": chunk.drug_name,
                "generic_name": chunk.generic_name,
                "category": chunk.category,
                **chunk.metadata,
            },
        )
