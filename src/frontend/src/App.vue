<template>
  <!-- 整个应用的容器：登录页、主框架都由路由决定渲染哪个组件 -->
  <router-view />
</template>

<script setup>
// App.vue 只负责放路由出口，具体页面逻辑都在 views 目录里
</script>

<style>
/* 全局基础样式（不加 scoped，所有页面都能用这些通用类） */
html,
body,
#app {
  height: 100%;
}

body {
  margin: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  font-size: 14px;
  color: #303133;
  background-color: #f5f7fa;
}

* {
  box-sizing: border-box;
}

/* 每个业务页面的最外层容器，统一留白 */
.page-container {
  padding: 16px;
}

/* 搜索区卡片与下方表格之间留一点距离 */
.search-card {
  margin-bottom: 16px;
}

.search-card .el-form-item {
  margin-bottom: 8px;
}

/* 表格上方一行：左边放操作按钮，右边放刷新等辅助按钮 */
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

/* 分页条统一靠右 */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 表单里的灰色说明文字 */
.form-tip {
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

/* 需要强调为红色（例如已逾期的天数） */
.text-danger {
  color: #f56c6c;
}

/* 表格里两行文字的情况：第二行用浅色小字 */
.cell-sub {
  font-size: 12px;
  color: #909399;
}

/* 弹窗里的长文本（公告详情）保留换行 */
.pre-wrap {
  white-space: pre-wrap;
  line-height: 1.8;
  word-break: break-word;
}

/* 可点击的表格标题 */
.link-title {
  color: #409eff;
  cursor: pointer;
}

.link-title:hover {
  text-decoration: underline;
}
</style>
