@echo off
setlocal enabledelayedexpansion

REM 统一版本号管理
set LIBRARY_VERSION=1.0.0
set LIBRARY_GROUP_ID=com.cn.core

REM 发布所有core模块到local_maven
echo 开始发布所有core模块到local_maven...
echo 版本: %LIBRARY_VERSION%
echo Group ID: %LIBRARY_GROUP_ID%
echo.

REM 定义需要发布的core模块列表
set MODULES_TO_PUBLISH=":core:core-resources" ":core:core-ui" ":core:core-utils"

REM 执行每个模块的发布命令
for %%m in (%MODULES_TO_PUBLISH%) do (
    echo 发布模块: %%m
    echo --------------------
    call gradlew %%m:publishReleasePublicationToMavenRepository --no-build-cache
    echo --------------------
    echo.
)

echo 所有core模块发布完成！
echo 发布路径: %CD%\build\maven-repo
echo.
pause
