# Deploying TGZ Lifesteal Backend to Vercel

This backend is designed with native Node.js (zero heavy dependencies) and includes a dedicated serverless entrypoint for Vercel at `backend/api/index.js`.

---

## Method 1: Deploy via GitHub (Recommended)

1. **Push your project to GitHub**:
   Ensure the `backend` folder is included in your repository.

2. **Open [Vercel Dashboard](https://vercel.com/new)**:
   - Click **Add New...** -> **Project**.
   - Select your GitHub repository.

3. **Configure the Project**:
   - **Framework Preset**: Select **Other**.
   - **Root Directory**: Click *Edit* and select **`backend`** (Important!).
   - **Node.js Version**: Ensure **22.x** is selected in *Settings > General*.

4. **Add Environment Variables**:
   In the **Environment Variables** section on Vercel, add:
   | Variable | Recommended / Default Value | Description |
   | :--- | :--- | :--- |
   | `ADMIN_BOOTSTRAP_PASSWORD` | `TgzAdminInitial2026!Secure` | Initial password for initial admin provisioning ONLY (`sg_karthi`, `dhanush`, `lmtg`, `darksight`). Once changed, the new password hash is stored permanently and this value is never used again. |
   | `ADMIN_USER_IDS` | `sg_karthi,dhanush,lmtg,darksight` | Comma-separated admin unique IDs (preserves `sg_karthi`) |
   | `MINECRAFT_SERVER_HOST` | `node2.vyroxsmp.online` | TGZ SMP Server IP |
   | `MINECRAFT_SERVER_PORT` | `10010` | TGZ SMP Server Port |
   | `MINECRAFT_BEDROCK_PORT` | `10011` | TGZ SMP Bedrock Port |
   | `MINECRAFT_PLUGIN_API_KEY` | `tgz_plugin_sec_99a8b7c6d5e4f3a2` | Key for Minecraft plugin telemetry |
   | `DISCORD_INVITE_URL` | `https://discord.gg/J5P89mxXbc` | TGZ Discord link |
   | `SESSION_EXPIRATION_DAYS` | `30` | Session lifetime |

   **Persistent Database Storage on Vercel (Critical for preventing data disappearing across cold starts)**:
   Because Vercel serverless functions have ephemeral `/tmp` filesystems, configure **ONE** of the following persistent cloud stores in your Vercel Environment Variables:
   | Option | Variables to Add | Description |
   | :--- | :--- | :--- |
   | **Vercel KV / Upstash (Recommended)** | `KV_REST_API_URL`<br>`KV_REST_API_TOKEN` | Connect Upstash Redis REST or Vercel KV with zero external dependencies. |
   | **Turso Cloud SQLite** | `TURSO_DATABASE_URL`<br>`TURSO_AUTH_TOKEN` | Free distributed SQLite database in the cloud. |
   | **PostgreSQL (Neon / Supabase)** | `DATABASE_URL` | Standard cloud PostgreSQL connection string. |

5. **Click Deploy**:
   Vercel will build and assign you a live HTTPS domain, such as:
   `https://tgz-lifesteal-backend.vercel.app`

---

## Method 2: Deploy via Vercel CLI

If you have Node.js installed on your computer, you can deploy directly from your terminal:

```bash
# 1. Open the backend directory
cd C:\Users\hp\Downloads\MC-App-main\backend

# 2. Run Vercel CLI (or npx vercel)
npx vercel
```

- Follow the prompts to log in and select your team.
- When asked "Link to existing project?", select **No**.
- When asked "In which directory is your code located?", press Enter (for current directory `./`).
- To deploy to production:
```bash
npx vercel --prod
```

---

## Verifying Your Vercel Backend

Once deployed, visit your Vercel URL in your browser:
- `https://your-project.vercel.app/` -> Returns JSON status, version, and API endpoints list.
- `https://your-project.vercel.app/api/health` -> `{"status":"ok","service":"TGZ Lifesteal API"}`
- `https://your-project.vercel.app/api/leaderboard` -> Returns current live leaderboard & server telemetry.

---

## Connecting the Android APK to Vercel

1. Open the **TGZ Lifesteal APK** on your phone or emulator.
2. On the **Login Screen**, tap the **Server Connection** icon (or go to **Settings > Server Connection**).
3. In the **Server URL** field, enter your Vercel URL:
   ```text
   https://your-project.vercel.app/
   ```
4. Tap **Save & Connect**.
5. Log in with admin credentials:
   - **Unique ID**: `sg_karthi`, `dhanush`, `lmtg`, or `darksight`
   - **Password**: `TgzAdminInitial2026!Secure` (or the password you set)
6. You are now live on your cloud Vercel backend!

---

## Connecting Minecraft Server Plugin / Ingestion

To push real-time player kills, deaths, playtime, and hearts from your Minecraft server:
- Send `POST https://your-project.vercel.app/api/leaderboard/telemetry`
- Include header: `X-Plugin-Key: tgz_plugin_sec_99a8b7c6d5e4f3a2`
- Payload:
  ```json
  {
    "server": {
      "online": true,
      "playersOnline": 18,
      "maxPlayers": 100,
      "tps": 20.0
    },
    "players": [
      {
        "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
        "username": "Notch",
        "playtimeSeconds": 7200,
        "kills": 15,
        "deaths": 2,
        "hearts": 23,
        "coins": 450
      }
    ]
  }
  ```
