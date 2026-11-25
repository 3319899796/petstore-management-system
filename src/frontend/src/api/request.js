// axios 请求封装：统一加 token、统一拆 {code,message,data}、统一错误提示、401 自动回登录页
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

// 创建一个独立的 axios 实例，后续所有接口都通过它发请求
const request = axios.create({
  // 开发环境是 /api（vite 代理到 8080）；打包部署时也可以在 .env 里改成完整地址
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

// 防止同时有多个请求都返回 401 时，弹出好几条提示、跳转好几次
let redirecting = false

/**
 * 401 统一处理：清空登录状态并回到登录页。
 * 注意后端有两种 401 写法：
 * 1. AuthInterceptor 直接返回 HTTP 401（token 缺失/过期）
 * 2. 业务异常处理器返回 HTTP 200，但 body.code = 401
 * 所以响应成功分支和失败分支都要调用这个函数。
 */
function handleUnauthorized(message) {
  const userStore = useUserStore()
  userStore.clearLogin()

  if (redirecting) return
  redirecting = true
  ElMessage.error(message || '登录已过期，请重新登录')

  if (router.currentRoute.value.path === '/login') {
    // 已经在登录页（例如登录接口本身返回 401），只需要提示，不用再跳
    redirecting = false
    return
  }
  router.replace('/login').finally(() => {
    redirecting = false
  })
}

// 请求拦截器：每次发请求前，如果本地存了 token，就放到 Authorization 头里
request.interceptors.request.use(
  (config) => {
    // 直接读 localStorage，避免在 Pinia 还没初始化时拿不到 store
    const token = localStorage.getItem('pet_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：后端固定返回 {code,message,data}，这里把它拆开
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 不是标准返回结构（比如文件流）就原样返回，避免误判
    if (!res || typeof res.code === 'undefined') {
      return res
    }

    if (res.code === 200) {
      // 业务成功：页面里拿到的直接就是 data，不用每次写 res.data.data
      return res.data
    }

    if (res.code === 401) {
      handleUnauthorized(res.message)
      return Promise.reject(new Error(res.message || '登录已过期'))
    }

    // 403 或其他业务错误：把后端的中文 message 弹出来（例如"该数据已被引用，无法删除"）
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const res = error.response?.data
    const message = res?.message

    if (status === 401 || res?.code === 401) {
      handleUnauthorized(message)
    } else if (status === 403 || res?.code === 403) {
      ElMessage.error(message || '您没有权限执行该操作')
    } else if (!error.response) {
      // 后端没启动、断网、代理失败都会走到这里
      ElMessage.error('无法连接后端服务，请确认后端已在 8080 端口启动')
    } else {
      ElMessage.error(message || `请求失败（HTTP ${status}）`)
    }
    return Promise.reject(error)
  }
)

export default request
