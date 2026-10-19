from __future__ import annotations

from pathlib import Path

from drug_rag.catalog.models import Catalog, CollectionEntry
from drug_rag.catalog.store import load_catalog, save_catalog


def publish_collection(
    catalog_path: str | Path,
    collection: CollectionEntry,
) -> Catalog:
    """发布已构建的索引版本，并原子切换对应领域的 active 指针。"""
    if collection.status != "published":
        raise ValueError("Collection must be published before activation")

    catalog = load_catalog(catalog_path)
    catalog.register_collection(collection)
    catalog.activate(collection.domain, collection.version_id)
    save_catalog(catalog, catalog_path)
    return catalog
