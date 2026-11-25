const net = require('node:net');

/**
 * Encodes a VarInt into a Buffer
 */
function writeVarInt(value) {
  const bytes = [];
  let v = value;
  while (true) {
    if ((v & ~0x7F) === 0) {
      bytes.push(v);
      break;
    }
    bytes.push((v & 0x7F) | 0x80);
    v >>>= 7;
  }
  return Buffer.from(bytes);
}

/**
 * Reads a VarInt from a buffer at offset
 */
function readVarInt(buffer, offset = 0) {
  let numRead = 0;
  let result = 0;
  let read;
  do {
    if (offset + numRead >= buffer.length) {
      throw new Error('Unexpected end of buffer reading VarInt');
    }
    read = buffer[offset + numRead];
    const value = read & 0x7F;
    result |= value << (7 * numRead);
    numRead++;
    if (numRead > 5) {
      throw new Error('VarInt is too big');
    }
  } while ((read & 0x80) !== 0);

  return { value: result, bytesRead: numRead };
}

/**
 * Direct Minecraft Java Server List Ping (SLP) over TCP
 */
function pingMinecraftServer(host, port, timeoutMs = 3500) {
  return new Promise((resolve) => {
    const startTime = Date.now();
    const socket = new net.Socket();
    let receivedData = Buffer.alloc(0);
    let resolved = false;

    const cleanup = () => {
      socket.removeAllListeners();
      socket.destroy();
    };

    socket.setTimeout(timeoutMs);

    socket.on('connect', () => {
      try {
        // Handshake packet (Packet ID 0x00)
        // Protocol version -1 (or 765 for 1.20.4), Server address, Port, Next state (1 = status)
        const hostBuf = Buffer.from(host, 'utf8');
        const hostLen = writeVarInt(hostBuf.length);
        const portBuf = Buffer.alloc(2);
        portBuf.writeUInt16BE(port, 0);

        const protoVersion = writeVarInt(765);
        const nextState = writeVarInt(1);

        const packetData = Buffer.concat([
          Buffer.from([0x00]),
          protoVersion,
          hostLen,
          hostBuf,
          portBuf,
          nextState
        ]);

        const packetLen = writeVarInt(packetData.length);
        socket.write(Buffer.concat([packetLen, packetData]));

        // Status request packet (Packet ID 0x00, empty)
        socket.write(Buffer.from([0x01, 0x00]));
      } catch (err) {
        if (!resolved) {
          resolved = true;
          cleanup();
          resolve(null);
        }
      }
    });

    socket.on('data', (chunk) => {
      receivedData = Buffer.concat([receivedData, chunk]);
      try {
        if (receivedData.length < 3) return;
        const totalPacket = readVarInt(receivedData, 0);
        let offset = totalPacket.bytesRead;
        const packetId = readVarInt(receivedData, offset);
        offset += packetId.bytesRead;

        if (packetId.value === 0x00) {
          const stringLength = readVarInt(receivedData, offset);
          offset += stringLength.bytesRead;

          if (receivedData.length >= offset + stringLength.value) {
            const jsonString = receivedData.subarray(offset, offset + stringLength.value).toString('utf8');
            const latencyMs = Date.now() - startTime;
            const parsed = JSON.parse(jsonString);

            if (!resolved) {
              resolved = true;
              cleanup();
              resolve({
                isOnline: true,
                host,
                port,
                onlinePlayers: parsed.players?.online || 0,
                maxPlayers: parsed.players?.max || 0,
                version: parsed.version?.name || '1.20.x',
                motd: typeof parsed.description === 'string' ? parsed.description : (parsed.description?.text || 'TGZ Lifesteal SMP'),
                latencyMs,
                players: (parsed.players?.sample || []).map(p => ({
                  username: p.name,
                  uuid: p.id
                })),
                querySource: 'Native TCP SLP'
              });
            }
          }
        }
      } catch (e) {
        // Need more data
      }
    });

    socket.on('timeout', () => {
      if (!resolved) {
        resolved = true;
        cleanup();
        resolve(null);
      }
    });

    socket.on('error', () => {
      if (!resolved) {
        resolved = true;
        cleanup();
        resolve(null);
      }
    });

    socket.connect(port, host);
  });
}

/**
 * Queries Minecraft server status via Native TCP SLP with HTTP fallback to api.mcsrvstat.us
 */
async function queryMinecraftServer(host = 'node2.vyroxsmp.online', port = 10010) {
  // 1. Try Native TCP SLP
  const directResult = await pingMinecraftServer(host, port, 3000);
  if (directResult) return directResult;

  // 2. HTTP Fallback to api.mcsrvstat.us/3/{host}:{port}
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 4000);
    const resp = await fetch(`https://api.mcsrvstat.us/3/${encodeURIComponent(`${host}:${port}`)}`, {
      signal: controller.signal,
      headers: { 'User-Agent': 'TGZ-Backend/1.0' }
    });
    clearTimeout(timeout);

    if (resp.ok) {
      const data = await resp.json();
      return {
        isOnline: Boolean(data.online),
        host,
        port,
        onlinePlayers: data.players?.online || 0,
        maxPlayers: data.players?.max || 0,
        version: data.version || '1.20.x',
        motd: data.motd?.clean?.join(' ') || 'TGZ Lifesteal SMP',
        latencyMs: null,
        players: (data.players?.list || []).map(p => ({
          username: typeof p === 'string' ? p : p.name,
          uuid: typeof p === 'object' ? p.uuid : null
        })),
        querySource: 'api.mcsrvstat.us'
      };
    }
  } catch (err) {
    // Both failed
  }

  // Return clean offline status
  return {
    isOnline: false,
    host,
    port,
    onlinePlayers: 0,
    maxPlayers: 0,
    version: null,
    motd: null,
    latencyMs: null,
    players: [],
    querySource: 'Offline / Unreachable'
  };
}

module.exports = {
  pingMinecraftServer,
  queryMinecraftServer
};
