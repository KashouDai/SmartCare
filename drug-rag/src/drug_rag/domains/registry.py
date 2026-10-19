from __future__ import annotations

from drug_rag.domains.base import RegisteredDomain


class UnknownKnowledgeDomainError(ValueError):
    """请求了尚未注册的知识域。"""


class DomainRegistry:
    def __init__(self, domains: list[RegisteredDomain] | None = None) -> None:
        self._domains = {domain.name: domain for domain in domains or []}

    def register(self, domain: RegisteredDomain) -> None:
        if domain.name in self._domains:
            raise ValueError(f"Knowledge domain already registered: {domain.name}")
        self._domains[domain.name] = domain

    def get(self, name: str) -> RegisteredDomain:
        try:
            return self._domains[name]
        except KeyError as exc:
            raise UnknownKnowledgeDomainError(f"Unknown knowledge domain: {name}") from exc

    def names(self) -> tuple[str, ...]:
        return tuple(self._domains)

    def describe(self) -> list[dict[str, str]]:
        return [self._domains[name].describe() for name in self._domains]
