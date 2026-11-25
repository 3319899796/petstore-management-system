<script setup>
// 主框架布局：左侧权限菜单 + 顶栏（页面标题、角色、退出登录）+ 右侧内容区
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Odometer,
  Star,
  User,
  FirstAidKit,
  Notebook,
  House,
  Present,
  Bell,
  Setting,
  UserFilled,
  ArrowDown,
  StarFilled,
  SwitchButton
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/**
 * 菜单配置表：path 必须和 router/index.js 里的地址一致。
 * perms 表示"至少拥有其中一个权限码才显示这个菜单"，
 * 这样三种角色登录后会自动看到三套不同的菜单，不需要写三份页面。
 */
const allMenus = [
  { path: '/dashboard', title: '首页看板', icon: Odometer, perms: ['stat:view'] },
  { path: '/pet', title: '宠物档案', icon: Star, perms: ['pet:list'] },
  { path: '/owner', title: '主人管理', icon: User, perms: ['owner:list'] },
  { path: '/vaccine', title: '免疫记录', icon: FirstAidKit, perms: ['vaccine:list'] },
  { path: '/medical', title: '就诊记录', icon: Notebook, perms: ['medical:list'] },
  { path: '/foster', title: '寄养管理', icon: House, perms: ['foster:list'] },
  { path: '/adoption', title: '领养管理', icon: Present, perms: ['adoption:list'] },
  { path: '/notice', title: '公告通知', icon: Bell, perms: ['notice:list'] },
  { path: '/user', title: '用户管理', icon: Setting, perms: ['user:list'] }
]

// 只保留当前用户有权限的菜单项
const visibleMenus = computed(() => allMenus.filter((menu) => userStore.hasAnyPerm(menu.perms)))

// 顶栏显示的角色标签颜色
const roleTagType = computed(() => {
  if (userStore.roleId === 1) return 'danger'
  if (userStore.roleId === 2) return 'warning'
  return 'success'
})

// 退出登录：先问一下，确认后通知后端（失败也不影响），最后清本地状态回登录页
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    // 点了取消，什么都不做
    return
  }

  try {
    await logout()
  } catch (e) {
    // 退出接口失败也继续清本地登录态，避免用户被困住
  }
  userStore.clearLogin()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

// el-dropdown 的菜单命令处理
function handleCommand(command) {
  if (command === 'logout') {
    handleLogout()
  }
}
</script>

<template>
  <el-container class="layout">
    <!-- 左侧菜单栏 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="24" color="#ffd04b"><StarFilled /></el-icon>
        <span class="logo-text">宠物店管理系统</span>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          router
          background-color="#2b3a4a"
          text-color="#bfcbd9"
          active-text-color="#ffd04b"
          class="menu"
        >
          <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="aside-footer">毕业设计演示系统 v1.0</div>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ route.meta.title || '首页' }}</span>
        </div>

        <div class="header-right">
          <el-tag :type="roleTagType" effect="light">{{ userStore.roleName || '未知角色' }}</el-tag>

          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span>{{ userStore.userInfo?.username || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 子页面内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  display: flex;
  flex-direction: column;
  background-color: #2b3a4a;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  padding: 0 16px;
  flex-shrink: 0;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.menu-scroll {
  flex: 1;
}

.menu {
  border-right: none;
}

.aside-footer {
  flex-shrink: 0;
  padding: 12px 16px;
  font-size: 12px;
  color: #7f8c9a;
  text-align: center;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
  outline: none;
}

.main {
  padding: 0;
  background-color: #f5f7fa;
  overflow-y: auto;
}
</style>
