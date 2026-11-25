// 极简前端静态服务器 + /api 反向代理（只用 node 内置模块，不需要任何依赖）
// 用法：node serve-frontend.mjs   → 打开 http://127.0.0.1:5173
// 作用：把 src/frontend/dist 里的构建产物当静态站点发布，并把 /api 请求转发给后端 8080。
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DIST = path.resolve(__dirname, '..', 'src', 'frontend', 'dist');
const PORT = Number(process.env.PORT || 5173);
const BACKEND = { host: '127.0.0.1', port: 8080 };

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8',
};

if (!fs.existsSync(DIST)) {
  console.error('找不到构建产物：' + DIST + '\n请先在 src/frontend 下执行 npm run build');
  process.exit(1);
}

const server = http.createServer((req, res) => {
  const url = req.url || '/';

  // 1) 接口请求：原样转发给后端（保留方法、头、体和查询串）
  if (url.startsWith('/api/')) {
    const proxyReq = http.request(
      { host: BACKEND.host, port: BACKEND.port, path: url, method: req.method, headers: req.headers },
      (proxyRes) => {
        res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
        proxyRes.pipe(res);
      }
    );
    proxyReq.on('error', (err) => {
      res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ code: 502, message: '后端未启动或无法连接：' + err.message, data: null }));
    });
    req.pipe(proxyReq);
    return;
  }

  // 2) 静态文件：先按路径找，找不到就回 index.html（前端路由接管）
  const cleanPath = decodeURIComponent(url.split('?')[0]);
  let filePath = path.join(DIST, cleanPath);
  if (!filePath.startsWith(DIST)) filePath = path.join(DIST, 'index.html'); // 防目录穿越
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(DIST, 'index.html');
  }
  const ext = path.extname(filePath).toLowerCase();
  res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' });
  fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, '127.0.0.1', () => {
  console.log('前端已启动：http://127.0.0.1:' + PORT);
  console.log('静态目录：' + DIST);
  console.log('/api 已代理到 http://' + BACKEND.host + ':' + BACKEND.port);
});
