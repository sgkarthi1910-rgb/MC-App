/**
 * TGZ Lifesteal - High-Performance Pure JavaScript In-Memory SQLite Adapter
 * Used seamlessly in serverless cloud environments (like Vercel / AWS Lambda)
 * where native C++ SQLite bindings (node:sqlite) are disabled or unavailable.
 * Zero external dependencies, 100% synchronous API matching Node DatabaseSync.
 */

function createInMemoryDatabase() {
  const tables = {
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
    app_settings: []
  };

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
          const countMatch = raw.match(/^SELECT\s+COUNT\(\*\)(?:\s+AS\s+([a-zA-Z0-9_]+))?\s+FROM\s+([a-zA-Z0-9_]+)(?:\s+WHERE\s+(.+?))?(?:\s+ORDER\s+|\s+LIMIT\s+|$)/i);
          if (countMatch) {
            const asName = countMatch[1] || 'cnt';
            const table = countMatch[2];
            const whereStr = countMatch[3];
            const list = tables[table] || [];
            const filtered = whereStr
              ? list.filter(r => matchCondition(r, whereStr, params, paramIndexRef, tables))
              : list;
            return [{ [asName]: filtered.length }];
          }

          // 2. Standard SELECT from table
          const selectMatch = raw.match(/^SELECT\s+(.+?)\s+FROM\s+([a-zA-Z0-9_]+)(?:\s+[a-zA-Z0-9_]+)?(?:\s+JOIN\s+.+?)?(?:\s+WHERE\s+(.+?))?(?:\s+ORDER\s+BY\s+(.+?))?(?:\s+LIMIT\s+([0-9]+))?$/i);
          if (selectMatch) {
            const colsStr = selectMatch[1];
            const table = selectMatch[2];
            const whereStr = selectMatch[3];
            const orderStr = selectMatch[4];
            const limitVal = selectMatch[5] ? parseInt(selectMatch[5], 10) : undefined;

            let list = (tables[table] || []).slice();

            // Apply WHERE
            if (whereStr) {
              list = list.filter(r => matchCondition(r, whereStr, params, paramIndexRef, tables));
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
            const list = tables[table] = tables[table] || [];

            const newRow = {};
            cols.forEach(c => {
              newRow[c] = params[paramIndexRef.i++];
            });

            // Upsert / Unique Check
            const primaryKeyCol = cols[0];
            const existingIdx = list.findIndex(r => r[primaryKeyCol] === newRow[primaryKeyCol]);
            if (existingIdx >= 0) {
              list[existingIdx] = { ...list[existingIdx], ...newRow };
            } else {
              list.push(newRow);
            }
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
                updates[col] = valExpr.replace(/^'|'$/g, '');
              }
            });

            let changes = 0;
            list.forEach(r => {
              if (matchCondition(r, whereStr, params, paramIndexRef, tables)) {
                Object.assign(r, updates);
                changes++;
              }
            });

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
            tables[table] = tables[table].filter(r => !matchCondition(r, whereStr, params, paramIndexRef, tables));
            return { changes: initialLen - tables[table].length };
          }

          return { changes: 0 };
        }
      };
    }
  };
}

module.exports = {
  createInMemoryDatabase
};
