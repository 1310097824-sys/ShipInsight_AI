@echo off
setlocal EnableExtensions

set "GSMV_ROOT=%~dp0"
if "%GSMV_ROOT:~-1%"=="\" set "GSMV_ROOT=%GSMV_ROOT:~0,-1%"


set "_BAILIAN_BAK=%BAILIAN_API_KEY%"
set "_DASHSCOPE_BAK=%DASHSCOPE_API_KEY%"
set "_DEEPSEEK_BAK=%DEEPSEEK_API_KEY%"
set "_BAIDU_BAK=%BAIDU_MAP_AK%"

set "GSMV_KEYS_B64="

if /I not "%GSMV_SKIP_API_PROMPT%"=="1" (
  for /f "usebackq delims=" %%K in (`powershell.exe -STA -NoProfile -ExecutionPolicy Bypass -File "%GSMV_ROOT%\scripts\gsmv-api-key-prompt.ps1"`) do set "GSMV_KEYS_B64=%%K"

  if defined GSMV_KEYS_B64 (
    for /f "usebackq tokens=1,* delims==" %%A in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$json=[System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($env:GSMV_KEYS_B64)); $keys=$json | ConvertFrom-Json; 'BAILIAN_API_KEY=' + [string]$keys.bailian; 'DASHSCOPE_API_KEY=' + [string]$keys.dashscope; 'DEEPSEEK_API_KEY=' + [string]$keys.deepseek; 'BAIDU_MAP_AK=' + [string]$keys.baidu_map"`) do set "%%A=%%B"
  ) else (
    echo API key dialog did not return values. Falling back to console input.
    set /p "BAILIAN_API_KEY=Enter Bailian API Key (leave blank allowed): "
    set /p "DEEPSEEK_API_KEY=Enter DeepSeek API Key (leave blank allowed): "
    set "DASHSCOPE_API_KEY=%BAILIAN_API_KEY%"
    set /p "BAIDU_MAP_AK=Enter Baidu Map AK (leave blank to keep system env): "
  )
)

if "%BAILIAN_API_KEY%"=="" set "BAILIAN_API_KEY=%_BAILIAN_BAK%"
if "%DASHSCOPE_API_KEY%"=="" set "DASHSCOPE_API_KEY=%_DASHSCOPE_BAK%"
if "%DEEPSEEK_API_KEY%"=="" set "DEEPSEEK_API_KEY=%_DEEPSEEK_BAK%"
if "%BAIDU_MAP_AK%"=="" set "BAIDU_MAP_AK=%_BAIDU_BAK%"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%GSMV_ROOT%\scripts\start-gsmv.ps1" -Root "%GSMV_ROOT%" -BailianApiKey "%BAILIAN_API_KEY%" -DeepSeekApiKey "%DEEPSEEK_API_KEY%" -BaiduMapAk "%BAIDU_MAP_AK%"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
  echo.
  echo GSMV start failed. Please check the error message above.
  pause >nul
  exit /b %EXIT_CODE%
)

echo.
echo Press any key to close this launcher window. The system keeps running in the background.
pause >nul