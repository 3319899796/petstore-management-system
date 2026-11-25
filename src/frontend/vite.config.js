// Vite 配置文件：负责开发服务器、接口代理和 @ 路径别名
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 让 Vite 能编译 .vue 单文件组件
  plugins: [vue()],
  resolve: {
    alias: {
      // 用 @ 代表 src 目录，import 时不用写一长串 ../../，例如 @/api/index.js
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 开发时浏览器访问 /api/xxx，Vite 会把它转发给 8080 端口的后端，
    // 这样前后端同源，不会出现跨域问题（后端 context-path 本身就是 /api，所以不需要重写路径）
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
