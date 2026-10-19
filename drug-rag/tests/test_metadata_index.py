from drug_rag.indexing.metadata_index import resolve_query_constraints


def test_resolve_drug_alias_without_form_suffix():
    directory = {
        "categories": [],
        "drugs": [
            {
                "doc_id": "candesartan",
                "drug_name": "必洛斯",
                "generic_name": "坎地沙坦酯片",
                "source_file": "必洛斯_坎地沙坦酯片__说明书.pdf",
                "category": "药品说明书",
            }
        ],
    }

    for question in ("坎地沙坦片有哪些常见不良反应？", "坎地沙坦酯有哪些不良反应？"):
        result = resolve_query_constraints(directory, question)
        assert result.generic_name == "坎地沙坦酯片"
        assert result.ambiguous is False


def test_resolve_drug_alias_without_capsule_or_tablet_form():
    directory = {
        "categories": [],
        "drugs": [
            {"doc_id": "omeprazole", "drug_name": "奥美拉唑肠溶胶囊", "generic_name": "奥美拉唑肠溶胶囊", "source_file": "奥美拉唑肠溶胶囊.pdf", "category": "药品说明书"},
            {"doc_id": "irbesartan", "drug_name": "安博维", "generic_name": "厄贝沙坦片", "source_file": "厄贝沙坦片.pdf", "category": "药品说明书"},
        ],
    }

    assert resolve_query_constraints(directory, "奥美拉唑有哪些禁忌？").generic_name == "奥美拉唑肠溶胶囊"
    assert resolve_query_constraints(directory, "厄贝沙坦有哪些常见不良反应？").generic_name == "厄贝沙坦片"
