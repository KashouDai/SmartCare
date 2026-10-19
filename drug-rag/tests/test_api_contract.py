from __future__ import annotations

import os
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from fastapi.testclient import TestClient

from drug_rag.api import main


class ApiContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.previous_token = os.environ.get("KNOWLEDGE_INTERNAL_TOKEN")
        os.environ["KNOWLEDGE_INTERNAL_TOKEN"] = "test-token"
        self.client = TestClient(main.create_app())

    def tearDown(self) -> None:
        if self.previous_token is None:
            os.environ.pop("KNOWLEDGE_INTERNAL_TOKEN", None)
        else:
            os.environ["KNOWLEDGE_INTERNAL_TOKEN"] = self.previous_token

    def test_health_is_exempt_from_internal_token(self) -> None:
        response = self.client.get("/api/health")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "ok")

    def test_ask_rejects_missing_or_invalid_token(self) -> None:
        payload = {"question": "测试问题"}

        missing = self.client.post("/api/ask", json=payload, headers={})
        invalid = self.client.post("/api/ask", json=payload, headers={"x-internal-token": "wrong"})

        self.assertEqual(missing.status_code, 401)
        self.assertEqual(invalid.status_code, 401)

    def test_ask_returns_compatible_shared_response(self) -> None:
        fake_result = SimpleNamespace(
            to_dict=lambda: {
                "answer": "根据说明书回答。",
                "domain": "drug",
                "status": "succeeded",
                "sources": [],
                "trace": {},
                "safety": {"risk_level": "low"},
            }
        )
        fake_runtime = {
            "llm_config": {"provider": "none"},
            "rerank_config": {},
            "intent_classifier_config": {},
            "prompts_path": "configs/prompts.yaml",
            "vector_store": object(),
            "summary_store": object(),
        }

        with patch.object(main, "get_runtime", return_value=fake_runtime), patch.object(
            main, "answer_question_with_trace", return_value=fake_result
        ):
            response = self.client.post(
                "/api/ask",
                json={"question": "测试问题"},
                headers={"x-internal-token": "test-token"},
            )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["domain"], "drug")
        self.assertEqual(body["status"], "succeeded")
        self.assertRegex(body["requestId"], r"^[0-9a-f-]{36}$")
        for field in ("answer", "sources", "trace", "safety"):
            self.assertIn(field, body)

    def test_ask_rejects_unregistered_domain(self) -> None:
        response = self.client.post(
            "/api/ask",
            json={"question": "测试问题", "domain": "hospital_policy"},
            headers={"x-internal-token": "test-token"},
        )

        self.assertEqual(response.status_code, 422)
        self.assertIn("Unsupported knowledge domain", response.json()["detail"])

    def test_ask_accepts_generic_filters_for_drug_constraints(self) -> None:
        fake_result = type("FakeResult", (), {"to_dict": lambda self: {
            "answer": "ok", "domain": "drug", "status": "succeeded",
            "sources": [], "trace": {}, "safety": {},
        }})()
        fake_runtime = {
            "llm_config": {"provider": "none"},
            "rerank_config": {},
            "intent_classifier_config": {},
            "prompts_path": "configs/prompts.yaml",
            "vector_store": object(),
            "summary_store": object(),
        }
        with patch.object(main, "get_runtime", return_value=fake_runtime), patch.object(
            main, "answer_question_with_trace", return_value=fake_result
        ), patch.object(main, "_resolve_constraints", return_value={
            "drug_name": "测试药",
            "generic_name": None,
            "category": None,
            "drug_constraint": None,
        }) as resolve_constraints:
            response = self.client.post(
                "/api/ask",
                json={"question": "怎么吃", "filters": {"drug_name": "测试药"}},
                headers={"x-internal-token": "test-token"},
            )

        self.assertNotEqual(response.status_code, 422)
        resolve_constraints.assert_called_once()

    def test_ready_can_report_unready_without_calling_external_models(self) -> None:
        settings = main.AppSettings()
        with patch.object(main, "get_settings", return_value=settings), patch.object(
            main, "load_yaml", return_value={}
        ), patch.object(
            main, "_check_chroma", return_value={"ok": True}
        ), patch.object(
            main, "_check_catalog", return_value={"ok": True}
        ), patch.object(
            main, "_check_env", return_value={"ok": True}
        ):
            response = self.client.get("/api/ready")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "ready")


if __name__ == "__main__":
    unittest.main()
