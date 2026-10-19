from __future__ import annotations

import json
from typing import Any

from drug_rag.llms import OpenAICompatibleChat, build_chat_model
from drug_rag.retrieval.intent_router import QueryRoute, route_question


INTENT_CATEGORIES = [
    "indication",
    "dosage",
    "adverse_reaction",
    "contraindication",
    "interaction",
    "special_population",
    "storage",
    "purchase_guidance",
    "general_qa",
]

SYSTEM_PROMPT = """你是一个药品说明书意图分类专家。请分析用户问题，将其分类到最合适的意图类别。

可用的意图类别：
- indication: 适应症、治疗疾病
- dosage: 用法用量、剂量、服用方法
- adverse_reaction: 不良反应、副作用
- contraindication: 禁忌、慎用、不能使用的情况
- interaction: 药物相互作用、合并用药
- special_population: 特殊人群（孕妇、儿童、老人、肝肾功能异常等）
- storage: 贮藏、保存条件、有效期
- purchase_guidance: 购药指导、用药建议
- general_qa: 通用问答、其他问题

请以 JSON 格式返回，包含以下字段：
{
  "intent": "意图类别",
  "confidence": 0.0-1.0的置信度,
  "reasoning": "简短的分类理由"
}

只返回 JSON，不要有其他内容。"""

USER_PROMPT_TEMPLATE = """用户问题：{question}

规则路由结果：
- 初始意图：{rule_intent}
- 匹配章节：{rule_sections}
- 匹配关键词：{rule_keywords}

请结合规则路由结果，给出最终的意图分类。"""


def route_question_with_llm(
    question: str,
    rule_route: QueryRoute,
    provider: str = "dashscope_compatible",
    model: str | None = None,
    base_url: str | None = None,
    api_key_env: str = "DASHSCOPE_API_KEY",
    temperature: float = 0.0,
) -> QueryRoute:
    """使用 LLM 对问题进行意图分类，增强规则路由的结果。

    Args:
        question: 用户问题
        rule_route: 规则路由的初步结果
        provider: LLM 提供商
        model: 模型名称
        base_url: API 基础 URL
        api_key_env: API 密钥环境变量名
        temperature: 生成温度

    Returns:
        更新后的 QueryRoute，包含 LLM 分类的意图
    """
    chat_model = build_chat_model(
        provider=provider,
        model=model,
        base_url=base_url,
        api_key_env=api_key_env,
        temperature=temperature,
    )

    if chat_model is None:
        return rule_route

    try:
        user_prompt = USER_PROMPT_TEMPLATE.format(
            question=question,
            rule_intent=rule_route.intent,
            rule_sections=", ".join(rule_route.sections),
            rule_keywords=", ".join(rule_route.matched_keywords),
        )

        messages = [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ]

        response_text = chat_model.invoke(messages)

        llm_result = _parse_llm_response(response_text)

        if llm_result and llm_result.get("intent") in INTENT_CATEGORIES:
            llm_intent = llm_result["intent"]
            llm_confidence = llm_result.get("confidence", 0.8)

            from drug_rag.retrieval.intent_router import INTENT_RULES

            intent_rule = next(
                (rule for rule in INTENT_RULES if rule.intent == llm_intent), None
            )

            if intent_rule:
                sections = intent_rule.sections
                if llm_intent == "special_population":
                    sections = _special_population_sections_from_llm(question)

                from drug_rag.retrieval.intent_router import _section_filter

                return QueryRoute(
                    intent=llm_intent,
                    sections=sections,
                    metadata_filter=_section_filter(sections),
                    matched_keywords=rule_route.matched_keywords,
                    confidence=llm_confidence,
                    scores={**rule_route.scores, "llm": llm_confidence},
                    source="llm",
                )

    except Exception as e:
        print(f"LLM 意图分类失败，回退到规则路由: {e}")

    return rule_route


def _parse_llm_response(response_text: str) -> dict[str, Any] | None:
    """解析 LLM 返回的 JSON 响应。"""
    response_text = response_text.strip()

    try:
        result = json.loads(response_text)
        if isinstance(result, dict):
            return result
    except json.JSONDecodeError:
        pass

    import re

    json_match = re.search(r"\{[^}]+\}", response_text, re.DOTALL)
    if json_match:
        try:
            result = json.loads(json_match.group())
            if isinstance(result, dict):
                return result
        except json.JSONDecodeError:
            pass

    return None


def _special_population_sections_from_llm(question: str) -> list[str]:
    """根据问题内容推断特殊人群相关章节。"""
    sections: list[str] = []

    if any(keyword in question for keyword in ["孕妇", "妊娠", "怀孕", "备孕"]):
        sections.extend(["孕妇及哺乳期妇女用药", "禁忌", "注意事项"])
    if any(keyword in question for keyword in ["哺乳", "母乳", "喂奶"]):
        sections.extend(["孕妇及哺乳期妇女用药", "注意事项"])
    if any(keyword in question for keyword in ["儿童", "小孩", "孩子", "婴儿", "宝宝", "未成年"]):
        sections.extend(["儿童用药", "注意事项"])
    if any(keyword in question for keyword in ["老人", "老年", "高龄"]):
        sections.extend(["老年用药", "老年患者用药", "注意事项"])
    if any(keyword in question for keyword in ["肝功能", "肾功能", "肝损伤", "肾损伤"]):
        sections.extend(["注意事项", "禁忌"])

    return list(dict.fromkeys(sections)) or ["注意事项"]
