// 路由配置：定义所有页面地址，并在全局前置守卫里做"未登录拦截"和"按权限拦截"
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/layout/MainLayout.vue'

// 计算当前用户登录后的默认首页：
// 主人没有 stat:view（看板）权限，所以回宠物档案页；工作人员和管理员去看板
function homePath(userStore) {
  return userStore.hasPerm('stat:view') ? '/dashboard' : '/pet'
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    // public: true 表示这个页面不登录也能访问，守卫会跳过登录检查
    meta: { title: '登录', public: true }
  },
  {
    // 主框架：左侧菜单 + 顶栏，子页面都渲染在它的 router-view 里
    path: '/',
    component: MainLayout,
    redirect: () => {
      const userStore = useUserStore()
      return userStore.isLoggedIn ? homePath(userStore) : '/login'
    },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: {
          title: '首页看板',
          perms: ['stat:view'],
          // 没有权限时给用户的提示文案（主人访问看板时会看到这句话）
          denyMessage: '该页面仅工作人员与管理员可用'
        }
      },
      {
        path: 'pet',
        name: 'PetList',
        component: () => import('@/views/pet/PetList.vue'),
        meta: { title: '宠物档案', perms: ['pet:list'] }
      },
      {
        path: 'owner',
        name: 'OwnerList',
        component: () => import('@/views/owner/OwnerList.vue'),
        meta: { title: '主人管理', perms: ['owner:list'] }
      },
      {
        path: 'vaccine',
        name: 'VaccineList',
        component: () => import('@/views/vaccine/VaccineList.vue'),
        meta: { title: '免疫记录', perms: ['vaccine:list'] }
      },
      {
        path: 'medical',
        name: 'MedicalList',
        component: () => import('@/views/medical/MedicalList.vue'),
        meta: { title: '就诊记录', perms: ['medical:list'] }
      },
      {
        path: 'foster',
        name: 'FosterList',
        component: () => import('@/views/foster/FosterList.vue'),
        meta: { title: '寄养管理', perms: ['foster:list'] }
      },
      {
        path: 'adoption',
        name: 'AdoptionList',
        component: () => import('@/views/adoption/AdoptionList.vue'),
        meta: { title: '领养管理', perms: ['adoption:list'] }
      },
      {
        path: 'notice',
        name: 'NoticeList',
        component: () => import('@/views/notice/NoticeList.vue'),
        meta: { title: '公告通知', perms: ['notice:list'] }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', perms: ['user:list'] }
      }
    ]
  },
  {
    // 404 兜底：地址写错时显示，不会白屏
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  // history 模式：地址栏是 /pet 这种干净路径（不要用 hash 的 #/pet）
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 全局前置守卫：每次跳转页面都会先执行这里。
 * 执行顺序：
 * 1. 设置浏览器标签页标题
 * 2. 登录页特殊处理：已登录的人直接送回首页
 * 3. 没登录 -> 跳到登录页，并记住原本想去的地址
 * 4. 路由配置了 perms -> 用 Pinia 的 hasAnyPerm 判断，没权限就提示并送回首页
 */
router.beforeEach((to) => {
  const userStore = useUserStore()
  document.title = to.meta.title
    ? `${to.meta.title} - 宠物店宠物信息管理系统`
    : '宠物店宠物信息管理系统'

  if (to.meta.public) {
    if (userStore.isLoggedIn) {
      return homePath(userStore)
    }
    return true
  }

  if (!userStore.isLoggedIn) {
    // query.redirect 记录原地址，登录成功后可以选择跳回去
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (Array.isArray(to.meta.perms) && to.meta.perms.length > 0 && !userStore.hasAnyPerm(to.meta.perms)) {
    ElMessage.warning(to.meta.denyMessage || '您没有权限访问该页面')
    const fallback = homePath(userStore)
    // 如果目标地址本身就是兜底首页，就不能再 next 它，否则会无限循环
    return to.path === fallback ? false : fallback
  }

  return true
})

export default router
