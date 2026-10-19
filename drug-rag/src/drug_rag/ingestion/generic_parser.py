from __future__ import annotations

import hashlib
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree

from drug_rag.loaders import load_pdf_pages
from drug_rag.schemas import KnowledgeChunk, KnowledgeDocument, KnowledgeSource

SUPPORTED_TEXT_EXTENSIONS = {".txt", ".md", ".markdown", ".docx"}
HEADING_PATTERN = re.compile(r"^#{1,6}\s+(.+?)\s*$")


def parse_text_document(
    path: str | Path,
    domain: str,
    version: str = "1.0",
    metadata: dict[str, object] | None = None,
    chunk_size: int = 900,
    chunk_overlap: int = 120,
) -> tuple[KnowledgeDocument, list[KnowledgeChunk]]:
    """解析 TXT/Markdown 文档，按标题和滑窗生成通用知识片段。"""
    source_path = Path(path)
    if source_path.suffix.lower() not in SUPPORTED_TEXT_EXTENSIONS:
        raise ValueError(f"Unsupported text document: {source_path.suffix}")
    if chunk_size <= 0 or chunk_overlap < 0 or chunk_overlap >= chunk_size:
        raise ValueError("chunk_overlap must be smaller than a positive chunk_size")

    text = _read_document_text(source_path)
    doc_id = hashlib.md5(source_path.read_bytes()).hexdigest()
    document = KnowledgeDocument(
        doc_id=doc_id,
        domain=domain,
        title=source_path.name,
        version=version,
        source_type=source_path.suffix.lower().lstrip("."),
        metadata=dict(metadata or {}),
    )

    sections = _extract_sections(text)
    chunks: list[KnowledgeChunk] = []
    for section_index, (section, body) in enumerate(sections):
        for chunk_index, chunk_text in enumerate(_split_text(body, chunk_size, chunk_overlap)):
            chunks.append(
                KnowledgeChunk(
                    chunk_id=f"{doc_id}:{section_index:03d}:{chunk_index:03d}",
                    doc_id=doc_id,
                    domain=domain,
                    title=source_path.name,
                    text=chunk_text,
                    section=section,
                    source=KnowledgeSource(source_file=source_path.name, source_path=str(source_path)),
                    version=version,
                    chunk_index=chunk_index,
                    metadata=dict(metadata or {}),
                )
            )
    return document, chunks


def parse_pdf_document(
    path: str | Path,
    domain: str,
    version: str = "1.0",
    metadata: dict[str, object] | None = None,
    chunk_size: int = 900,
    chunk_overlap: int = 120,
) -> tuple[KnowledgeDocument, list[KnowledgeChunk]]:
    """解析通用 PDF，按页保留来源，不应用药品说明书专用字段规则。"""
    source_path = Path(path)
    if source_path.suffix.lower() != ".pdf":
        raise ValueError(f"Unsupported PDF document: {source_path.suffix}")
    if chunk_size <= 0 or chunk_overlap < 0 or chunk_overlap >= chunk_size:
        raise ValueError("chunk_overlap must be smaller than a positive chunk_size")

    pages = load_pdf_pages(source_path)
    doc_id = hashlib.md5(source_path.read_bytes()).hexdigest()
    drug_metadata = _extract_drug_metadata(pages, source_path.stem) if domain == "drug" else {}
    document = KnowledgeDocument(
        doc_id=doc_id,
        domain=domain,
        title=source_path.name,
        version=version,
        source_type="pdf",
        metadata={**drug_metadata, **dict(metadata or {})},
    )
    chunks: list[KnowledgeChunk] = []
    for page in pages:
        text = _normalize_pdf_text(page.text)
        if not text:
            continue
        section = _pdf_page_section(text, page.page_number)
        for chunk_index, chunk_text in enumerate(_split_text(text, chunk_size, chunk_overlap)):
            chunks.append(
                KnowledgeChunk(
                    chunk_id=f"{doc_id}:page-{page.page_number:04d}:{chunk_index:03d}",
                    doc_id=doc_id,
                    domain=domain,
                    title=source_path.name,
                    text=chunk_text,
                    section=section,
                    source=KnowledgeSource(
                        source_file=source_path.name,
                        source_path=str(source_path),
                        page_start=page.page_number,
                        page_end=page.page_number,
                    ),
                    version=version,
                    chunk_index=chunk_index,
                    metadata={**drug_metadata, **dict(metadata or {})},
                )
            )
    return document, chunks


def _extract_sections(text: str) -> list[tuple[str, str]]:
    sections: list[tuple[str, str]] = []
    current_name = "全文"
    current_lines: list[str] = []
    for line in text.splitlines():
        match = HEADING_PATTERN.match(line.strip())
        if match:
            _append_section(sections, current_name, current_lines)
            current_name = match.group(1).strip()
            current_lines = []
        else:
            current_lines.append(line)
    _append_section(sections, current_name, current_lines)
    return sections


def _normalize_pdf_text(text: str) -> str:
    return "\n".join(line.strip() for line in text.replace("\r", "\n").splitlines() if line.strip())


def _pdf_page_section(text: str, page_number: int) -> str:
    for section in ("药品名称", "适应症", "用法用量", "禁忌", "注意事项", "不良反应", "药物相互作用", "特殊人群用药"):
        if f"【{section}】" in text:
            return section
    first_line = text.splitlines()[0].strip()
    heading = HEADING_PATTERN.match(first_line)
    return heading.group(1).strip() if heading else f"第{page_number}页"


def _extract_drug_metadata(pages: list, filename_stem: str) -> dict[str, str]:
    text = "\n".join(page.text for page in pages)
    metadata = {"drug_name": "unknown", "generic_name": "unknown", "category": "unknown"}
    for key, pattern in (
        ("generic_name", r"通用名称\s*[\u003a\uff1a]\s*([^\r\n]+)"),
        ("drug_name", r"商品名称\s*[\u003a\uff1a]\s*([^\r\n]+)"),
    ):
        match = re.search(pattern, text)
        if match:
            metadata[key] = match.group(1).strip().replace("®", "")
    if metadata["generic_name"] == "unknown":
        candidates = re.findall(r"[\u4e00-\u9fff]{4,}", filename_stem)
        if candidates:
            metadata["generic_name"] = max(candidates, key=len)
    if metadata["drug_name"] == "unknown":
        metadata["drug_name"] = metadata["generic_name"]
    return metadata


def _read_document_text(path: Path) -> str:
    if path.suffix.lower() != ".docx":
        return path.read_text(encoding="utf-8")

    try:
        with zipfile.ZipFile(path) as document:
            xml_bytes = document.read("word/document.xml")
        root = ElementTree.fromstring(xml_bytes)
    except (OSError, KeyError, ElementTree.ParseError, zipfile.BadZipFile) as exc:
        raise ValueError(f"DOCX parse failed: {exc}") from exc

    paragraphs: list[str] = []
    for paragraph in root.iter():
        if not paragraph.tag.endswith("}p"):
            continue
        texts = [node.text for node in paragraph.iter() if node.tag.endswith("}t") and node.text]
        if texts:
            paragraphs.append("".join(texts))
    return "\n".join(paragraphs)


def _append_section(sections: list[tuple[str, str]], name: str, lines: list[str]) -> None:
    body = "\n".join(line.strip() for line in lines if line.strip()).strip()
    if body:
        sections.append((name, body))


def _split_text(text: str, chunk_size: int, chunk_overlap: int) -> list[str]:
    if len(text) <= chunk_size:
        return [text] if text else []

    chunks: list[str] = []
    start = 0
    while start < len(text):
        end = min(len(text), start + chunk_size)
        window = text[start:end]
        if end < len(text):
            cut = max(window.rfind("\n"), window.rfind("。"), window.rfind("；"))
            if cut > chunk_size * 0.5:
                end = start + cut + 1
        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)
        if end >= len(text):
            break
        start = end - chunk_overlap
    return chunks
