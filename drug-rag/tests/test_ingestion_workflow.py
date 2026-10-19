from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from drug_rag.catalog import load_catalog
from drug_rag.ingestion.workflow import build_and_publish_knowledge_version


class IngestionWorkflowTest(unittest.TestCase):
    def test_batch_index_and_catalog_publish_are_connected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            input_dir = root / "input"
            input_dir.mkdir()
            (input_dir / "流程.md").write_text("# 入院\n提交材料。", encoding="utf-8")
            catalog_path = root / "catalog.json"

            with patch(
                "drug_rag.ingestion.workflow.build_knowledge_vector_index",
                return_value=1,
            ) as build_index, patch(
                "drug_rag.ingestion.workflow.build_knowledge_summary_index",
                return_value=1,
            ) as build_summary:
                report, catalog = build_and_publish_knowledge_version(
                    input_dir=input_dir,
                    work_dir=root / "work",
                    catalog_path=catalog_path,
                    domain="hospital_policy",
                    version_id="policy-20261018-v1",
                    collection_name="policy_chunks_v1",
                )

            persisted = load_catalog(catalog_path)

        self.assertEqual(report.doc_count, 1)
        self.assertEqual(report.chunk_count, 1)
        self.assertEqual(catalog.active_version("hospital_policy"), "policy-20261018-v1")
        self.assertEqual(persisted.active_collection("hospital_policy").chunk_count, 1)
        build_index.assert_called_once()
        build_summary.assert_called_once()

    def test_failed_batch_does_not_publish_catalog_version(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            input_dir = root / "input"
            input_dir.mkdir()
            (input_dir / "bad.txt").write_bytes(b"\xff\xfe")
            catalog_path = root / "catalog.json"

            with self.assertRaises(ValueError):
                build_and_publish_knowledge_version(
                    input_dir=input_dir,
                    work_dir=root / "work",
                    catalog_path=catalog_path,
                    domain="hospital_policy",
                    version_id="policy-failed-v1",
                    collection_name="policy_chunks_failed",
                )

            self.assertFalse(catalog_path.exists())
