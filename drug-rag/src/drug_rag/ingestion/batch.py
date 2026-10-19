from __future__ import annotations

import json
from dataclasses import asdict, dataclass, field
from pathlib import Path

from drug_rag.ingestion.generic_parser import parse_pdf_document, parse_text_document
from drug_rag.ingestion.jsonl import write_knowledge_jsonl

SUPPORTED_DOCUMENT_EXTENSIONS = {".pdf", ".txt", ".md", ".markdown", ".docx"}


@dataclass(slots=True)
class GenericDocumentReport:
    path: str
    domain: str
    doc_id: str = ""
    chunk_count: int = 0
    status: str = "ok"
    error: str = ""


@dataclass(slots=True)
class GenericBatchReport:
    doc_count: int = 0
    chunk_count: int = 0
    failed_count: int = 0
    out_jsonl: str = ""
    documents: list[GenericDocumentReport] = field(default_factory=list)

    def to_dict(self) -> dict[str, object]:
        return {
            "doc_count": self.doc_count,
            "chunk_count": self.chunk_count,
            "failed_count": self.failed_count,
            "out_jsonl": self.out_jsonl,
            "documents": [asdict(item) for item in self.documents],
        }


def parse_document_batch(
    input_dir: str | Path,
    out_jsonl: str | Path,
    domain: str,
    version: str = "1.0",
    metadata: dict[str, object] | None = None,
    recursive: bool = True,
    fail_fast: bool = False,
) -> GenericBatchReport:
    input_path = Path(input_dir)
    if not input_path.exists():
        raise FileNotFoundError(input_path)
    pattern = "**/*" if recursive else "*"
    documents = sorted(
        path for path in input_path.glob(pattern) if path.is_file() and path.suffix.lower() in SUPPORTED_DOCUMENT_EXTENSIONS
    )
    report = GenericBatchReport(out_jsonl=str(Path(out_jsonl)))
    all_chunks = []
    for path in documents:
        item = GenericDocumentReport(path=str(path), domain=domain)
        try:
            if path.suffix.lower() == ".pdf":
                document, chunks = parse_pdf_document(path, domain, version, metadata)
            else:
                document, chunks = parse_text_document(path, domain, version, metadata)
            item.doc_id = document.doc_id
            item.chunk_count = len(chunks)
            if chunks:
                report.doc_count += 1
                report.chunk_count += len(chunks)
                all_chunks.extend(chunks)
            else:
                item.status = "empty"
        except Exception as exc:
            item.status = "failed"
            item.error = f"{type(exc).__name__}: {exc}"
            report.failed_count += 1
            if fail_fast:
                report.documents.append(item)
                raise
        report.documents.append(item)

    write_knowledge_jsonl(all_chunks, out_jsonl)
    return report


def write_generic_batch_report(report: GenericBatchReport, path: str | Path) -> None:
    output_path = Path(path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report.to_dict(), ensure_ascii=False, indent=2), encoding="utf-8")
