@echo off
chcp 65001 >nul
title ShipInsight Flyway 数据库迁移

echo ============================================
echo   ShipInsight AI - Flyway 数据库迁移工具
echo ============================================
echo.
echo   * 全新安装（空数据库）: 从 V1 开始顺序建表
echo   * 已有数据库: 只执行未跑过的新迁移文件
echo.
echo   数据库: localhost:3306/gsmv
echo ============================================
echo.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

cd /d "%PROJECT_DIR%"

echo [1/2] 检测项目中 mvnw.cmd ...
if not exist "mvnw.cmd" (
    echo [错误] 未找到 mvnw.cmd，请确保在项目根目录运行此脚本。
    pause
    exit /b 1
)
echo [√] mvnw.cmd 已找到

echo.
echo [2/2] 正在执行 Flyway 迁移 ...
echo       首次运行或文件较多时可能需要几十秒，请耐心等待。
echo.

call mvnw.cmd flyway:migrate -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================
    echo   [失败] Flyway 迁移执行出错！
    echo
    echo   常见原因:
    echo   1. MySQL 服务未启动
    echo   2. gsmv 数据库不存在
    echo   3. 数据库连接信息不对（端口/用户名/密码）
    echo   请检查上方错误信息后重试
    echo ============================================
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ============================================
echo   [成功] Flyway 数据库迁移执行完毕！
echo ============================================
echo.
pause
