from drug_rag.domains.base import RegisteredDomain
from drug_rag.domains.registry import DomainRegistry, UnknownKnowledgeDomainError


def build_default_registry() -> DomainRegistry:
    return DomainRegistry(
        [
            RegisteredDomain(
                name="drug",
                display_name="药品说明书",
                description="药品说明书、用法用量、禁忌和不良反应等资料。",
            )
        ]
    )


__all__ = [
    "DomainRegistry",
    "RegisteredDomain",
    "UnknownKnowledgeDomainError",
    "build_default_registry",
]
