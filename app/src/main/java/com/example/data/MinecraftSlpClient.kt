package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Native Minecraft Java Edition Server List Ping (TCP SLP, protocol 1.7+) client.
 * Queries the Minecraft server directly over TCP for authentic online status,
 * player counts, version string, MOTD, latency, and sample player list.
 */
class MinecraftSlpClient(
    private val timeoutMs: Int = 4500
) {
    data class SlpResult(
        val online: Boolean,
        val version: String?,
        val motd: String?,
        val onlinePlayers: Int,
        val maxPlayers: Int,
        val latencyMs: Long?,
        val samplePlayers: List<MinecraftPlayer>
    )

    suspend fun ping(host: String, port: Int): SlpResult = withContext(Dispatchers.IO) {
        Socket().use { socket ->
            val connectStart = System.currentTimeMillis()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.soTimeout = timeoutMs

            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            // 1. Handshake packet (Packet ID 0x00)
            val handshakeBody = ByteArrayOutputStream().apply {
                writeVarInt(0x00)
                writeVarInt(765) // Protocol version (1.20.4+ compatible with modern Java servers)
                writeMcString(host)
                write((port ushr 8) and 0xFF)
                write(port and 0xFF)
                writeVarInt(1) // Next state: 1 = Status
            }.toByteArray()

            output.writeVarInt(handshakeBody.size)
            output.write(handshakeBody)

            // 2. Status Request packet (Packet ID 0x00, empty payload)
            output.writeVarInt(1)
            output.writeVarInt(0x00)
            output.flush()

            // 3. Read Status Response packet
            val packetLength = input.readVarInt()
            if (packetLength <= 0) {
                throw EOFException("Empty SLP status response")
            }
            val packetId = input.readVarInt()
            if (packetId != 0x00) {
                throw IllegalStateException("Unexpected SLP packet ID: $packetId")
            }

            val jsonLength = input.readVarInt()
            val jsonBytes = ByteArray(jsonLength)
            input.readFully(jsonBytes)
            val rawJson = String(jsonBytes, StandardCharsets.UTF_8)

            // 4. Optional Ping packet (Packet ID 0x01) to measure RTT latency
            val latencyMs = runCatching {
                val payload = System.currentTimeMillis()
                val pingBody = ByteArrayOutputStream().apply {
                    writeVarInt(0x01)
                    val dos = DataOutputStream(this)
                    dos.writeLong(payload)
                }.toByteArray()
                val pingStart = System.currentTimeMillis()
                output.writeVarInt(pingBody.size)
                output.write(pingBody)
                output.flush()

                input.readVarInt() // length
                val pongId = input.readVarInt()
                if (pongId == 0x01) {
                    input.readLong()
                    (System.currentTimeMillis() - pingStart).coerceAtLeast(1L)
                } else {
                    (System.currentTimeMillis() - connectStart).coerceAtLeast(1L)
                }
            }.getOrElse {
                (System.currentTimeMillis() - connectStart).coerceAtLeast(1L)
            }

            parseSlpJson(rawJson, latencyMs)
        }
    }

    private fun parseSlpJson(rawJson: String, latencyMs: Long?): SlpResult {
        val root = JSONObject(rawJson)
        val versionName = root.optJSONObject("version")?.optString("name")?.takeIf { it.isNotBlank() }
        val playersObj = root.optJSONObject("players")
        val onlineCount = playersObj?.optInt("online", 0) ?: 0
        val maxCount = playersObj?.optInt("max", 0) ?: 0

        val sampleList = mutableListOf<MinecraftPlayer>()
        val sampleArray = playersObj?.optJSONArray("sample")
        if (sampleArray != null) {
            for (i in 0 until sampleArray.length()) {
                val item = sampleArray.optJSONObject(i) ?: continue
                val name = item.optString("name").trim()
                val id = item.optString("id").trim().ifBlank { null }
                // Filter out anonymous/fake MOTD spacer entries with invalid usernames
                if (name.isNotBlank() && !name.contains("§") && name.length <= 24) {
                    sampleList.add(
                        MinecraftPlayer(
                            username = name,
                            uuid = id,
                            avatarUrl = buildMinecraftAvatarUrl(name, id)
                        )
                    )
                }
            }
        }

        val motdText = extractMotdText(root.opt("description"))
            ?.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return SlpResult(
            online = true,
            version = versionName,
            motd = motdText,
            onlinePlayers = onlineCount,
            maxPlayers = maxCount,
            latencyMs = latencyMs,
            samplePlayers = sampleList
        )
    }

    private fun extractMotdText(desc: Any?): String? {
        return when (desc) {
            is String -> desc
            is JSONObject -> {
                val sb = StringBuilder()
                val text = desc.optString("text")
                if (text.isNotEmpty()) sb.append(text)
                val extra: JSONArray? = desc.optJSONArray("extra")
                if (extra != null) {
                    for (i in 0 until extra.length()) {
                        extractMotdText(extra.opt(i))?.let { sb.append(it) }
                    }
                }
                sb.toString()
            }
            is JSONArray -> {
                val sb = StringBuilder()
                for (i in 0 until desc.length()) {
                    extractMotdText(desc.opt(i))?.let { sb.append(it) }
                }
                sb.toString()
            }
            else -> null
        }
    }

    private fun ByteArrayOutputStream.writeVarInt(value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                write(v)
                return
            }
            write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun DataOutputStream.writeVarInt(value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                writeByte(v)
                return
            }
            writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun ByteArrayOutputStream.writeMcString(str: String) {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        writeVarInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readVarInt(): Int {
        var numRead = 0
        var result = 0
        while (true) {
            val read = readByte().toInt()
            val value = read and 0x7F
            result = result or (value shl (7 * numRead))
            numRead++
            if (numRead > 5) throw RuntimeException("VarInt is too big")
            if ((read and 0x80) == 0) break
        }
        return result
    }
}

fun buildMinecraftAvatarUrl(username: String, uuid: String?): String {
    val cleanId = uuid?.replace("-", "")?.takeIf { it.length == 32 && !it.all { c -> c == '0' } }
    val identifier = cleanId ?: username
    return "https://mc-heads.net/avatar/$identifier/64"
}
