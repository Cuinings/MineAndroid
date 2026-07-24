#!/usr/bin/env python3
"""
Phase 1 migration script: Move source files from home aggregator to sub-modules.
Run from project root: python migrate_phase1.py
"""

import shutil
import os

BASE = r"C:\Users\Work\AndroidStudioProjects\MineAndroid"
HOME_SRC = os.path.join(BASE, "board", "module", "home", "src")
DATA_SRC = os.path.join(BASE, "board", "module", "home-data", "src")
PRES_SRC = os.path.join(BASE, "board", "module", "home-presentation", "src")

PKG = os.path.join("main", "java", "com", "cn", "board", "meet", "home")

def ensure_dir(path):
    os.makedirs(path, exist_ok=True)

def copy_tree(src_rel, dst_base):
    """Copy a relative source path from HOME_SRC into dst_base."""
    full_src = os.path.join(HOME_SRC, src_rel)
    full_dst = os.path.join(dst_base, src_rel)
    if os.path.exists(full_src):
        ensure_dir(os.path.dirname(full_dst))
        shutil.copytree(full_src, full_dst, dirs_exist_ok=True)
        print(f"  OK: {src_rel}")
    else:
        print(f"  SKIP (not found): {src_rel}")

print("=" * 60)
print("Phase 1: Migrating Home module sources to sub-modules")
print("=" * 60)

# ---- Step 1: Copy home src to home-presentation (full copy) ----
print("\n[1/3] Copying all sources to home-presentation...")
copy_tree("main", PRES_SRC)
copy_tree("test", PRES_SRC)
copy_tree("androidTest", PRES_SRC)

# Also copy resources
res_src = os.path.join(HOME_SRC, "main", "res")
res_dst = os.path.join(PRES_SRC, "main", "res")
if os.path.exists(res_src):
    shutil.copytree(res_src, res_dst, dirs_exist_ok=True)
    print("  OK: res/")

# ---- Step 2: data-layer files have been manually written to home-data ----
print("\n[2/3] Data-layer files already in home-data (AppInfoEntity, EmAppType, SoftEntity, SoftDiffCallBack, AppDataBase, AppInfoDao, EmAppTypeConverter, AppInfoRepository, AppUtil)")

# ---- Step 3: Remove moved files from presentation ----
print("\n[3/3] Removing data-layer duplicates from presentation...")
# Data-layer packages that should NOT be in presentation:
data_packages = ["room", "entity", "util"]
for pkg_name in data_packages:
    pkg_path = os.path.join(PRES_SRC, PKG, pkg_name)
    if os.path.exists(pkg_path):
        shutil.rmtree(pkg_path)
        print(f"  Removed: {pkg_name}/")

print("\n" + "=" * 60)
print("Phase 1 migration complete!")
print("=" * 60)
print("\nNext steps:")
print("  1. Delete or rename the original home/src/ directory")
print("  2. Run: ./gradlew :board:module:home-data:compileDebugKotlin")
print("  3. Run: ./gradlew :board:module:home-presentation:compileDebugKotlin")
print("  4. Run: ./gradlew :board:module:home:compileDebugKotlin")
