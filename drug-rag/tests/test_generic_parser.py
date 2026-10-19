from __future__ import annotations

import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from drug_rag.ingestion import parse_pdf_document, parse_text_document
from drug_rag.loaders import PdfPage


class GenericParserTest(unittest.TestCase):
    def test_markdown_preserves_domain_sections_and_metadata(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "流程.md"
            path.write_text("# 入院流程\n准备身份证。\n\n## 办理步骤\n提交材料。", encoding="utf-8")

            document, chunks = parse_text_document(
                path,
                domain="hospital_policy",
                version="2026.10",
                metadata={"department": "住院部"},
            )

        self.assertEqual(document.domain, "hospital_policy")
        self.assertEqual(document.version, "2026.10")
        self.assertEqual(document.source_type, "md")
        self.assertEqual([chunk.section for chunk in chunks], ["入院流程", "办理步骤"])
        self.assertEqual(chunks[0].metadata["department"], "住院部")
        self.assertEqual(chunks[0].source.page_start, None)

    def test_plain_text_uses_full_text_section(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "宣教.txt"
            path.write_text("按时复诊。\n保持规律作息。", encoding="utf-8")

            _, chunks = parse_text_document(path, domain="health_education")

        self.assertEqual(len(chunks), 1)
        self.assertEqual(chunks[0].section, "全文")
        self.assertIn("按时复诊", chunks[0].text)

    def test_docx_extracts_paragraph_text(self) -> None:
        document_xml = (
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
            '<w:body><w:p><w:r><w:t>检查前准备</w:t></w:r></w:p>'
            '<w:p><w:r><w:t>检查前八小时禁食。</w:t></w:r></w:p></w:body></w:document>'
        ).encode("utf-8")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "检查.docx"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("word/document.xml", document_xml)

            document, chunks = parse_text_document(path, domain="care_instruction")

        self.assertEqual(document.source_type, "docx")
        self.assertEqual(chunks[0].section, "全文")
        self.assertIn("检查前准备", chunks[0].text)
        self.assertIn("禁食", chunks[0].text)

    def test_generic_pdf_preserves_page_source_without_drug_rules(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "检查注意事项.pdf"
            path.write_bytes(b"pdf fixture")
            pages = [
                PdfPage(1, "# 检查前准备\n检查前八小时禁食。"),
                PdfPage(2, "检查当天携带相关资料。"),
            ]

            with patch("drug_rag.ingestion.generic_parser.load_pdf_pages", return_value=pages):
                document, chunks = parse_pdf_document(path, domain="care_instruction")

        self.assertEqual(document.source_type, "pdf")
        self.assertEqual([chunk.section for chunk in chunks], ["检查前准备", "第2页"])
        self.assertEqual(chunks[0].source.page_start, 1)
        self.assertEqual(chunks[1].source.page_start, 2)
        self.assertNotIn("drug_name", chunks[0].metadata)

    def test_rejects_unsupported_extension_and_invalid_overlap(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "doc.pdf"
            path.write_bytes(b"not a pdf")

            with self.assertRaises(ValueError):
                parse_text_document(path, domain="drug")

            txt_path = Path(directory) / "doc.txt"
            txt_path.write_text("内容", encoding="utf-8")
            with self.assertRaises(ValueError):
                parse_text_document(txt_path, domain="drug", chunk_size=10, chunk_overlap=10)
