async function hashPin(pin, phone) {
  const data = new TextEncoder().encode(phone + ':' + pin);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
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
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET,POST,OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type,Authorization' }
      });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    try {
      if (path === '/api/register' && request.method === 'POST') {
        const { phone, pin } = await request.json();
        if (!phone || !pin || pin.length < 4) {
          return createResponse(400, { error: 'Invalid phone or PIN (min 4 digits)' });
        }
        const existing = await env.KV.get('user:' + phone);
        if (existing) {
          return createResponse(409, { error: 'Phone already registered, please login' });
        }
        const pinHash = await hashPin(pin, phone);
        await env.KV.put('user:' + phone, JSON.stringify({ pinHash, createdAt: Date.now() }));
        const rawToken = phone + ':' + Date.now() + ':' + crypto.randomUUID();
        const tokenBytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(rawToken));
        const token = Array.from(new Uint8Array(tokenBytes)).map(b => b.toString(16).padStart(2, '0')).join('');
        await env.KV.put('session:' + token, phone, { expirationTtl: 2592000 });
        return createResponse(201, { token, message: 'Registered successfully' });
      }

      if (path === '/api/login' && request.method === 'POST') {
        const { phone, pin } = await request.json();
        if (!phone || !pin) {
          return createResponse(400, { error: 'Phone and PIN required' });
        }
        const lockoutKey = 'lockout:' + phone;
        const lockoutStr = await env.KV.get(lockoutKey);
        if (lockoutStr) {
          return createResponse(429, { error: 'Too many attempts. Try again later.' });
        }
        const failKey = 'fail:' + phone;
        let failCount = parseInt(await env.KV.get(failKey) || '0');

        const userStr = await env.KV.get('user:' + phone);
        if (!userStr) {
          return createResponse(401, { error: 'Phone not found' });
        }
        const user = JSON.parse(userStr);
        const pinHash = await hashPin(pin, phone);
        if (pinHash !== user.pinHash) {
          failCount++;
          await env.KV.put(failKey, String(failCount), { expirationTtl: 3600 });
          if (failCount >= 5) {
            await env.KV.put(lockoutKey, '1', { expirationTtl: 3600 });
          }
          return createResponse(401, { error: 'Wrong PIN' });
        }
        await env.KV.delete(failKey);
        const rawToken = phone + ':' + Date.now() + ':' + crypto.randomUUID();
        const tokenBytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(rawToken));
        const token = Array.from(new Uint8Array(tokenBytes)).map(b => b.toString(16).padStart(2, '0')).join('');
        await env.KV.put('session:' + token, phone, { expirationTtl: 2592000 });
        return createResponse(200, { token, message: 'Login successful' });
      }

      if (path === '/api/backup' && request.method === 'POST') {
        const token = getAuthUser(request);
        if (!token) return createResponse(401, { error: 'No token' });
        const phone = await env.KV.get('session:' + token);
        if (!phone) return createResponse(401, { error: 'Invalid or expired token' });

        const body = await request.json();
        if (!body.data) return createResponse(400, { error: 'No backup data' });

        await env.KV.put('backup:' + phone, JSON.stringify({
          data: body.data,
          version: body.version || 1,
          createdAt: Date.now()
        }));
        return createResponse(200, { message: 'Backup saved' });
      }

      if (path === '/api/backup' && request.method === 'GET') {
        const token = getAuthUser(request);
        if (!token) return createResponse(401, { error: 'No token' });
        const phone = await env.KV.get('session:' + token);
        if (!phone) return createResponse(401, { error: 'Invalid or expired token' });

        const backupStr = await env.KV.get('backup:' + phone);
        if (!backupStr) return createResponse(404, { error: 'No backup found' });

        const backup = JSON.parse(backupStr);
        return createResponse(200, { data: backup.data, version: backup.version, createdAt: backup.createdAt });
      }

      return createResponse(404, { error: 'Not found' });
    } catch (e) {
      return createResponse(500, { error: 'Internal error: ' + e.message });
    }
  }
};
