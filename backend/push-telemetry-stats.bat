@echo off
title TGZ Lifesteal - Push Player Stats Webhook
echo ========================================================
echo   TGZ LIFESTEAL - STREAM SMP TELEMETRY TO BACKEND
echo   Target: http://127.0.0.1:3000/api/minecraft/report-stats
echo ========================================================
echo.
echo Pushing authoritative player stats to Leaderboard...
powershell -NoProfile -Command "$body = @{ players = @( @{ uuid = '069a79f4-44e9-4726-a5be-fca90e38aaf5'; username = 'sg_karthi'; playtime_seconds = 86400; kills = 28; deaths = 4; hearts = 18; coins = 5400 }, @{ uuid = '853c80ef-3c37-49fd-aa49-938b674adae6'; username = 'Viper_X'; playtime_seconds = 54000; kills = 19; deaths = 9; hearts = 14; coins = 3100 }, @{ uuid = 'c4c8df63-3069-42b4-ae49-d34e622dfb42'; username = 'ShadowNinja'; playtime_seconds = 36000; kills = 12; deaths = 11; hearts = 10; coins = 1800 } ) } | ConvertTo-Json -Depth 4; $headers = @{ 'X-Plugin-Key' = 'tgz_plugin_sec_99a8b7c6d5e4f3a2'; 'Content-Type' = 'application/json' }; $resp = Invoke-RestMethod -Uri 'http://127.0.0.1:3000/api/minecraft/report-stats' -Method Post -Body $body -Headers $headers; Write-Host 'Success! Telemetry processed: ' $resp.processedCount ' players.'"
echo.
echo Done! Open or refresh the Leaderboard tab in your TGZ APK to see live rankings.
pause
