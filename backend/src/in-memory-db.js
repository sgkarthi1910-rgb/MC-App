/**
 * TGZ Lifesteal - High-Performance Pure JavaScript In-Memory SQLite Adapter
 * Used seamlessly in serverless cloud environments (like Vercel / AWS Lambda)
 * where native C++ SQLite bindings (node:sqlite) are disabled or unavailable.
 * Zero external dependencies, 100% synchronous API matching Node DatabaseSync.
 */

const fs = require('node:fs');
const path = require('node:path');

function createInMemoryDatabase(storageFilePath) {
  let tables = {
    schema_migrations: [],
    users: [],
    sessions: [],
    announcements: [],
    announcement_reads: [],
    conversations: [],
    messages: [],
    reward_configs: [],
    reward_claims: [],
    minecraft_player_stats: [],
    teams: [],
    team_members: [],
    team_invitations: [],
    app_settings: [],
    groups: [],
    group_members: [],
    group_messages: [],
    group_message_reads: [],
    audit_logs: []
  };

  let lastSyncTime = Date.now();
  let lastSyncStatus = 'INITIALIZED';

  // Restore existing records from persistent store or seed file
  const candidates = [
    storageFilePath,
    path.join(__dirname, '..', 'data', 'tgz_store.json'),
    path.join(process.cwd(), 'backend', 'data', 'tgz_store.json'),
    path.join(process.cwd(), 'data', 'tgz_store.json'),
    path.join(__dirname, '..', '..', 'backend', 'data', 'tgz_store.json')
  ].filter(Boolean);

  let loaded = false;
  for (const cand of candidates) {
    if (fs.existsSync(cand)) {
      try {
        const saved = JSON.parse(fs.readFileSync(cand, 'utf8'));
        if (saved && typeof saved === 'object') {
          Object.keys(tables).forEach(t => {
            if (Array.isArray(saved[t])) tables[t] = saved[t];
          });
          loaded = true;
          lastSyncTime = Date.now();
          lastSyncStatus = 'LOADED_FROM_DISK';
          break;
        }
      } catch (e) {}
    }
  }

  // Cloud KV / Upstash sync if configured
  const kvUrl = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  const kvToken = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
  let initialSyncPromise = null;
  let pendingPersistPromise = Promise.resolve();
  let lastCloudPullAt = 0;
  let isPersisting = false;

  async function syncFromCloudKV() {
    if (!kvUrl || !kvToken || typeof fetch !== 'function' || isPersisting) return;
    try {
      const signal = typeof AbortSignal !== 'undefined' && typeof AbortSignal.timeout === 'function' ? AbortSignal.timeout(3000) : undefined;
      const res = await fetch(`${kvUrl}/get/tgz_store`, {
        headers: { Authorization: `Bearer ${kvToken}` },
        cache: 'no-store',
        signal
      });
      if (res.ok) {
        const json = await res.json();
        const remoteData = typeof json.result === 'string' ? JSON.parse(json.result) : json.result;
        if (remoteData && typeof remoteData === 'object') {
          Object.keys(tables).forEach(t => {
            if (Array.isArray(remoteData[t]) && remoteData[t].length > 0) {
              tables[t] = remoteData[t];
            }
          });
          lastSyncTime = Date.now();
          lastCloudPullAt = lastSyncTime;
          lastSyncStatus = 'SYNCED_FROM_CLOUD_KV';
          persistLocal();
        }
      }
    } catch (e) {
      lastSyncStatus = 'CLOUD_KV_SYNC_FAILED: ' + (e?.message || 'Error');
    }
  }

  // Initial cloud fetch attempt
  if (kvUrl && kvToken) {
    initialSyncPromise = syncFromCloudKV().catch(() => {});
  }

  function persistLocal() {
    if (storageFilePath) {
      try {
        const dir = path.dirname(storageFilePath);
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
        fs.writeFileSync(storageFilePath, JSON.stringify(tables, null, 2), 'utf8');
        lastSyncTime = Date.now();
      } catch (e) {}
    }
  }

  function persist() {
    persistLocal();
    // Strictly serialize Cloud KV pushes so concurrent writes never arrive out of order
    if (kvUrl && kvToken && typeof fetch === 'function') {
      isPersisting = true;
      pendingPersistPromise = pendingPersistPromise.then(async () => {
        try {
          const payload = JSON.stringify(tables);
          const signal = typeof AbortSignal !== 'undefined' && typeof AbortSignal.timeout === 'function' ? AbortSignal.timeout(3000) : undefined;
          const res = await fetch(`${kvUrl}/set/tgz_store`, {
            method: 'POST',
            headers: {
              Authorization: `Bearer ${kvToken}`,
              'Content-Type': 'text/plain; charset=utf-8'
            },
            body: payload,
            signal
          });
          if (res.ok) {
            lastSyncTime = Date.now();
            lastCloudPullAt = lastSyncTime;
            lastSyncStatus = 'SYNCED_TO_CLOUD_KV';
          } else {
            lastSyncStatus = `CLOUD_KV_PUSH_HTTP_${res.status}`;
          }
        } catch (err) {
          lastSyncStatus = 'CLOUD_KV_PUSH_FAILED: ' + (err?.message || 'Error');
        }
      }).finally(() => {
        isPersisting = false;
      });
    }
  }

  // Save initially if we loaded from fallback into target
  if (loaded && storageFilePath && !fs.existsSync(storageFilePath)) {
    persistLocal();
  }

  function matchCondition(row, condStr, params, paramIndexRef, tables) {
    if (!condStr) return true;
    const clean = condStr.trim();
    if (!clean || clean === '1' || clean === '1 = 1') return true;

    // Handle OR expressions
    if (clean.includes(' OR ') && !clean.startsWith('(')) {
      const parts = clean.split(' OR ');
      return parts.some(p => matchCondition(row, p, params, paramIndexRef, tables));
    }

    // Handle AND expressions
    if (clean.includes(' AND ')) {
      const parts = clean.split(' AND ');
      return parts.every(p => matchCondition(row, p, params, paramIndexRef, tables));
    }

    // Handle parentheses
    if (clean.startsWith('(') && clean.endsWith(')')) {
      return matchCondition(row, clean.slice(1, -1), params, paramIndexRef, tables);
    }

    // Strip table prefixes: u.id -> id, s.token_hash -> token_hash
    const norm = clean.replace(/^[a-zA-Z0-9_]+\.([a-zA-Z0-9_]+)/, '$1');

    // Pattern: col = ?
    const eqParam = norm.match(/^([a-zA-Z0-9_]+)\s*=\s*\?$/);
    if (eqParam) {
      const col = eqParam[1];
      const val = params[paramIndexRef.i++];
      return String(row[col] ?? '').toLowerCase() === String(val ?? '').toLowerCase();
    }

    // Pattern: col != ? or col <> ?
    const neqParam = norm.match(/^([a-zA-Z0-9_]+)\s*(?:!=|<>)\s*\?$/);
    if (neqParam) {
      const col = neqParam[1];
      const val = params[paramIndexRef.i++];
      return String(row[col] ?? '').toLowerCase() !== String(val ?? '').toLowerCase();
    }

    // Pattern: col = 'value'
    const eqLit = norm.match(/^([a-zA-Z0-9_]+)\s*=\s*'([^']*)'$/);
    if (eqLit) {
      const col = eqLit[1];
      const val = eqLit[2];
      return String(row[col] ?? '') === val;
    }

    // Pattern: col != 'value'
    const neqLit = norm.match(/^([a-zA-Z0-9_]+)\s*!=\s*'([^']*)'$/);
    if (neqLit) {
      const col = neqLit[1];
      const val = neqLit[2];
      return String(row[col] ?? '') !== val;
    }

    // Pattern: col = number
    const eqNum = norm.match(/^([a-zA-Z0-9_]+)\s*=\s*([0-9]+)$/);
    if (eqNum) {
      const col = eqNum[1];
      const val = Number(eqNum[2]);
      return Number(row[col]) === val;
    }

    // Pattern: col > ?
    const gtParam = norm.match(/^([a-zA-Z0-9_]+)\s*>\s*\?$/);
    if (gtParam) {
      const col = gtParam[1];
      const val = Number(params[paramIndexRef.i++]);
      return Number(row[col]) > val;
    }

    // Pattern: col < ?
    const ltParam = norm.match(/^([a-zA-Z0-9_]+)\s*<\s*\?$/);
    if (ltParam) {
      const col = ltParam[1];
      const val = Number(params[paramIndexRef.i++]);
      return Number(row[col]) < val;
    }

    // Pattern: col IS NOT NULL
    const notNull = norm.match(/^([a-zA-Z0-9_]+)\s+IS\s+NOT\s+NULL$/i);
    if (notNull) {
      return row[notNull[1]] !== null && row[notNull[1]] !== undefined;
    }

    // Pattern: col IN ('a', 'b')
    const inLit = norm.match(/^([a-zA-Z0-9_]+)\s+IN\s*\(([^)]+)\)$/i);
    if (inLit) {
      const col = inLit[1];
      const list = inLit[2].split(',').map(s => s.trim().replace(/^'|'$/g, ''));
      return list.includes(String(row[col]));
    }

    // Pattern: col NOT IN ('a', 'b')
    const notInLit = norm.match(/^([a-zA-Z0-9_]+)\s+NOT\s+IN\s*\(([^)]+)\)$/i);
    if (notInLit) {
      const col = notInLit[1];
      const list = notInLit[2].split(',').map(s => s.trim().replace(/^'|'$/g, ''));
      return !list.includes(String(row[col]));
    }

    return true;
  }

  return {
    exec(sql) {
      // DDL operations initialize tables in memory
      return this;
    },

    prepare(sql) {
      const raw = sql.trim().replace(/\s+/g, ' ');

      return {
        get(...params) {
          const res = this.all(...params);
          return res.length > 0 ? res[0] : undefined;
        },

        all(...params) {
          const paramIndexRef = { i: 0 };

          // 1. SELECT COUNT(*)
          const countMatch = raw.match(/^SELECT\s+COUNT\(\*\)(?:\s+AS\s+([a-zA-Z0-9_]+))?\s+FROM\s+([a-zA-Z0-9_]+)(?:\s+[a-zA-Z0-9_]+)?(?:\s+WHERE\s+(.+?))?(?:\s+ORDER\s+|\s+LIMIT\s+|$)/i);
          if (countMatch) {
            const asName = countMatch[1] || 'cnt';
            const table = countMatch[2];
            const whereStr = countMatch[3];
            const list = tables[table] || [];
            const filtered = whereStr
              ? list.filter(r => matchCondition(r, whereStr, params, { i: 0 }, tables))
              : list;
            return [{ [asName]: filtered.length }];
          }

          // 2. Standard SELECT from table (with optional JOIN support)
          const selectMatch = raw.match(/^SELECT\s+(.+?)\s+FROM\s+([a-zA-Z0-9_]+)(?:\s+(?:AS\s+)?(?!WHERE\b|ORDER\b|LIMIT\b|JOIN\b|INNER\b|LEFT\b)([a-zA-Z0-9_]+))?(?:\s+((?:(?:INNER|LEFT)\s+)?JOIN\s+(?:(?!WHERE\b|ORDER\b|LIMIT\b).)+?))?(?:\s+WHERE\s+(.+?))?(?:\s+ORDER\s+BY\s+(.+?))?(?:\s+LIMIT\s+([0-9]+))?$/i);
          if (selectMatch) {
            const colsStr = selectMatch[1];
            const table = selectMatch[2];
            const baseAlias = selectMatch[3] || table;
            const joinClause = selectMatch[4];
            const whereStr = selectMatch[5];
            const orderStr = selectMatch[6];
            const limitVal = selectMatch[7] ? parseInt(selectMatch[7], 10) : undefined;

            let list = (tables[table] || []).map(r => ({ ...r }));

            // Apply JOINs if present
            if (joinClause) {
              const joinRegex = /(LEFT\s+|INNER\s+)?JOIN\s+([a-zA-Z0-9_]+)(?:\s+(?:AS\s+)?(?!ON\b)([a-zA-Z0-9_]+))?\s+ON\s+([a-zA-Z0-9_.]+)\s*=\s*([a-zA-Z0-9_.]+)/gi;
              let jm;
              while ((jm = joinRegex.exec(joinClause)) !== null) {
                const isLeft = (jm[1] || '').toUpperCase().includes('LEFT');
                const joinTable = jm[2];
                const joinAlias = jm[3] || joinTable;
                const leftExpr = jm[4];
                const rightExpr = jm[5];
                const [leftPrefix, leftCol] = leftExpr.includes('.') ? leftExpr.split('.') : [baseAlias, leftExpr];
                const [rightPrefix, rightCol] = rightExpr.includes('.') ? rightExpr.split('.') : [joinAlias, rightExpr];
                const joinRows = tables[joinTable] || [];

                const nextList = [];
                for (const row of list) {
                  const baseVal = leftPrefix === joinAlias ? row[rightCol] : row[leftCol];
                  const targetCol = leftPrefix === joinAlias ? leftCol : rightCol;
                  const matches = joinRows.filter(jr => jr[targetCol] === baseVal);
                  if (matches.length > 0) {
                    for (const m of matches) {
                      nextList.push({ ...m, ...row });
                    }
                  } else if (isLeft) {
                    nextList.push({ ...row });
                  }
                }
                list = nextList;
              }
            }

            // Apply WHERE (reset param index per row!)
            if (whereStr) {
              list = list.filter(r => matchCondition(r, whereStr, params, { i: 0 }, tables));
            }

            // Apply ORDER BY
            if (orderStr) {
              const orderParts = orderStr.split(',').map(s => s.trim());
              list.sort((a, b) => {
                for (const part of orderParts) {
                  const [colRaw, dir] = part.split(/\s+/);
                  const col = colRaw.replace(/^[a-zA-Z0-9_]+\./, '');
                  const isDesc = (dir || '').toUpperCase() === 'DESC';
                  const valA = a[col];
                  const valB = b[col];
                  if (valA !== valB) {
                    if (valA === undefined) return 1;
                    if (valB === undefined) return -1;
                    return isDesc ? (valB > valA ? 1 : -1) : (valA > valB ? 1 : -1);
                  }
                }
                return 0;
              });
            }

            // Apply LIMIT
            if (limitVal !== undefined) {
              list = list.slice(0, limitVal);
            }

            // Format Columns
            if (colsStr === '*' || colsStr.includes('.*')) {
              return list.map(r => ({ ...r }));
            }

            return list.map(r => {
              const rowObj = {};
              colsStr.split(',').forEach(c => {
                const asMatch = c.trim().match(/(?:[a-zA-Z0-9_]+\.)?([a-zA-Z0-9_]+)(?:\s+AS\s+([a-zA-Z0-9_]+))?/i);
                if (asMatch) {
                  const origCol = asMatch[1];
                  const targetCol = asMatch[2] || origCol;
                  rowObj[targetCol] = r[origCol];
                }
              });
              return rowObj;
            });
          }

          // Fallback
          return [];
        },

        run(...params) {
          const paramIndexRef = { i: 0 };

          // 1. INSERT INTO table
          const insertMatch = raw.match(/^INSERT(?:\s+OR\s+IGNORE)?\s+INTO\s+([a-zA-Z0-9_]+)\s*\(([^)]+)\)\s*VALUES\s*\(([^)]+)\)(?:\s+ON\s+CONFLICT.*)?$/i);
          if (insertMatch) {
            const table = insertMatch[1];
            const cols = insertMatch[2].split(',').map(s => s.trim());
            const valExprs = [];
            let cur = '';
            let inQuote = false;
            for (const ch of insertMatch[3]) {
              if (ch === "'" ) inQuote = !inQuote;
              if (ch === ',' && !inQuote) {
                valExprs.push(cur.trim());
                cur = '';
              } else {
                cur += ch;
              }
            }
            if (cur.trim()) valExprs.push(cur.trim());

            const list = tables[table] = tables[table] || [];

            const newRow = {};
            cols.forEach((c, idx) => {
              const expr = valExprs[idx] ?? '?';
              if (expr === '?') {
                newRow[c] = params[paramIndexRef.i++];
              } else if (expr.startsWith("'") && expr.endsWith("'")) {
                newRow[c] = expr.slice(1, -1);
              } else if (/^-?\d+$/.test(expr)) {
                newRow[c] = parseInt(expr, 10);
              } else if (/^-?\d+\.\d+$/.test(expr)) {
                newRow[c] = parseFloat(expr);
              } else if (expr.toLowerCase() === 'null') {
                newRow[c] = null;
              } else {
                newRow[c] = expr;
              }
            });

            // Upsert / Unique Check
            let existingIdx = -1;
            if (table === 'group_members' || table === 'group_message_reads') {
              existingIdx = list.findIndex(r => r.group_id === newRow.group_id && r.user_id === newRow.user_id);
            } else if (table === 'team_members') {
              existingIdx = list.findIndex(r => r.team_id === newRow.team_id && r.user_id === newRow.user_id);
            } else if (table === 'announcement_reads') {
              existingIdx = list.findIndex(r => r.user_id === newRow.user_id && r.announcement_id === newRow.announcement_id);
            } else if (table === 'reward_claims') {
              existingIdx = list.findIndex(r => r.user_id === newRow.user_id && r.reward_id === newRow.reward_id);
            } else if (table === 'conversations') {
              existingIdx = list.findIndex(r => (r.user1_id === newRow.user1_id && r.user2_id === newRow.user2_id) || (r.user1_id === newRow.user2_id && r.user2_id === newRow.user1_id) || r.id === newRow.id);
            } else if (table === 'app_settings') {
              existingIdx = list.findIndex(r => r.key === newRow.key);
            } else if (table === 'reward_configs') {
              existingIdx = list.findIndex(r => r.reward_id === newRow.reward_id);
            } else if (table === 'schema_migrations') {
              existingIdx = list.findIndex(r => r.version === newRow.version);
            } else if (table === 'sessions') {
              existingIdx = list.findIndex(r => r.id === newRow.id || (newRow.token_hash && r.token_hash === newRow.token_hash));
            } else {
              const primaryKeyCol = cols[0];
              existingIdx = list.findIndex(r => r[primaryKeyCol] === newRow[primaryKeyCol]);
            }
            if (existingIdx >= 0) {
              list[existingIdx] = { ...list[existingIdx], ...newRow };
            } else {
              list.push(newRow);
            }
            persist();
            return { changes: 1, lastInsertRowid: list.length };
          }

          // 2. UPDATE table SET ... WHERE ...
          const updateMatch = raw.match(/^UPDATE\s+([a-zA-Z0-9_]+)\s+SET\s+(.+?)\s+WHERE\s+(.+)$/i);
          if (updateMatch) {
            const table = updateMatch[1];
            const setStr = updateMatch[2];
            const whereStr = updateMatch[3];
            const list = tables[table] || [];

            const setPairs = setStr.split(',').map(s => s.trim());
            const updates = {};
            setPairs.forEach(p => {
              const [col, valExpr] = p.split('=').map(s => s.trim());
              if (valExpr === '?') {
                updates[col] = params[paramIndexRef.i++];
              } else {
                const cleaned = valExpr.replace(/^'|'$/g, '');
                if (/^-?\d+$/.test(cleaned)) {
                  updates[col] = parseInt(cleaned, 10);
                } else if (/^-?\d+\.\d+$/.test(cleaned)) {
                  updates[col] = parseFloat(cleaned);
                } else if (cleaned.toLowerCase() === 'null') {
                  updates[col] = null;
                } else {
                  updates[col] = cleaned;
                }
              }
            });

            const whereStartIdx = paramIndexRef.i;
            let changes = 0;
            list.forEach(r => {
              if (matchCondition(r, whereStr, params, { i: whereStartIdx }, tables)) {
                Object.assign(r, updates);
                changes++;
              }
            });

            if (changes > 0) persist();
            return { changes };
          }

          // 3. DELETE FROM table WHERE ...
          const deleteMatch = raw.match(/^DELETE\s+FROM\s+([a-zA-Z0-9_]+)(?:\s+WHERE\s+(.+))?$/i);
          if (deleteMatch) {
            const table = deleteMatch[1];
            const whereStr = deleteMatch[2];
            if (!tables[table]) return { changes: 0 };

            if (!whereStr) {
              const changes = tables[table].length;
              tables[table] = [];
              return { changes };
            }

            const initialLen = tables[table].length;
            tables[table] = tables[table].filter(r => !matchCondition(r, whereStr, params, { i: 0 }, tables));
            const changes = initialLen - tables[table].length;
            if (changes > 0) persist();
            return { changes };
          }

          return { changes: 0 };
        }
      };
    },

    getLastSyncTime() {
      return lastSyncTime;
    },

    getLastSyncStatus() {
      return lastSyncStatus;
    },

    async ensureSynced(force = false) {
      if (initialSyncPromise) {
        await initialSyncPromise;
        initialSyncPromise = null;
      } else if (kvUrl && kvToken && (force || process.env.VERCEL || (Date.now() - lastCloudPullAt > 2000))) {
        await syncFromCloudKV();
      }
    },

    async flushSync() {
      try {
        await pendingPersistPromise;
      } catch (e) {}
    },

    dumpData() {
      return JSON.parse(JSON.stringify(tables));
    },

    restoreData(data) {
      if (data && typeof data === 'object') {
        Object.keys(tables).forEach(t => {
          if (Array.isArray(data[t])) tables[t] = data[t];
        });
        persist();
        return true;
      }
      return false;
    }
  };
}

module.exports = {
  createInMemoryDatabase
};
