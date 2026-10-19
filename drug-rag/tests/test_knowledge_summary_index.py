from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from drug_rag.ingestion import parse_text_document, write_knowledge_jsonl
from drug_rag.indexing.knowledge_summary_index import build_knowledge_summary_index
from test_knowledge_vector_index import FakeVectorStore


class KnowledgeSummaryIndexTest(unittest.TestCase):
    def test_builds_one_summary_per_document_section(self) -> None:
        FakeVectorStore.instances.clear()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "制度.md"
            source.write_text("# 入院\n提交材料。\n\n# 出院\n办理结算。", encoding="utf-8")
            _, chunks = parse_text_document(source, domain="hospital_policy", version="2026.10")
            jsonl = root / "chunks.jsonl"
            write_knowledge_jsonl(chunks, jsonl)

            with patch(
                "drug_rag.indexing.knowledge_summary_index._chroma_class",
                return_value=FakeVectorStore,
            ), patch(
                "drug_rag.indexing.knowledge_summary_index.build_embeddings",
                return_value=object(),
            ):
                count = build_knowledge_summary_index(jsonl, root / "chroma", "policy_summaries")

        store = FakeVectorStore.instances[0]
        self.assertEqual(count, 2)
        self.assertEqual(len(store.texts), 2)
        self.assertEqual({metadata["section"] for metadata in store.metadatas}, {"入院", "出院"})
        self.assertTrue(all(metadata["domain"] == "hospital_policy" for metadata in store.metadatas))
