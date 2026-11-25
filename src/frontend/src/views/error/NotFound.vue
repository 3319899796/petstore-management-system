<script setup>
// 404 页面：访问不存在的地址时展示，避免出现空白页
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  // 根路径在路由里会按权限跳到看板或宠物列表
  router.replace('/')
}
</script>

<template>
  <div class="not-found">
    <el-result icon="warning" title="404" sub-title="页面不存在，或您输入的地址有误">
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
</style>
