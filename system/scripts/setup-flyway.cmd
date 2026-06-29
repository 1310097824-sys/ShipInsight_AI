@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title ShipInsight Flyway 数据库迁移

set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_NAME=gsmv"
set "DB_USER=root"
set "DB_PASSWORD=123456"
set "FLYWAY_URL=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
set "FLYWAY_LOG=%TEMP%\shipinsight_flyway_%RANDOM%_%RANDOM%.log"

echo.
echo ============================================
echo   ShipInsight AI - Flyway 数据库迁移工具
echo ============================================
echo.
echo   数据库: %DB_HOST%:%DB_PORT%/%DB_NAME%  (%DB_USER%/%DB_PASSWORD%)
echo   迁移文件: src\main\resources\db\migration\
echo.
echo   * 空 MySQL 且无 gsmv 库: 自动创建 gsmv 后从 V1 建库
echo   * 已有 gsmv 且有 Flyway 历史: 只补跑未执行的新迁移
echo   * 已有表但无 Flyway 历史: 自动清空 schema 后重建
echo ============================================
echo.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

cd /d "%PROJECT_DIR%"
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 无法进入项目目录: %PROJECT_DIR%
    pause
    exit /b 1
)

echo   当前目录: %cd%
echo.

if not exist "mvnw.cmd" (
    echo [错误] 未找到 mvnw.cmd，请确认脚本位于 system\scripts 目录下。
    pause
    exit /b 1
)

:: =============================================
:: Step 1 - 检测 Java
:: =============================================
echo [1/3] 检测 Java 运行环境 ...

set "JAVA_EXE="
set "JAVA_HOME_DETECTED="

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        set "JAVA_HOME_DETECTED=%JAVA_HOME%"
    )
)

if not defined JAVA_EXE (
    for /f "delims=" %%i in ('where java 2^>nul') do (
        set "JAVA_EXE=%%i"
        goto :java_found
    )
)

:java_found
if not defined JAVA_EXE (
    for %%B in ("C:\Program Files\Java" "C:\Program Files\Eclipse Adoptium" "D:\Java") do (
        if exist "%%~B" (
            for /d %%J in ("%%~B\jdk-*") do (
                if exist "%%J\bin\java.exe" (
                    set "JAVA_EXE=%%J\bin\java.exe"
                    set "JAVA_HOME_DETECTED=%%J"
                    goto :java_ok
                )
            )
        )
    )
)

:java_ok
if not defined JAVA_EXE (
    echo.
    echo [错误] 未找到 Java 运行环境！
    echo.
    echo   请安装 JDK 17 或更高版本:
    echo     * https://adoptium.net/         ^(推荐^)
    echo     * https://www.oracle.com/java/
    echo.
    echo   安装后请设置 JAVA_HOME 环境变量，或确保 java 在 PATH 中。
    echo.
    pause
    exit /b 1
)

"%JAVA_EXE%" -version 2>&1 | findstr /i "version"
echo [√] Java 环境就绪

:: =============================================
:: Step 2 - 检测 MySQL
:: =============================================
echo.
echo [2/3] 检测 MySQL 连接 (%DB_HOST%:%DB_PORT%) ...

powershell -NoProfile -Command "try { (New-Object Net.Sockets.TcpClient('%DB_HOST%', %DB_PORT%)).Close(); exit 0 } catch { exit 1 }"
if not errorlevel 1 (
    echo [√] MySQL 端口 %DB_PORT% 已开放
    goto :mysql_check_done
)

echo.
echo [警告] 无法连接 MySQL (%DB_HOST%:%DB_PORT%)
echo.
echo   请先启动 MySQL 服务:
echo     net start MySQL80
echo     ^(或通过“服务”管理器启动 MySQL 服务^)
echo.
choice /C YN /M "是否忽略并继续尝试迁移"
if errorlevel 2 exit /b 1

:mysql_check_done

:: =============================================
:: Step 3 - 执行迁移
:: =============================================
echo.
echo [3/3] 正在执行 Flyway 迁移 ...
echo       首次运行需下载 Maven 及插件依赖，请耐心等待...
echo.

if not defined JAVA_HOME (
    if defined JAVA_HOME_DETECTED (
        set "JAVA_HOME=%JAVA_HOME_DETECTED%"
    )
)

call :run_migrate
set "MIGRATE_EXIT=%ERRORLEVEL%"

if "%MIGRATE_EXIT%"=="0" (
    goto :success
)

set "NEEDS_CLEAN=0"
findstr /I /C:"no schema history table" "%FLYWAY_LOG%" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    findstr /I /C:"non-empty schema" "%FLYWAY_LOG%" >nul 2>nul
    if !ERRORLEVEL! EQU 0 set "NEEDS_CLEAN=1"
)

if "!NEEDS_CLEAN!"=="1" (
    echo.
    echo ============================================
    echo   检测到已有表但没有 Flyway 迁移历史
    echo ============================================
    echo.
    echo   将清空 %DB_NAME% schema 中的已有对象，并按迁移脚本重建数据库。
    echo   该分支只会在 Flyway 报告“非空库且无历史表”时触发。
    echo.

    call :run_clean_and_migrate
    if !ERRORLEVEL! EQU 0 (
        goto :success
    )
)

echo.
echo ============================================
echo   [失败] Flyway 迁移执行出错！
echo ============================================
echo.
echo   常见原因:
echo   1. MySQL 服务未启动或端口不正确
echo   2. root/123456 无法连接或没有建库/删表权限
echo   3. 无法连接 Maven 中央仓库，请检查网络或 Maven 镜像
echo   4. 迁移文件有冲突，请查看上方 Flyway 报错详情
echo.
del "%FLYWAY_LOG%" 2>nul
pause
exit /b 1

:success
echo.
echo ============================================
echo   [成功] Flyway 数据库迁移执行完毕！
echo ============================================
echo.
echo   所有待执行的迁移文件已应用完成，可启动应用服务器。
echo.
del "%FLYWAY_LOG%" 2>nul
pause
exit /b 0

:run_migrate
call mvnw.cmd flyway:migrate -DskipTests "-Dflyway.url=%FLYWAY_URL%" "-Dflyway.user=%DB_USER%" "-Dflyway.password=%DB_PASSWORD%" > "%FLYWAY_LOG%" 2>&1
set "FLYWAY_EXIT=%ERRORLEVEL%"
type "%FLYWAY_LOG%"
exit /b %FLYWAY_EXIT%

:run_clean_and_migrate
call mvnw.cmd flyway:clean flyway:migrate -DskipTests "-Dflyway.cleanDisabled=false" "-Dflyway.url=%FLYWAY_URL%" "-Dflyway.user=%DB_USER%" "-Dflyway.password=%DB_PASSWORD%" > "%FLYWAY_LOG%" 2>&1
set "FLYWAY_EXIT=%ERRORLEVEL%"
type "%FLYWAY_LOG%"
exit /b %FLYWAY_EXIT%
