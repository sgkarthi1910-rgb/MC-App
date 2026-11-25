@echo off
title TGZ Lifesteal - Backend Service
echo ========================================================
echo   TGZ LIFESTEAL BACKEND SERVICE
echo   Minecraft SMP: node2.vyroxsmp.online:10010
echo   Admin ID: sg_karthi
echo   Initial Password: Set in .env (Default: TgzAdminInitial2026!Secure)
echo ========================================================
cd /d "%~dp0"
node src/server.js
pause
