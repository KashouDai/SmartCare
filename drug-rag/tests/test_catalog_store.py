from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from drug_rag.catalog import CollectionEntry, load_catalog, publish_collection, save_catalog


class CatalogStoreTest(unittest.TestCase):
    def test_catalog_round_trip_preserves_version_fields(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "catalog.json"
            catalog = load_catalog(path)
            catalog.register_collection(
                CollectionEntry(
                    version_id="drug-v1",
                    domain="drug",
                    collection_name="drug_chunks_v1",
                    summary_collection_name="drug_summaries_v1",
                    embedding_model="hash",
                    status="published",
                    document_count=1,
                    chunk_count=2,
                )
            )
            catalog.activate("drug", "drug-v1")
            save_catalog(catalog, path)

            loaded = load_catalog(path)

        self.assertEqual(loaded.schema_version, 2)
        self.assertEqual(loaded.active_version("drug"), "drug-v1")
        self.assertEqual(loaded.active_collection("drug").summary_collection_name, "drug_summaries_v1")

    def test_publish_collection_switches_active_and_retains_old_version(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "catalog.json"
            publish_collection(
                path,
                CollectionEntry(
                    version_id="policy-v1",
                    domain="hospital_policy",
                    collection_name="policy_chunks_v1",
                    embedding_model="hash",
                    status="published",
                ),
            )
            result = publish_collection(
                path,
                CollectionEntry(
                    version_id="policy-v2",
                    domain="hospital_policy",
                    collection_name="policy_chunks_v2",
                    embedding_model="hash",
                    status="published",
                ),
            )

        self.assertEqual(result.active_version("hospital_policy"), "policy-v2")
        self.assertEqual(result.domains["hospital_policy"].retained, ["policy-v1"])

    def test_unpublished_collection_is_not_written(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "catalog.json"
            with self.assertRaises(ValueError):
                publish_collection(
                    path,
                    CollectionEntry(
                        version_id="policy-indexing",
                        domain="hospital_policy",
                        collection_name="policy_chunks_indexing",
                        embedding_model="hash",
                    ),
                )
            self.assertFalse(path.exists())
