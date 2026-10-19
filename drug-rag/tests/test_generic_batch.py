from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from drug_rag.ingestion import parse_document_batch, read_knowledge_jsonl


class GenericBatchTest(unittest.TestCase):
    def test_mixed_text_documents_are_written_to_generic_jsonl(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            input_dir = Path(directory) / "input"
            input_dir.mkdir()
            (input_dir / "制度.md").write_text("# 入院\n提交材料。", encoding="utf-8")
            (input_dir / "宣教.txt").write_text("按时复诊。", encoding="utf-8")
            output = Path(directory) / "processed" / "chunks.jsonl"

            report = parse_document_batch(
                input_dir,
                output,
                domain="hospital_policy",
                metadata={"department": "综合管理"},
            )
            chunks = list(read_knowledge_jsonl(output))

        self.assertEqual(report.doc_count, 2)
        self.assertEqual(report.chunk_count, 2)
        self.assertEqual(report.failed_count, 0)
        self.assertEqual(len(chunks), 2)
        self.assertTrue(all(chunk.domain == "hospital_policy" for chunk in chunks))
        self.assertTrue(all(chunk.metadata["department"] == "综合管理" for chunk in chunks))

    def test_unsupported_files_are_ignored(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            input_dir = Path(directory) / "input"
            input_dir.mkdir()
            (input_dir / "ignore.csv").write_text("a,b", encoding="utf-8")
            output = Path(directory) / "chunks.jsonl"

            report = parse_document_batch(input_dir, output, domain="health_education")
            output_content = output.read_text(encoding="utf-8")

        self.assertEqual(report.doc_count, 0)
        self.assertEqual(report.chunk_count, 0)
        self.assertEqual(output_content, "")
