from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any

from drug_rag.retrieval.intent_router import build_metadata_filter
from drug_rag.retrieval.query_rewriter import QueryRewrite, build_query_rewrite


AUTO_MODE = "auto"
AUTO_RERANK = "auto"

FIXED_SECTION_INTENTS = {"dosage", "indication", "storage"}
SAFETY_INTENTS = {"special_population", "contraindication", "interaction", "adverse_reaction", "purchase_guidance"}
DRUG_REQUIRED_INTENTS = {
    "dosage",
    "storage",
    "special_population",
    "contraindication",
    "interaction",
    "adverse_reaction",
}
DIRECTORY_QUERY_HINTS = [
    "买药",
    "想买",
    "买点",
    "购买",
    "推荐",
    "药店",
    "处方",
    "消炎药",
    "降压药",
    "降糖药",
    "止痛药",
    "胃药",
]


@dataclass(slots=True)
class RetrievalDecision:
    intent: str
    strategy: str
    metadata_filter: dict[str, Any]
    rewritten_query: str
    candidate_k: int
    final_k: int
    use_metadata: bool
    use_summary: bool
    use_vector: bool
    rerank: str
    reason: str
    need_clarification: bool = False
    clarification_question: str = ""
    route_type: str = "answerable"

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def build_retrieval_decision(
    question: str,
    drug_name: str | None = None,
    generic_name: str | None = None,
    category: str | None = None,
    top_k: int = 6,
    candidate_k: int | None = None,
    rerank: str = AUTO_RERANK,
    use_rewrite: bool = True,
    intent_classifier: dict[str, Any] | None = None,
) -> tuple[QueryRewrite, RetrievalDecision]:
    """根据问题意图选择自动检索策略。

    - 固定章节问题，如用法用量/适应症/贮藏，走 metadata_first。
    - 高风险或购药决策问题，走 safety_hybrid，避免漏掉禁忌和注意事项。
    - 有药品/分类约束但意图不明确，走 hybrid。
    - 泛问且无明确章节，走 summary_guided_hybrid，先用摘要定位章节。
    """
    rewrite = build_query_rewrite(
        question,
        drug_name=drug_name,
        generic_name=generic_name,
        intent_classifier=intent_classifier,
    )
    metadata_filter = build_metadata_filter(
        rewrite.route,
        drug_name=drug_name,
        generic_name=generic_name,
        category=category,
    )
    intent = rewrite.route.intent
    final_k = max(1, top_k)
    chosen_rerank = "keyword" if rerank in {AUTO_RERANK, "", None} else rerank
    rewritten_query = rewrite.rewritten_question if use_rewrite else question
    has_drug_constraint = bool(drug_name or generic_name)
    has_directory_constraint = has_drug_constraint or bool(category)
    need_clarification = False
    clarification_question = ""
    route_type = "answerable"

    if intent in DRUG_REQUIRED_INTENTS and not has_drug_constraint and not category:
        strategy = "clarification"
        default_candidate_k = 0
        use_metadata = False
        use_summary = False
        use_vector = False
        need_clarification = True
        route_type = "missing_drug"
        clarification_question = "请先确认具体药品名称或通用名；不同药品的用法、禁忌、特殊人群和相互作用不能混用说明书结论。"
        reason = (
            f"问题命中 {intent} 意图，但没有识别到具体药品或分类约束；"
            "该场景不能把多个药品说明书结论混用，应先追问药品名称。"
        )
    elif intent == "purchase_guidance" and not has_drug_constraint:
        strategy = "directory_search"
        default_candidate_k = max(final_k * 2, 10)
        use_metadata = bool(category)
        use_summary = True
        use_vector = True
        need_clarification = True
        route_type = "directory_search"
        clarification_question = "请补充想咨询的具体药品名称，或说明已诊断疾病、年龄、孕哺情况、正在使用的药物，系统才能按说明书逐一核对。"
        reason = (
            "问题属于购药指导，但未限定具体药品；先做目录/类别导向检索并提示用户补充具体药品、症状和用药背景，"
            "不直接给出某个药品的剂量或禁忌结论。"
        )
    elif intent == "general_qa" and not has_directory_constraint:
        if _looks_like_directory_query(question):
            strategy = "directory_search"
            default_candidate_k = max(final_k * 2, 10)
            use_metadata = False
            use_summary = True
            use_vector = True
            need_clarification = True
            route_type = "directory_search"
            clarification_question = "请补充具体药品名称或更明确的用药场景；药品说明书 RAG 只能依据已入库说明书回答，不能直接替代医生或药师推荐用药。"
            reason = (
                "问题没有命中明确说明书章节，但语义更接近购药/类别咨询；"
                "先走目录导向检索和追问，而不是直接泛化回答。"
            )
        else:
            strategy = "clarification"
            default_candidate_k = 0
            use_metadata = False
            use_summary = False
            use_vector = False
            need_clarification = True
            route_type = "ambiguous_question"
            clarification_question = "请补充具体药品名称，以及想查询的是用法用量、适应症、禁忌、不良反应、贮藏方式还是特殊人群用药。"
            reason = (
                "问题没有命中明确说明书章节，也没有药品或分类约束；"
                "企业场景下不应直接进入泛问检索，应先追问以明确药品和问题类型。"
            )
    elif intent in FIXED_SECTION_INTENTS:
        strategy = "metadata_first"
        default_candidate_k = max(final_k + 2, 8)
        use_metadata = True
        use_summary = False
        use_vector = True
        reason = (
            f"问题命中 {intent} 意图，答案通常位于固定说明书章节；"
            "优先使用章节与药品元数据过滤，命中不足时再用向量检索兜底。"
        )
    elif intent in SAFETY_INTENTS:
        strategy = "safety_hybrid"
        default_candidate_k = max(final_k * 2, 12)
        use_metadata = True
        use_summary = True
        use_vector = True
        reason = (
            f"问题命中 {intent} 高风险或购药决策意图，单一路径容易漏掉禁忌、注意事项或特殊人群章节；"
            "采用 metadata + summary + vector 混合召回，并对意图章节加权重排。"
        )
    elif metadata_filter:
        strategy = "hybrid"
        default_candidate_k = max(final_k * 2, 10)
        use_metadata = True
        use_summary = True
        use_vector = True
        reason = "问题已有药品或分类约束，但意图不够明确；采用混合检索兼顾精准过滤和语义召回。"
    else:
        strategy = "summary_guided_hybrid"
        default_candidate_k = max(final_k * 2, 10)
        use_metadata = False
        use_summary = True
        use_vector = True
        reason = "问题未命中明确章节，先用摘要索引定位可能相关章节，再回到原文 chunk 取证并用向量检索兜底。"

    return rewrite, RetrievalDecision(
        intent=intent,
        strategy=strategy,
        metadata_filter=metadata_filter,
        rewritten_query=rewritten_query,
        candidate_k=candidate_k or default_candidate_k,
        final_k=final_k,
        use_metadata=use_metadata,
        use_summary=use_summary,
        use_vector=use_vector,
        rerank=chosen_rerank,
        reason=reason,
        need_clarification=need_clarification,
        clarification_question=clarification_question,
        route_type=route_type,
    )


def _looks_like_directory_query(question: str) -> bool:
    normalized = question.strip().lower()
    return any(keyword in normalized for keyword in DIRECTORY_QUERY_HINTS)
