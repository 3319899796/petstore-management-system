// 用户登录状态仓库（Pinia）：token、用户信息、权限码都持久化到 localStorage，
// 刷新页面后不会掉登录；hasPerm 是全局权限判断的唯一入口
import { defineStore } from 'pinia'

// localStorage 的 key 统一放在这里，避免各个文件写错字符串
const TOKEN_KEY = 'pet_token'
const USER_KEY = 'pet_userInfo'
const PERM_KEY = 'pet_permissions'

// 从 localStorage 读 JSON，解析失败时返回默认值（防止用户手动改坏缓存导致白屏）
function readJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch (e) {
    return fallback
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    // 只保存页面展示需要的字段，不保存密码
    userInfo: readJson(USER_KEY, null),
    // 后端返回的权限码数组，例如 ['pet:list', 'pet:add']；管理员是 ['*']
    // 这里额外判断一次数组，防止 localStorage 被手动改坏后 hasPerm 报错
    permissions: (() => {
      const saved = readJson(PERM_KEY, [])
      return Array.isArray(saved) ? saved : []
    })()
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    // 当前登录人的角色 id：1 管理员，2 工作人员，3 宠物主人
    roleId: (state) => state.userInfo?.roleId ?? null,
    roleName: (state) => state.userInfo?.roleName || '',
    // 管理员权限是 *，单独判断方便页面区分管理员
    isAdmin: (state) => state.permissions.includes('*'),
    isOwner: (state) => state.userInfo?.roleId === 3
  },

  actions: {
    // 登录成功后把后端返回的 LoginVO 存起来
    setLogin(loginVO) {
      this.token = loginVO.token || ''
      this.userInfo = {
        userId: loginVO.userId,
        username: loginVO.username,
        roleId: loginVO.roleId,
        roleName: loginVO.roleName
      }
      this.permissions = Array.isArray(loginVO.permissions) ? loginVO.permissions : []

      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
      localStorage.setItem(PERM_KEY, JSON.stringify(this.permissions))
    },

    // 退出登录 / 401 时清空所有登录痕迹
    clearLogin() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(PERM_KEY)
    },

    /**
     * 判断当前用户是否拥有某个权限码。
     * 为什么要特殊处理 '*'：管理员在数据库里的权限就是 *，代表拥有全部权限，
     * 所以只要权限列表里有 *，任何权限码都返回 true。
     */
    hasPerm(code) {
      if (!code) return true
      if (this.permissions.includes('*')) return true
      return this.permissions.includes(code)
    },

    // 满足任意一个权限码即可（用于"新增或编辑都显示"这类按钮）
    hasAnyPerm(codes) {
      if (!Array.isArray(codes) || codes.length === 0) return true
      return codes.some((code) => this.hasPerm(code))
    }
  }
})
