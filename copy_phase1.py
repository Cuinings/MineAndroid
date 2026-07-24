import shutil
import os

base = r"C:\Users\Work\AndroidStudioProjects\MineAndroid\board\module"
src_dir = os.path.join(base, "home", "src")
dest_dir = os.path.join(base, "home-presentation", "src")

print(f"Copying from {src_dir} to {dest_dir}")
if os.path.exists(src_dir):
    shutil.copytree(src_dir, dest_dir, dirs_exist_ok=True)
    print("Copy succeeded!")
else:
    print(f"Source directory does not exist: {src_dir}")
