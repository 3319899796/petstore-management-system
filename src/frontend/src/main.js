// 应用入口：创建 Vue 应用，按顺序装上 Pinia（状态）、Router（路由）、Element Plus（UI 组件库）
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 把 Element Plus 自带的所有图标注册成全局组件，
// 这样模板里可以直接写 <el-icon><Plus /></el-icon>，不用每个页面单独 import
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

// 先装 Pinia 再装 Router：因为路由守卫里要用 Pinia 里的用户登录状态
app.use(createPinia())
app.use(router)
// locale 设为中文，分页、日期选择器、弹窗按钮等才会显示中文
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
