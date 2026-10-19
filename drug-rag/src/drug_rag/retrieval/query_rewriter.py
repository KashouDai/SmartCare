from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any

from drug_rag.retrieval.intent_router import QueryRoute, route_question
from drug_rag.retrieval.llm_intent_classifier import route_question_with_llm


INTENT_ANCHORS = {
    "dosage": ["成人", "给药频次", "剂量调整"],
    "indication": ["治疗疾病", "适应症"],
    "contraindication": ["禁用", "慎用", "风险人群"],
    "adverse_reaction": ["常见不良反应", "严重不良反应"],
    "interaction": ["合并用药", "相互作用", "降压药"],
    "special_population": ["特殊人群", "孕妇", "儿童", "老年患者"],
    "storage": ["保存条件", "贮藏", "常温", "冷藏", "冷冻", "温度", "有效期"],
    "purchase_guidance": ["购药指导", "适应症", "禁忌", "用法用量"],
    "general_qa": [],
}


@dataclass(slots=True)
class QueryRewrite:
    original_question: str
    rewritten_question: str
    route: QueryRoute
    anchors: list[str]

    def to_dict(self) -> dict[str, Any]:
        data = asdict(self)
        data["route"] = self.route.to_dict()
        return data


def build_query_rewrite(
    question: str,
    drug_name: str | None = None,
    generic_name: str | None = None,
    intent_classifier: dict[str, Any] | None = None,
) -> QueryRewrite:
    route = route_question(question)
    intent_classifier = intent_classifier or {}
    if intent_classifier.get("enabled"):
        route = route_question_with_llm(
            question,
            route,
            provider=intent_classifier.get("provider", "none"),
            model=intent_classifier.get("model"),
            base_url=intent_classifier.get("base_url"),
            api_key_env=intent_classifier.get("api_key_env", "DASHSCOPE_API_KEY"),
            temperature=float(intent_classifier.get("temperature", 0.0)),
        )
    anchors = []
    if drug_name and drug_name != "unknown":
        anchors.append(drug_name)
    if generic_name and generic_name != "unknown":
        anchors.append(generic_name)
    anchors.extend(route.sections)
    if route.intent == "special_population":
        anchors.extend(_special_population_anchors(route.sections))
    else:
        anchors.extend(INTENT_ANCHORS.get(route.intent, []))
    anchors.extend(route.matched_keywords)
    anchors.append(question)
    unique_anchors = list(dict.fromkeys(anchor for anchor in anchors if anchor))
    return QueryRewrite(
        original_question=question,
        rewritten_question=" ".join(unique_anchors),
        route=route,
        anchors=unique_anchors,
    )


def rewrite_question(
    question: str,
    drug_name: str | None = None,
    generic_name: str | None = None,
    intent_classifier: dict[str, Any] | None = None,
) -> str:
    return build_query_rewrite(
        question,
        drug_name=drug_name,
        generic_name=generic_name,
        intent_classifier=intent_classifier,
    ).rewritten_question


def _special_population_anchors(sections: list[str]) -> list[str]:
    anchors = ["特殊人群"]
    if "孕妇及哺乳期妇女用药" in sections:
        anchors.extend(["孕妇", "妊娠", "哺乳"])
    if "儿童用药" in sections:
        anchors.extend(["儿童", "儿童安全性"])
    if "老年用药" in sections or "老年患者用药" in sections:
        anchors.extend(["老年患者", "老年用药"])
    if sections == ["注意事项", "禁忌"] or "禁忌" in sections:
        anchors.extend(["慎用", "禁忌"])
    return anchors
