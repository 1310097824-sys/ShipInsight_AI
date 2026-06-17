@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0start-clickhouse.ps1" %*
