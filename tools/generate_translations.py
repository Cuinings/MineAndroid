#!/usr/bin/env python3
"""
JSON → Android strings.xml 翻译生成器

用法:
    python tools/generate_translations.py <模块目录>

示例:
    python tools/generate_translations.py board/module/home

工作流程:
    1. 读取 {模块目录}/translations/ 下所有 JSON 文件
    2. 对每个语言 (如 zh-CN.json, en.json, ru.json, pt.json):
       - zh-CN → 生成到 res/values/strings.xml（默认语言）
       - en    → 生成到 res/values-en/strings.xml
       - ru    → 生成到 res/values-ru/strings.xml
       - pt    → 生成到 res/values-pt/strings.xml
    3. JSON 格式:
       {
           "str_key": "翻译文本",
           "str_key_2": "翻译文本2"
       }

目录结构约定:
    模块目录/
    ├── translations/          ← 翻译人员编辑这里（加入 Git）
    │   ├── zh-CN.json         ← 中文源（默认语言的基础翻译）
    │   ├── en.json
    │   ├── ru.json
    │   └── pt.json
    └── src/main/res/
        ├── values/strings.xml         ← 自动生成（.gitignore）
        ├── values-en/strings.xml      ← 自动生成（.gitignore）
        ├── values-ru/strings.xml      ← 自动生成（.gitignore）
        └── values-pt/strings.xml      ← 自动生成（.gitignore）
"""

import sys
import os
import json
import re
from pathlib import Path

# 语言标签 → Android values 目录后缀映射
LANG_TO_VALUES_SUFFIX = {
    "zh-CN": "",           # 默认语言 → values/
    "en":    "en",
    "ru":    "ru",
    "pt":    "pt",
}

# Android XML 需要转义的字符
XML_ESCAPES = [
    ("&", "&amp;"),
    ("<", "&lt;"),
    (">", "&gt;"),
    ('"', "\\\""),
    ("'", "\\'"),
]


def escape_xml(text: str) -> str:
    """对 XML 特殊字符进行转义，但保留 Android 占位符 (%s, %d, %1$s 等)"""
    for char, escaped in XML_ESCAPES:
        text = text.replace(char, escaped)
    return text


def load_json(json_path: str) -> dict[str, str]:
    """读取翻译 JSON 文件，返回 {key: value} 字典"""
    if not os.path.exists(json_path):
        print(f"  [WARN] 文件不存在: {json_path}")
        return {}
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, dict):
        print(f"  [ERROR] JSON 根元素必须是对象: {json_path}")
        return {}
    return data


def generate_xml(strings: dict[str, str]) -> str:
    """根据字典生成 strings.xml 内容"""
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for key, value in sorted(strings.items()):
        escaped = escape_xml(str(value))
        lines.append(f'    <string name="{key}">{escaped}</string>')
    lines.append("</resources>")
    lines.append("")  # 末尾换行
    return "\n".join(lines)


def process_module(module_dir: str):
    """处理单个模块：读取 JSON → 生成 XML"""
    module_path = Path(module_dir)
    translations_dir = module_path / "translations"
    res_dir = module_path / "src" / "main" / "res"

    if not translations_dir.is_dir():
        print(f"  [SKIP] 没有 translations/ 目录: {translations_dir}")
        return

    print(f"处理模块: {module_dir}")

    generated_count = 0
    for lang_tag, values_suffix in LANG_TO_VALUES_SUFFIX.items():
        json_file = translations_dir / f"{lang_tag}.json"
        strings = load_json(str(json_file))

        if not strings:
            continue

        # 确定输出目录和文件
        if values_suffix:
            output_dir = res_dir / f"values-{values_suffix}"
        else:
            output_dir = res_dir / "values"

        output_file = output_dir / "strings.xml"

        # 确保输出目录存在
        output_dir.mkdir(parents=True, exist_ok=True)

        xml_content = generate_xml(strings)

        # 检查是否需要更新（避免无意义的文件变更）
        if output_file.exists():
            old_content = output_file.read_text(encoding="utf-8")
            if old_content.strip() == xml_content.strip():
                continue  # 内容未变，跳过

        output_file.write_text(xml_content, encoding="utf-8")
        generated_count += 1
        print(f"  [OK] {lang_tag} → {output_file.relative_to(module_path)}")

    if generated_count == 0:
        print("  [INFO] 所有翻译 XML 均无变化，无需更新")
    else:
        print(f"  [DONE] 共生成/更新 {generated_count} 个文件")


def main():
    if len(sys.argv) < 2:
        print("用法: python tools/generate_translations.py <模块目录> [模块目录2 ...]")
        print("示例: python tools/generate_translations.py board/module/home")
        sys.exit(1)

    for module_dir in sys.argv[1:]:
        process_module(module_dir)

    print("\n全部完成！")


if __name__ == "__main__":
    main()
