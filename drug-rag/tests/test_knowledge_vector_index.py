from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from drug_rag.indexing.knowledge_vector_index import build_knowledge_vector_index
from drug_rag.ingestion import parse_text_document, write_knowledge_jsonl


class FakeVectorStore:
    instances: list["FakeVectorStore"] = []

    def __init__(self, **kwargs) -> None:
        self.kwargs = kwargs
        self.deleted_ids = []
        self.texts = []
        self.metadatas = []
        self.ids = []
        self.persisted = False
        self.__class__.instances.append(self)

    def delete(self, ids):
        self.deleted_ids = ids

    def add_texts(self, texts, metadatas, ids):
        self.texts = texts
        self.metadatas = metadatas
        self.ids = ids

    def persist(self):
        self.persisted = True


class KnowledgeVectorIndexTest(unittest.TestCase):
    def test_builds_independent_collection_with_flat_metadata(self) -> None:
        FakeVectorStore.instances.clear()
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "制度.md"
            source.write_text("# 入院\n提交材料。", encoding="utf-8")
            _, chunks = parse_text_document(source, domain="hospital_policy", version="2026.10")
            jsonl = Path(directory) / "chunks.jsonl"
            write_knowledge_jsonl(chunks, jsonl)

            with patch(
                "drug_rag.indexing.knowledge_vector_index._chroma_class",
                return_value=FakeVectorStore,
            ), patch(
                "drug_rag.indexing.knowledge_vector_index.build_embeddings",
                return_value=object(),
            ):
                count = build_knowledge_vector_index(jsonl, Path(directory) / "chroma", "policy_chunks")

        store = FakeVectorStore.instances[0]
        self.assertEqual(count, 1)
        self.assertEqual(store.kwargs["collection_name"], "policy_chunks")
        self.assertEqual(store.ids, [chunks[0].chunk_id])
        self.assertEqual(store.deleted_ids, store.ids)
        self.assertEqual(store.metadatas[0]["domain"], "hospital_policy")
        self.assertEqual(store.metadatas[0]["version"], "2026.10")
        self.assertEqual(store.metadatas[0]["page_start"], -1)
        self.assertTrue(store.persisted)

    def test_empty_jsonl_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            jsonl = Path(directory) / "empty.jsonl"
            jsonl.write_text("", encoding="utf-8")

            with self.assertRaises(ValueError):
                build_knowledge_vector_index(jsonl)
