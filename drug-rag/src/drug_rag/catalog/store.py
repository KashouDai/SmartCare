from __future__ import annotations

import json
from pathlib import Path

from drug_rag.catalog.models import Catalog


def load_catalog(path: str | Path) -> Catalog:
    catalog_path = Path(path)
    if not catalog_path.exists():
        return Catalog()
    payload = json.loads(catalog_path.read_text(encoding="utf-8"))
    return Catalog.from_dict(payload)


def save_catalog(catalog: Catalog, path: str | Path) -> None:
    """以临时文件替换方式保存 Catalog，避免写入过程中留下半份 JSON。"""
    catalog_path = Path(path)
    catalog_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = catalog_path.with_name(f".{catalog_path.name}.tmp")
    temporary_path.write_text(
        json.dumps(catalog.to_dict(), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    temporary_path.replace(catalog_path)
