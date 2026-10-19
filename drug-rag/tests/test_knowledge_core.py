from __future__ import annotations

import unittest
from unittest.mock import Mock

from drug_rag.chains.qa_chain import AnswerResult
from drug_rag.catalog import Catalog, CollectionEntry
from drug_rag.domains import UnknownKnowledgeDomainError, build_default_registry
from drug_rag.schemas import DrugChunk, DrugDocument, KnowledgeChunk, KnowledgeDocument, SourceSpan


class KnowledgeModelTest(unittest.TestCase):
    def test_drug_document_converts_to_generic_document(self) -> None:
        document = DrugDocument(
            doc_id="doc-1",
            source_file="说明书.pdf",
            source_path="data/raw/说明书.pdf",
            pages=2,
            drug_name="测试药",
        )

        result = KnowledgeDocument.from_drug_document(document)

        self.assertEqual(result.domain, "drug")
        self.assertEqual(result.title, "说明书.pdf")
        self.assertEqual(result.metadata["drug_name"], "测试药")

    def test_drug_chunk_preserves_source_and_drug_metadata(self) -> None:
        chunk = DrugChunk(
            chunk_id="chunk-1",
            doc_id="doc-1",
            text="禁忌内容",
            section="禁忌",
            source=SourceSpan("说明书.pdf", "data/raw/说明书.pdf", 1, 1),
            drug_name="测试药",
            chunk_index=2,
        )

        result = KnowledgeChunk.from_drug_chunk(chunk)

        self.assertEqual(result.domain, "drug")
        self.assertEqual(result.source.page_start, 1)
        self.assertEqual(result.metadata["drug_name"], "测试药")
        self.assertEqual(result.chunk_index, 2)


class DomainRegistryTest(unittest.TestCase):
    def test_default_registry_only_registers_drug(self) -> None:
        registry = build_default_registry()

        self.assertEqual(registry.names(), ("drug",))
        self.assertEqual(registry.get("drug").display_name, "药品说明书")

    def test_unknown_domain_does_not_fallback_to_drug(self) -> None:
        registry = build_default_registry()

        with self.assertRaises(UnknownKnowledgeDomainError):
            registry.get("hospital_policy")


class AnswerResponseTest(unittest.TestCase):
    def test_answer_result_contains_shared_response_fields(self) -> None:
        trace = Mock()
        trace.results = []
        trace.to_dict.return_value = {"results": []}
        safety = Mock()
        safety.to_dict.return_value = {"risk_level": "low"}

        payload = AnswerResult(
            answer="根据说明书回答。",
            trace=trace,
            safety=safety,
        ).to_dict()

        self.assertEqual(payload["domain"], "drug")
        self.assertEqual(payload["status"], "succeeded")
        self.assertIn("sources", payload)
        self.assertIn("trace", payload)
        self.assertIn("safety", payload)


class CatalogVersionTest(unittest.TestCase):
    def test_legacy_catalog_is_read_as_current_schema(self) -> None:
        catalog = Catalog.from_dict(
            {
                "schema_version": 1,
                "revision": 0,
                "domains": {},
                "collections": {},
            }
        )

        self.assertEqual(catalog.schema_version, 2)

    def test_catalog_can_register_and_activate_published_version(self) -> None:
        catalog = Catalog()
        first = CollectionEntry(
            version_id="policy-20261018-v1",
            domain="hospital_policy",
            collection_name="policy_chunks_v1",
            summary_collection_name="policy_summaries_v1",
            embedding_model="hash",
            status="published",
            document_count=2,
            chunk_count=4,
        )
        second = CollectionEntry(
            version_id="policy-20261019-v1",
            domain="hospital_policy",
            collection_name="policy_chunks_v2",
            embedding_model="hash",
            status="published",
        )

        catalog.register_collection(first)
        catalog.activate("hospital_policy", first.version_id)
        catalog.register_collection(second)
        catalog.activate("hospital_policy", second.version_id)

        self.assertEqual(catalog.active_version("hospital_policy"), second.version_id)
        self.assertEqual(catalog.domains["hospital_policy"].retained, [first.version_id])
        self.assertEqual(catalog.active_collection("hospital_policy").collection_name, "policy_chunks_v2")
        self.assertEqual(catalog.collections[first.version_id].document_count, 2)

    def test_catalog_rejects_activation_of_indexing_version(self) -> None:
        catalog = Catalog()
        version = CollectionEntry(
            version_id="policy-indexing-v1",
            domain="hospital_policy",
            collection_name="policy_chunks_indexing",
            embedding_model="hash",
        )
        catalog.register_collection(version)

        with self.assertRaises(ValueError):
            catalog.activate("hospital_policy", version.version_id)


if __name__ == "__main__":
    unittest.main()
