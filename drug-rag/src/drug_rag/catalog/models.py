from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Iterable, Literal

SCHEMA_VERSION = 2

AccessScope = Literal["public", "authenticated", "internal"]
CollectionStatus = Literal["indexing", "published", "offline", "failed"]


@dataclass(slots=True)
class CatalogDocument:
    """文档级清单，用于按 Catalog 重建索引。"""

    doc_id: str
    title: str
    version: str | None = None
    content_hash: str | None = None
    source_type: str = "other"
    access_scope: AccessScope = "public"

    def to_dict(self) -> dict[str, Any]:
        return {
            "doc_id": self.doc_id,
            "title": self.title,
            "version": self.version,
            "content_hash": self.content_hash,
            "source_type": self.source_type,
            "access_scope": self.access_scope,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> CatalogDocument:
        return cls(
            doc_id=data["doc_id"],
            title=data["title"],
            version=data.get("version"),
            content_hash=data.get("content_hash"),
            source_type=data.get("source_type", "other"),
            access_scope=data.get("access_scope", "public"),
        )


@dataclass(slots=True)
class CollectionEntry:
    """一个领域版本快照，对应一个 Chroma collection。"""

    version_id: str
    domain: str
    collection_name: str
    embedding_model: str
    summary_collection_name: str | None = None
    status: CollectionStatus = "indexing"
    created_at: str | None = None
    published_at: str | None = None
    document_count: int = 0
    chunk_count: int = 0
    documents: list[CatalogDocument] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "version_id": self.version_id,
            "domain": self.domain,
            "collection_name": self.collection_name,
            "embedding_model": self.embedding_model,
            "summary_collection_name": self.summary_collection_name,
            "status": self.status,
            "created_at": self.created_at,
            "published_at": self.published_at,
            "document_count": self.document_count,
            "chunk_count": self.chunk_count,
            "documents": [document.to_dict() for document in self.documents],
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> CollectionEntry:
        return cls(
            version_id=data["version_id"],
            domain=data["domain"],
            collection_name=data["collection_name"],
            embedding_model=data["embedding_model"],
            summary_collection_name=data.get("summary_collection_name"),
            status=data.get("status", "indexing"),
            created_at=data.get("created_at"),
            published_at=data.get("published_at"),
            document_count=int(data.get("document_count", 0)),
            chunk_count=int(data.get("chunk_count", 0)),
            documents=[CatalogDocument.from_dict(item) for item in data.get("documents", [])],
        )


@dataclass(slots=True)
class DomainEntry:
    """领域指针：active 指向当前生效版本，retained 保留可回退版本。"""

    active: str | None = None
    retained: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {"active": self.active, "retained": list(self.retained)}

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> DomainEntry:
        return cls(
            active=data.get("active"),
            retained=list(data.get("retained", [])),
        )


@dataclass(slots=True)
class Catalog:
    """跨领域版本注册表。

    版本切换只改 domains 的 active 指针；revision 单调递增，作为缓存键的一部分，
    使旧缓存条目自然失效，无需依赖可靠的缓存清理时序。
    """

    schema_version: int = SCHEMA_VERSION
    revision: int = 0
    updated_at: str | None = None
    domains: dict[str, DomainEntry] = field(default_factory=dict)
    collections: dict[str, CollectionEntry] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema_version": self.schema_version,
            "revision": self.revision,
            "updated_at": self.updated_at,
            "domains": {name: self.domains[name].to_dict() for name in sorted(self.domains)},
            "collections": {
                version_id: self.collections[version_id].to_dict()
                for version_id in sorted(self.collections)
            },
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> Catalog:
        return cls(
            schema_version=max(int(data.get("schema_version", SCHEMA_VERSION)), SCHEMA_VERSION),
            revision=int(data.get("revision", 0)),
            updated_at=data.get("updated_at"),
            domains={
                name: DomainEntry.from_dict(item)
                for name, item in data.get("domains", {}).items()
            },
            collections={
                version_id: CollectionEntry.from_dict(item)
                for version_id, item in data.get("collections", {}).items()
            },
        )

    def active_version(self, domain: str) -> str | None:
        entry = self.domains.get(domain)
        return entry.active if entry else None

    def active_collection(self, domain: str) -> CollectionEntry | None:
        version_id = self.active_version(domain)
        return self.collections.get(version_id) if version_id else None

    def catalog_version(self, domains: Iterable[str]) -> str:
        """按领域名排序拼接各领域当前生效版本，如 `drug-...-v1+disease-...-v1`。

        单领域时退化为该领域的版本串；没有任何生效版本时返回空串。
        """
        versions = [
            version
            for name in sorted(set(domains))
            if (version := self.active_version(name))
        ]
        return "+".join(versions)

    def register_collection(self, collection: CollectionEntry) -> None:
        if collection.version_id in self.collections:
            raise ValueError(f"Catalog version already exists: {collection.version_id}")
        if collection.created_at is None:
            collection.created_at = _now_iso()
        self.collections[collection.version_id] = collection
        self.revision += 1
        self.updated_at = _now_iso()

    def activate(self, domain: str, version_id: str) -> None:
        collection = self.collections.get(version_id)
        if collection is None or collection.domain != domain:
            raise ValueError(f"Catalog version does not belong to domain: {version_id}")
        if collection.status != "published":
            raise ValueError(f"Only published versions can be activated: {version_id}")

        domain_entry = self.domains.setdefault(domain, DomainEntry())
        previous = domain_entry.active
        if previous and previous != version_id and previous not in domain_entry.retained:
            domain_entry.retained.append(previous)
        domain_entry.active = version_id
        collection.published_at = collection.published_at or _now_iso()
        self.revision += 1
        self.updated_at = _now_iso()


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()
