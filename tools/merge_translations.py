#!/usr/bin/env python3
"""
合并各模块 translations/*.json → board/proxy/assets/translations/*.json

用法:
    python tools/merge_translations.py

工作流程:
    1. 遍历所有子模块的 translations/ 目录
    2. 按语言标签聚合所有模块的翻译条目
    3. 合并输出到 board/proxy/src/main/assets/translations/
    4. 同 Key 冲突时后处理的模块覆盖前者
"""
import json
import os
import shutil
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = ROOT / "board" / "proxy" / "src" / "main" / "assets" / "translations"

# 需要合并的语言
LANGS = ["en", "ru", "pt"]


def collect_and_merge():
    merged = {lang: {} for lang in LANGS}

    # 遍历所有子目录中的 translations/
    for module_root, dirs, files in os.walk(ROOT):
        if os.path.basename(module_root) == "translations":
            for lang in LANGS:
                json_file = os.path.join(module_root, f"{lang}.json")
                if os.path.exists(json_file):
                    try:
                        with open(json_file, "r", encoding="utf-8") as f:
                            data = json.load(f)
                        # 后处理的模块覆盖前面的（通常是子模块覆盖基础模块）
                        merged[lang].update(data)
                    except Exception as e:
                        print(f"  [WARN] {json_file}: {e}")

    # 写入输出目录
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for lang, strings in merged.items():
        if not strings:
            print(f"  [SKIP] {lang}: 无翻译条目")
            continue
        output_file = OUTPUT_DIR / f"{lang}.json"
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(strings, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"  [OK] {lang}.json — {len(strings)} 条翻译")


if __name__ == "__main__":
    print("合并各模块翻译 JSON...")
    collect_and_merge()
    print(f"\n输出目录: {OUTPUT_DIR}")
    print("完成！")
