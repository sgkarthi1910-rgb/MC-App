const { createServer } = require('../backend/src/server');

let cachedInstance = null;

module.exports = (req, res) => {
  return new Promise((resolve) => {
    let finished = false;
    const onFinish = () => {
      if (!finished) {
        finished = true;
        resolve();
      }
    };

    res.on('finish', onFinish);
    res.on('close', onFinish);

    try {
      if (!cachedInstance) {
        cachedInstance = createServer();
      }

      // Handle Vercel URL mappings
      const parsedUrl = new URL(req.url, 'http://localhost');
      let targetPath = parsedUrl.searchParams.get('__path') || req.headers['x-matched-path'] || req.headers['x-forwarded-uri'] || parsedUrl.pathname;
      if (targetPath) {
        targetPath = '/' + targetPath.replace(/^\/+/, '');
        req.url = targetPath;
      }
      if (req.url === '/api' || req.url === '/api/') {
        req.url = '/';
      }

      cachedInstance.handleRequest(req, res);
    } catch (err) {
      console.error('[TGZ LIFESTEAL] Unhandled Serverless Error:', err);
      if (!res.headersSent) {
        res.statusCode = 500;
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
        res.end(JSON.stringify({
          error: 'Serverless Function Execution Error',
          message: err.message,
          stack: err.stack
        }));
      }
      onFinish();
    }
  });
};
