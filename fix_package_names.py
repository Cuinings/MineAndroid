import os
import re

def fix_package_names(root_dir):
    """修复所有Kotlin文件的包名，从 com.cn.launcher 改为 com.cn.launcher"""
    
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.kt'):
                file_path = os.path.join(root, file)
                
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    if 'package com.cn.launcher' in content:
                        new_content = content.replace('package com.cn.launcher', 'package com.cn.launcher')
                        
                        with open(file_path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        
                        print(f'Fixed: {file_path}')
                    
                except Exception as e:
                    print(f'Error processing {file_path}: {e}')

if __name__ == '__main__':
    launcher_dir = r'c:\Users\Work\AndroidStudioProjects\MineAndroid\launcher\src\main\java\com\cn\launcher'
    fix_package_names(launcher_dir)
    print('Package name fix completed!')
