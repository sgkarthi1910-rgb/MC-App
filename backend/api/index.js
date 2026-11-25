const { createServer } = require('../src/server');

let cachedInstance = null;

module.exports = (req, res) => {
  if (!cachedInstance) {
    cachedInstance = createServer();
  }
  cachedInstance.server.emit('request', req, res);
};
