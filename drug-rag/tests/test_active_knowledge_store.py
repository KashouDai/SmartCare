from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from drug_rag.catalog import CollectionEntry, publish_collection
from drug_rag.indexing.knowledge_vector_index import load_active_knowledge_stores
from test_knowledge_vector_index import FakeVectorStore


class ActiveKnowledgeStoreTest(unittest.TestCase):
    def test_loads_chunk_and_summary_collections_from_active_catalog(self) -> None:
        FakeVectorStore.instances.clear()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            catalog_path = root / "catalog.json"
            publish_collection(
                catalog_path,
                CollectionEntry(
                    version_id="policy-v1",
                    domain="hospital_policy",
                    collection_name="policy_chunks_v1",
                    summary_collection_name="policy_summaries_v1",
                    embedding_model="hash",
                    status="published",
                ),
            )
            with patch(
                "drug_rag.indexing.knowledge_vector_index._chroma_class",
                return_value=FakeVectorStore,
            ), patch(
                "drug_rag.indexing.knowledge_vector_index.build_embeddings",
                return_value=object(),
            ):
                stores = load_active_knowledge_stores(catalog_path, "hospital_policy", root / "chroma")

        self.assertEqual(stores["version_id"], "policy-v1")
        self.assertEqual(stores["chunks"].kwargs["collection_name"], "policy_chunks_v1")
        self.assertEqual(stores["summaries"].kwargs["collection_name"], "policy_summaries_v1")

    def test_missing_active_domain_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                load_active_knowledge_stores(Path(directory) / "missing.json", "drug")
