/**
 * Post Desk REST API Server — Node.js Implementation
 * Matches Spring Boot postservice spec for Experiment 2.1.1 & 2.1.2
 */

const http = require('http');
const crypto = require('crypto');

const PORT = process.env.PORT || 8080;

let posts = [
  {
    id: 1,
    title: "Welcome to Post Desk",
    content: "This is a sample scheduled post demonstrating RESTful API design, Bean Validation, and MDC correlation tracing.",
    scheduledAt: new Date(Date.now() + 86400000).toISOString(),
    status: "SCHEDULED",
    createdAt: new Date().toISOString(),
    publishedAt: null
  }
];

let nextId = 2;

function generateCorrelationId() {
  return 'req-' + crypto.randomBytes(6).toString('hex');
}

function sendEnvelope(res, statusCode, success, message, data, correlationId) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, X-Correlation-Id',
    'Access-Control-Expose-Headers': 'X-Correlation-Id',
    'X-Correlation-Id': correlationId
  });

  const envelope = {
    success,
    message,
    data,
    correlationId,
    timestamp: new Date().toISOString()
  };

  res.end(JSON.stringify(envelope));
}

function parseJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => body += chunk.toString());
    req.on('end', () => {
      if (!body) return resolve({});
      try {
        resolve(JSON.parse(body));
      } catch (err) {
        reject(err);
      }
    });
  });
}

const server = http.createServer(async (req, res) => {
  const startTime = Date.now();
  const correlationId = req.headers['x-correlation-id'] || generateCorrelationId();
  const url = new URL(req.url, `http://${req.headers.host}`);
  const path = url.pathname;
  const method = req.method;

  // Log incoming request with correlation ID (MDC pattern)
  console.log(`[${new Date().toLocaleTimeString()}] INFO  [${correlationId}] server - ${method} ${path}`);

  // CORS Preflight
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, X-Correlation-Id',
      'Access-Control-Expose-Headers': 'X-Correlation-Id',
      'X-Correlation-Id': correlationId
    });
    res.end();
    return;
  }

  try {
    // GET /api/posts
    if (method === 'GET' && path === '/api/posts') {
      return sendEnvelope(res, 200, true, null, posts, correlationId);
    }

    // GET /api/posts/:id
    const getMatch = path.match(/^\/api\/posts\/(\d+)$/);
    if (method === 'GET' && getMatch) {
      const id = parseInt(getMatch[1], 10);
      const post = posts.find(p => p.id === id);
      if (!post) {
        return sendEnvelope(res, 404, false, `post ${id} was not found`, null, correlationId);
      }
      return sendEnvelope(res, 200, true, null, post, correlationId);
    }

    // POST /api/posts
    if (method === 'POST' && path === '/api/posts') {
      const body = await parseJsonBody(req);
      const errors = {};

      if (!body.title || !body.title.trim()) {
        errors.title = "Title is required";
      } else if (body.title.length > 120) {
        errors.title = "Title must not exceed 120 characters";
      }

      if (!body.content || !body.content.trim()) {
        errors.content = "Content is required";
      }

      if (!body.scheduledAt) {
        errors.scheduledAt = "scheduledAt date-time is required";
      } else if (new Date(body.scheduledAt) <= new Date()) {
        errors.scheduledAt = "scheduledAt date-time must be in the future";
      }

      if (Object.keys(errors).length > 0) {
        return sendEnvelope(res, 400, false, "Validation failed", errors, correlationId);
      }

      const newPost = {
        id: nextId++,
        title: body.title.trim(),
        content: body.content.trim(),
        scheduledAt: new Date(body.scheduledAt).toISOString(),
        status: "SCHEDULED",
        createdAt: new Date().toISOString(),
        publishedAt: null
      };

      posts.push(newPost);
      return sendEnvelope(res, 201, true, "post scheduled", newPost, correlationId);
    }

    // PUT /api/posts/:id
    const putMatch = path.match(/^\/api\/posts\/(\d+)$/);
    if (method === 'PUT' && putMatch) {
      const id = parseInt(putMatch[1], 10);
      const post = posts.find(p => p.id === id);
      if (!post) {
        return sendEnvelope(res, 404, false, `post ${id} was not found`, null, correlationId);
      }

      if (post.status === 'PUBLISHED') {
        return sendEnvelope(res, 409, false, `post ${id} is already published and can't be edited`, null, correlationId);
      }

      const body = await parseJsonBody(req);
      const errors = {};

      if (!body.title || !body.title.trim()) errors.title = "Title is required";
      if (!body.content || !body.content.trim()) errors.content = "Content is required";
      if (!body.scheduledAt) errors.scheduledAt = "scheduledAt date-time is required";

      if (Object.keys(errors).length > 0) {
        return sendEnvelope(res, 400, false, "Validation failed", errors, correlationId);
      }

      post.title = body.title.trim();
      post.content = body.content.trim();
      post.scheduledAt = new Date(body.scheduledAt).toISOString();

      return sendEnvelope(res, 200, true, "post updated", post, correlationId);
    }

    // PATCH /api/posts/:id/publish
    const patchMatch = path.match(/^\/api\/posts\/(\d+)\/publish$/);
    if (method === 'PATCH' && patchMatch) {
      const id = parseInt(patchMatch[1], 10);
      const post = posts.find(p => p.id === id);
      if (!post) {
        return sendEnvelope(res, 404, false, `post ${id} was not found`, null, correlationId);
      }

      if (post.status === 'PUBLISHED') {
        return sendEnvelope(res, 409, false, `post ${id} is already published`, null, correlationId);
      }

      post.status = 'PUBLISHED';
      post.publishedAt = new Date().toISOString();

      return sendEnvelope(res, 200, true, "post published", post, correlationId);
    }

    // DELETE /api/posts/:id
    const deleteMatch = path.match(/^\/api\/posts\/(\d+)$/);
    if (method === 'DELETE' && deleteMatch) {
      const id = parseInt(deleteMatch[1], 10);
      const index = posts.findIndex(p => p.id === id);
      if (index === -1) {
        return sendEnvelope(res, 404, false, `post ${id} was not found`, null, correlationId);
      }

      posts.splice(index, 1);
      return sendEnvelope(res, 200, true, "post deleted", null, correlationId);
    }

    // Unmatched path -> 404
    sendEnvelope(res, 404, false, `Endpoint ${method} ${path} not found`, null, correlationId);

  } catch (err) {
    console.error(`[${correlationId}] ERROR unhandled exception:`, err);
    sendEnvelope(res, 500, false, "An unexpected internal server error occurred", null, correlationId);
  } finally {
    const elapsed = Date.now() - startTime;
    console.log(`[${new Date().toLocaleTimeString()}] INFO  [${correlationId}] CorrelationIdFilter - ${method} ${path} -> ${res.statusCode} (${elapsed}ms)`);
  }
});

server.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`🚀 Post Desk REST API Server listening on http://localhost:${PORT}`);
  console.log(`   - Experiment 2.1.1 & 2.1.2 REST API Specification`);
  console.log(`   - Endpoints: GET/POST /api/posts, PUT/DELETE /api/posts/:id, PATCH /api/posts/:id/publish`);
  console.log(`=======================================================`);
});
