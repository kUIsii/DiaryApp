// DiaryApp Sync Worker
// Handles: phone + PIN login, backup upload/download

// Simple SHA-256 hash for PIN
async function hashPin(pin, phone) {
  const data = new TextEncoder().encode(phone + ':' + pin);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
}

function generateToken(phone) {
  const data = new TextEncoder().encode(phone + ':' + Date.now() + ':' + crypto.randomUUID());
  const hash = crypto.subtle.digest('SHA-256', data);
  return hash;
}

async function createResponse(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
  });
}

function getAuthUser(request) {
  const auth = request.headers.get('Authorization');
  if (!auth || !auth.startsWith('Bearer ')) return null;
  return auth.slice(7);
}

export default {
  async fetch(request, env) {
    // CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET,POST,OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type,Authorization' }
      });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    try {
      // POST /api/register - phone + pin, returns token
      if (path === '/api/register' && request.method === 'POST') {
        const { phone, pin } = await request.json();
        if (!phone || !pin || pin.length < 4) {
          return createResponse(400, { error: 'Invalid phone or PIN (min 4 digits)' });
        }
        const existing = await env.DB.prepare('SELECT id FROM users WHERE phone = ?').bind(phone).first();
        if (existing) {
          return createResponse(409, { error: 'Phone already registered, please login' });
        }
        const pinHash = await hashPin(pin, phone);
        const result = await env.DB.prepare('INSERT INTO users (phone, pin_hash) VALUES (?, ?)').bind(phone, pinHash).run();
        const token = phone + ':' + Date.now();
        return createResponse(201, { token, message: 'Registered successfully' });
      }

      // POST /api/login - phone + pin, returns token
      if (path === '/api/login' && request.method === 'POST') {
        const { phone, pin } = await request.json();
        if (!phone || !pin) {
          return createResponse(400, { error: 'Phone and PIN required' });
        }
        // Check for rate limiting (5 failed attempts lockout)
        const lockoutKey = 'lockout:' + phone;
        const lockoutStr = await env.KV.get(lockoutKey);
        if (lockoutStr) {
          return createResponse(429, { error: 'Too many attempts. Try again later.' });
        }
        // Check fail count
        const failKey = 'fail:' + phone;
        let failCount = parseInt(await env.KV.get(failKey) || '0');

        const user = await env.DB.prepare('SELECT id, pin_hash FROM users WHERE phone = ?').bind(phone).first();
        if (!user) {
          return createResponse(401, { error: 'Phone not found' });
        }
        const pinHash = await hashPin(pin, phone);
        if (pinHash !== user.pin_hash) {
          failCount++;
          await env.KV.put(failKey, String(failCount), { expirationTtl: 3600 });
          if (failCount >= 5) {
            await env.KV.put(lockoutKey, '1', { expirationTtl: 3600 });
          }
          return createResponse(401, { error: 'Wrong PIN' });
        }
        // Reset fail count on success
        await env.KV.delete(failKey);
        const rawToken = phone + ':' + Date.now() + ':' + crypto.randomUUID();
        const tokenBytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(rawToken));
        const token = Array.from(new Uint8Array(tokenBytes)).map(b => b.toString(16).padStart(2, '0')).join('');
        // Store token with 30 day expiry
        await env.KV.put('session:' + token, phone, { expirationTtl: 2592000 });
        return createResponse(200, { token, message: 'Login successful' });
      }

      // POST /api/backup - upload backup data
      if (path === '/api/backup' && request.method === 'POST') {
        const token = getAuthUser(request);
        if (!token) return createResponse(401, { error: 'No token' });
        const phone = await env.KV.get('session:' + token);
        if (!phone) return createResponse(401, { error: 'Invalid or expired token' });

        const body = await request.json();
        if (!body.data) return createResponse(400, { error: 'No backup data' });

        const user = await env.DB.prepare('SELECT id FROM users WHERE phone = ?').bind(phone).first();
        await env.DB.prepare('DELETE FROM backups WHERE user_id = ?').bind(user.id).run();
        await env.DB.prepare('INSERT INTO backups (user_id, backup_data, version) VALUES (?, ?, ?)')
          .bind(user.id, JSON.stringify(body.data), body.version || 1).run();
        return createResponse(200, { message: 'Backup saved' });
      }

      // GET /api/backup - download backup data
      if (path === '/api/backup' && request.method === 'GET') {
        const token = getAuthUser(request);
        if (!token) return createResponse(401, { error: 'No token' });
        const phone = await env.KV.get('session:' + token);
        if (!phone) return createResponse(401, { error: 'Invalid or expired token' });

        const user = await env.DB.prepare('SELECT id FROM users WHERE phone = ?').bind(phone).first();
        if (!user) return createResponse(404, { error: 'User not found' });

        const backup = await env.DB.prepare('SELECT backup_data, version, created_at FROM backups WHERE user_id = ? ORDER BY created_at DESC LIMIT 1')
          .bind(user.id).first();
        if (!backup) return createResponse(404, { error: 'No backup found' });

        return createResponse(200, { data: JSON.parse(backup.backup_data), version: backup.version, createdAt: backup.created_at });
      }

      return createResponse(404, { error: 'Not found' });
    } catch (e) {
      return createResponse(500, { error: 'Internal error: ' + e.message });
    }
  }
};
