from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Protocol


class KnowledgeDomain(Protocol):
    name: str

    def describe(self) -> dict[str, Any]:
        ...


@dataclass(frozen=True, slots=True)
class RegisteredDomain:
    name: str
    display_name: str
    description: str

    def describe(self) -> dict[str, str]:
        return {
            "name": self.name,
            "display_name": self.display_name,
            "description": self.description,
        }
