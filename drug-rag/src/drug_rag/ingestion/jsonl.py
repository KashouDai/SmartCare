from __future__ import annotations

import json
from pathlib import Path
from typing import Iterable, Iterator

from drug_rag.io_utils import ensure_parent
from drug_rag.schemas import KnowledgeChunk, KnowledgeSource


def write_knowledge_jsonl(chunks: Iterable[KnowledgeChunk], path: str | Path) -> int:
    output_path = ensure_parent(path)
    count = 0
    with output_path.open("w", encoding="utf-8") as output:
        for chunk in chunks:
            output.write(json.dumps(chunk.to_dict(), ensure_ascii=False) + "\n")
            count += 1
    return count


def read_knowledge_jsonl(path: str | Path) -> Iterator[KnowledgeChunk]:
    with Path(path).open("r", encoding="utf-8") as source:
        for line in source:
            if not line.strip():
                continue
            payload = json.loads(line)
            yield KnowledgeChunk(
                chunk_id=payload["chunk_id"],
                doc_id=payload["doc_id"],
                domain=payload["domain"],
                title=payload["title"],
                text=payload["text"],
                section=payload["section"],
                source=KnowledgeSource(**payload["source"]),
                version=payload.get("version", "1.0"),
                chunk_index=payload.get("chunk_index", 0),
                metadata=payload.get("metadata", {}),
            )
