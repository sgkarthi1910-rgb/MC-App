@echo off
title TGZ Lifesteal - Backend Service
echo ========================================================
echo   TGZ LIFESTEAL BACKEND SERVICE
echo   Minecraft SMP: node2.vyroxsmp.online:10010
echo   Admin IDs: sg_karthi, dhanush, lmtg, darksight
echo   Initial Password: Set in .env (Default: TgzAdminInitial2026!Secure)
echo ========================================================
cd /d "%~dp0"
node src/server.js
pause
