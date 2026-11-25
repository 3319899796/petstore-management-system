<script setup>
// 登录页：账号密码登录，回车可提交；登录成功后按角色跳到不同首页
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
// 后端返回的错误信息（例如"用户名或密码错误"），显示在登录按钮上方
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

// Element Plus 表单校验规则
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  errorMsg.value = ''
  loading.value = true
  try {
    const data = await login({
      username: form.username.trim(),
      password: form.password
    })
    // 把 token、角色、权限码存进 Pinia（同时写入 localStorage）
    userStore.setLogin(data)
    ElMessage.success(`登录成功，欢迎您，${data.roleName}`)

    // 主人没有 stat:view 权限，看板进不去，所以直接去宠物档案页；
    // 工作人员和管理员去看板首页
    if (userStore.hasPerm('stat:view')) {
      router.replace('/dashboard')
    } else {
      router.replace('/pet')
    }
  } catch (error) {
    // 请求层已经弹过 ElMessage，这里再把后端 message 固定显示在页面上，方便答辩演示
    errorMsg.value =
      error?.response?.data?.message || error?.message || '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <div class="logo-circle">
          <el-icon :size="30" color="#fff"><User /></el-icon>
        </div>
        <h2 class="login-title">宠物店宠物信息管理系统</h2>
        <p class="login-subtitle">Pet Store Management System</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            clearable
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          :closable="false"
          show-icon
          class="login-error"
        />

        <el-button
          type="primary"
          size="large"
          class="login-button"
          :loading="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form>

      <div class="demo-tip">
        <div>演示账号（密码均为 123456）：</div>
        <div>管理员 admin ｜ 工作人员 staff ｜ 宠物主人 owner</div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1f2d3d 0%, #3a5169 60%, #4f6d8f 100%);
}

.login-card {
  width: 420px;
  padding: 8px 12px;
  border-radius: 10px;
}

.login-header {
  text-align: center;
  margin-bottom: 18px;
}

.logo-circle {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #337ecc);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.login-subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: #909399;
  letter-spacing: 1px;
}

.login-error {
  margin-bottom: 14px;
}

.login-button {
  width: 100%;
  margin-top: 4px;
}

.demo-tip {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #dcdfe6;
  font-size: 12px;
  line-height: 1.9;
  color: #909399;
  text-align: center;
}
</style>
