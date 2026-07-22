@echo off

rem 构建和发布core目录下所有工程的脚本
echo ==============================
echo 开始构建和发布core目录下的所有工程
echo ==============================

rem 进入项目根目录
cd /d "%~dp0"

rem 清理之前的构建
echo 清理之前的构建...
call gradlew clean -x test

if %errorlevel% neq 0 (
    echo 清理构建失败！
    pause
    exit /b %errorlevel%
)

echo 清理完成！

rem 构建和发布core-resources
echo 构建和发布 core-resources...
call gradlew :core:core-resources:assembleRelease :core:core-resources:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-resources 失败！
    pause
    exit /b %errorlevel%
)

echo core-resources 构建和发布成功！

rem 构建和发布core-utils
echo 构建和发布 core-utils...
call gradlew :core:core-utils:assembleRelease :core:core-utils:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-utils 失败！
    pause
    exit /b %errorlevel%
)

echo core-utils 构建和发布成功！

rem 构建和发布core-ui
echo 构建和发布 core-ui...
call gradlew :core:core-ui:assembleRelease :core:core-ui:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-ui 失败！
    pause
    exit /b %errorlevel%
)

echo core-ui 构建和发布成功！

rem 构建和发布core-remote下的项目

rem 构建和发布andlinker
echo 构建和发布 core-remote:andlinker...
call gradlew :core:core-remote:andlinker:assembleRelease :core:core-remote:andlinker:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-remote:andlinker 失败！
    pause
    exit /b %errorlevel%
)

echo core-remote:andlinker 构建和发布成功！

rem 构建和发布msg-subscriber-annotation
echo 构建和发布 core-remote:msg-subscriber-annotation...
call gradlew :core:core-remote:msg-subscriber-annotation:assemble :core:core-remote:msg-subscriber-annotation:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-remote:msg-subscriber-annotation 失败！
    pause
    exit /b %errorlevel%
)

echo core-remote:msg-subscriber-annotation 构建和发布成功！

rem 构建和发布msg-subscriber-processor
echo 构建和发布 core-remote:msg-subscriber-processor...
call gradlew :core:core-remote:msg-subscriber-processor:assemble :core:core-remote:msg-subscriber-processor:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-remote:msg-subscriber-processor 失败！
    pause
    exit /b %errorlevel%
)

echo core-remote:msg-subscriber-processor 构建和发布成功！

rem 构建和发布msg-router-client
echo 构建和发布 core-remote:msg-router-client...
call gradlew :core:core-remote:msg-router-client:assembleRelease :core:core-remote:msg-router-client:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-remote:msg-router-client 失败！
    pause
    exit /b %errorlevel%
)

echo core-remote:msg-router-client 构建和发布成功！

rem 构建和发布msg-router-service
echo 构建和发布 core-remote:msg-router-service...
call gradlew :core:core-remote:msg-router-service:assembleRelease :core:core-remote:msg-router-service:publish

if %errorlevel% neq 0 (
    echo 构建或发布 core-remote:msg-router-service 失败！
    pause
    exit /b %errorlevel%
)

echo core-remote:msg-router-service 构建和发布成功！

echo ==============================
echo 所有core目录下的工程构建和发布完成！
echo 发布目录：%~dp0maven-repo
echo ==============================

pause
