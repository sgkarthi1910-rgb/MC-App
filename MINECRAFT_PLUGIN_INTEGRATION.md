# TGZ Lifesteal - Minecraft SMP Server Plugin Integration Guide

## Overview

The TGZ Lifesteal application and backend retrieve real-time server telemetry from the configured Minecraft Java SMP server:

- **Configured Server Address**: `node2.vyroxsmp.online:10010`
- **Native Telemetry**: Live status (Online/Offline), player count, latency (ms), MOTD, and online player sample are retrieved via direct native TCP Server List Ping (SLP) protocol.
- **Authoritative Player Statistics & Playtime**: An IP address alone cannot provide player UUIDs, historical playtime, kills, deaths, or heart counts. To feed the Playtime Rewards and SMP Leaderboard with 100% authoritative data, use the secure backend webhook integration described below.

---

## 1. Webhook Endpoint Specification

- **Method**: `POST`
- **URL**: `http://<your-backend-host>:3000/api/minecraft/report-stats`
- **Required Header**: `X-Plugin-Key: <your_configured_secret_key>` (configured via `MINECRAFT_PLUGIN_API_KEY` in `backend/.env`)
- **Content-Type**: `application/json`

### Payload Format (JSON):

```json
{
  "players": [
    {
      "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "username": "Notch",
      "playtime_seconds": 18000,
      "kills": 42,
      "deaths": 5,
      "hearts": 16,
      "coins": 1500
    },
    {
      "uuid": "853c80ef-3c37-49fd-aa49-938b674adae6",
      "username": "Jeb_",
      "playtime_seconds": 7200,
      "kills": 12,
      "deaths": 8,
      "hearts": 10,
      "coins": 300
    }
  ]
}
```

### Response (200 OK):

```json
{
  "success": true,
  "processedCount": 2,
  "timestamp": 1700000000000
}
```

---

## 2. Spigot / Paper Plugin Implementation Example (Java / Kotlin)

Add a scheduled task in your Paper/Spigot server plugin running every 60–120 seconds:

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TgzTelemetryPlugin extends JavaPlugin {

    private static final String BACKEND_URL = "http://localhost:3000/api/minecraft/report-stats";
    private static final String API_KEY = "tgz_plugin_sec_99a8b7c6d5e4f3a2";

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::syncPlayerStats, 200L, 2400L); // every 2 mins
    }

    private void syncPlayerStats() {
        JsonArray players = new JsonArray();
        for (Player p : Bukkit.getOnlinePlayers()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", p.getUniqueId().toString());
            obj.addProperty("username", p.getName());
            
            // Playtime in seconds (Statistic.PLAY_ONE_MINUTE returns ticks on Spigot/Paper, 20 ticks = 1 sec)
            int ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
            obj.addProperty("playtime_seconds", ticks / 20);
            obj.addProperty("kills", p.getStatistic(Statistic.PLAYER_KILLS));
            obj.addProperty("deaths", p.getStatistic(Statistic.DEATHS));
            obj.addProperty("hearts", (int) Math.round(p.getMaxHealth() / 2.0));
            players.add(obj);
        }

        JsonObject body = new JsonObject();
        body.add("players", players);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_URL))
                .header("Content-Type", "application/json")
                .header("X-Plugin-Key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            getLogger().warning("Failed to sync telemetry to TGZ backend: " + e.getMessage());
        }
    }
}
```

---

## 3. Playtime Crate Rewards Linked to Authoritative Playtime

When stats are streamed from the server:

1. The member's playtime is automatically credited to their TGZ account.
2. The user sees exact live progress toward all **5 official crates**:
   - **Common Crate**
   - **Monthly Crate**
   - **Special Crate**
   - **Arcane Crate**
   - **Matrix Crate**
3. When the configured required playtime is reached, the user clicks **Claim Crate Key**.
4. The backend securely checks eligibility, prohibits duplicate claims, and logs the voucher for in-game or staff fulfillment.
