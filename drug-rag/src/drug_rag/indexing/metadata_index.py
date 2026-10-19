from __future__ import annotations

import json
from collections import defaultdict
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

from drug_rag.io_utils import ensure_parent, read_jsonl


@dataclass(slots=True)
class DrugConstraint:
    drug_name: str | None = None
    generic_name: str | None = None
    category: str | None = None
    matched_drugs: list[dict[str, Any]] | None = None
    matched_categories: list[str] | None = None
    ambiguous: bool = False

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def build_drug_directory(jsonl_path: str | Path, out_path: str | Path) -> dict[str, Any]:
    """从 master JSONL 聚合出药品目录。

    药品目录不是向量索引，而是结构化目录：记录药品名、通用名、分类、
    来源文件、章节列表和页码。问答前会用它识别用户问题中的药品/分类约束。
    """
    grouped: dict[str, list] = defaultdict(list)
    with Path(jsonl_path).open(encoding="utf-8") as source:
        first_line = next((line for line in source if line.strip()), "")
    if first_line and "\"domain\"" in first_line:
        with Path(jsonl_path).open(encoding="utf-8") as source:
            for line in source:
                if not line.strip():
                    continue
                payload = json.loads(line)
                metadata = payload.get("metadata", {})
                grouped[payload["doc_id"]].append(
                    {
                        "drug_name": metadata.get("drug_name", "unknown"),
                        "generic_name": metadata.get("generic_name", "unknown"),
                        "category": metadata.get("category", "unknown"),
                        "source": payload["source"],
                        "section": payload.get("section", ""),
                    }
                )
    else:
        for chunk in read_jsonl(jsonl_path):
            grouped[chunk.doc_id].append(chunk)

    drugs = []
    for doc_id, chunks in grouped.items():
        first = chunks[0]
        first_drug_name = first["drug_name"] if isinstance(first, dict) else first.drug_name
        first_generic_name = first["generic_name"] if isinstance(first, dict) else first.generic_name
        first_category = first["category"] if isinstance(first, dict) else first.category
        sections = sorted({chunk["section"] if isinstance(chunk, dict) else chunk.section for chunk in chunks})
        section_pages: dict[str, list[int]] = defaultdict(list)
        for chunk in chunks:
            section = chunk["section"] if isinstance(chunk, dict) else chunk.section
            source = chunk["source"] if isinstance(chunk, dict) else chunk.source.to_dict()
            start = source.get("page_start") or 0
            end = source.get("page_end") or start
            section_pages[section].extend(range(start, end + 1))
        drugs.append(
            {
                "doc_id": doc_id,
                "drug_name": first_drug_name,
                "generic_name": first_generic_name,
                "category": first_category,
                "source_file": (first["source"] if isinstance(first, dict) else first.source.to_dict())["source_file"],
                "source_path": (first["source"] if isinstance(first, dict) else first.source.to_dict())["source_path"],
                "sections": sections,
                "section_pages": {
                    section: sorted(set(pages)) for section, pages in section_pages.items()
                },
                "chunk_count": len(chunks),
            }
        )

    categories = sorted({item["category"] for item in drugs if item.get("category")})
    category_counts = {
        category: sum(1 for item in drugs if item.get("category") == category)
        for category in categories
    }
    directory = {
        "drug_count": len(drugs),
        "categories": categories,
        "category_counts": category_counts,
        "drugs": sorted(drugs, key=lambda item: (item["category"], item["generic_name"], item["drug_name"])),
    }
    path = ensure_parent(out_path)
    path.write_text(json.dumps(directory, ensure_ascii=False, indent=2), encoding="utf-8")
    return directory


def load_drug_directory(path: str | Path) -> dict[str, Any]:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def find_drugs(
    directory: dict[str, Any],
    drug_name: str | None = None,
    generic_name: str | None = None,
    category: str | None = None,
    section: str | None = None,
) -> list[dict[str, Any]]:
    """按商品名、通用名、分类或章节在药品目录中查药品。"""
    results = []
    for item in directory.get("drugs", []):
        if drug_name and drug_name not in item.get("drug_name", ""):
            continue
        if generic_name and generic_name not in item.get("generic_name", ""):
            continue
        if category and category != item.get("category"):
            continue
        if section and section not in item.get("sections", []):
            continue
        results.append(item)
    return results


def resolve_query_constraints(directory: dict[str, Any], question: str) -> DrugConstraint:
    """从用户问题中解析药品约束。

    如果问题唯一命中某个药品，则返回 drug_name/generic_name/category；
    如果只命中一个分类，则返回 category；如果命中多个药品，则标记 ambiguous，
    避免系统误把多个药品混成一个答案。
    """
    normalized_question = _normalize(question)
    matched_drugs = []
    for item in directory.get("drugs", []):
        names = _drug_match_names(item)
        if any(name and name in normalized_question for name in names):
            matched_drugs.append(item)

    matched_categories = [
        category
        for category in directory.get("categories", [])
        if category and _normalize(category) in normalized_question
    ]

    unique_doc_ids = {item.get("doc_id") for item in matched_drugs}
    if len(unique_doc_ids) == 1 and matched_drugs:
        item = matched_drugs[0]
        return DrugConstraint(
            drug_name=item.get("drug_name"),
            generic_name=item.get("generic_name"),
            category=item.get("category"),
            matched_drugs=matched_drugs,
            matched_categories=matched_categories,
            ambiguous=False,
        )

    if not matched_drugs and len(matched_categories) == 1:
        return DrugConstraint(
            category=matched_categories[0],
            matched_drugs=[],
            matched_categories=matched_categories,
            ambiguous=False,
        )

    return DrugConstraint(
        matched_drugs=matched_drugs,
        matched_categories=matched_categories,
        ambiguous=len(unique_doc_ids) > 1 or len(matched_categories) > 1,
    )


def _drug_match_names(item: dict[str, Any]) -> list[str]:
    names = [
        item.get("drug_name", ""),
        item.get("generic_name", ""),
        Path(item.get("source_file", "")).stem,
    ]
    # 说明书中的通用名可能带有剂型修饰词，用户提问时常会省略剂型或“酯”等修饰词。
    # 最终仍由 doc_id 唯一性检查避免误合并。
    generic_name = str(item.get("generic_name", ""))
    if generic_name.endswith("酯片"):
        names.append(generic_name[:-2] + "片")
    for suffix in ("肠溶胶囊", "缓释胶囊", "分散片", "胶囊", "颗粒", "片"):
        if generic_name.endswith(suffix) and len(generic_name) > len(suffix):
            names.append(generic_name[:-len(suffix)])
            break
    return [_normalize(name) for name in names if name and name != "unknown"]


def _normalize(text: str) -> str:
    return (
        str(text)
        .lower()
        .replace(" ", "")
        .replace("_", "")
        .replace("-", "")
        .replace("®", "")
        .replace("（", "(")
        .replace("）", ")")
    )
