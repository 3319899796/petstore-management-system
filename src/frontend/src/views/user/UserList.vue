<script setup>
// 用户管理页（仅管理员）：账号的增删改查、分配角色、重置密码
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
import {
  getUserPage,
  getUserRoles,
  createUser,
  updateUser,
  resetUserPassword,
  deleteUser
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 页面入口由路由守卫控制（user:list），按钮再按 user:edit 控制（管理员 * 自动拥有）
const canEdit = computed(() => userStore.hasPerm('user:edit'))
const currentUserId = computed(() => userStore.userInfo?.userId)

const loading = ref(false)
const list = ref([])
const total = ref(0)
const roles = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

/* ---------------- 新增/编辑弹窗 ---------------- */
const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  userId: null,
  username: '',
  password: '',
  roleId: null,
  phone: ''
})

// 密码规则：新增必填；编辑时留空表示不修改，填了才校验长度
function validatePassword(rule, value, callback) {
  if (dialogMode.value === 'create' && !value) {
    callback(new Error('请输入密码'))
  } else if (value && (value.length < 6 || value.length > 30)) {
    callback(new Error('密码长度需为 6-30 位'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 30, message: '账号长度需为 3-30 位', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]+$/,
      message: '账号只能包含字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [
    {
      pattern: /^$|^1\d{10}$/,
      message: '手机号格式不正确（11 位，以 1 开头）',
      trigger: 'blur'
    }
  ]
}

async function loadList() {
  loading.value = true
  try {
    const data = await getUserPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined
    })
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    roles.value = await getUserRoles()
  } catch (e) {
    roles.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.pageNum = 1
  loadList()
}

function resetForm() {
  Object.assign(form, {
    userId: null,
    username: '',
    password: '',
    roleId: null,
    phone: ''
  })
  if (formRef.value) formRef.value.clearValidate()
}

function handleCreate() {
  resetForm()
  dialogMode.value = 'create'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  dialogMode.value = 'edit'
  Object.assign(form, {
    userId: row.userId,
    username: row.username,
    password: '',
    roleId: row.roleId,
    // 列表里的手机号是脱敏的（138****8000），不能回填到输入框，
    // 所以留空；留空提交时不会发送 phone 字段，避免把原号码清掉
    phone: ''
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      username: form.username.trim(),
      roleId: form.roleId,
      phone: form.phone ? form.phone.trim() : ''
    }
    if (dialogMode.value === 'edit') {
      payload.userId = form.userId
      // 编辑时密码留空 = 不修改密码，因此不把 password 字段发给后端
      //（后端 @Size(min=6) 会把空串当成校验失败）
      if (form.password) {
        payload.password = form.password
      }
      // 手机号留空 = 保持原号码，不发送该字段
      if (!payload.phone) {
        delete payload.phone
      }
      await updateUser(payload)
      ElMessage.success('用户修改成功')
    } else {
      payload.password = form.password
      await createUser(payload)
      ElMessage.success('用户新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // 请求层统一提示
  } finally {
    submitting.value = false
  }
}

// 重置密码：后端固定重置为 123456
async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确定把用户「${row.username}」的密码重置为 123456 吗？`,
      '重置密码',
      { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await resetUserPassword(row.userId)
    ElMessage.success('密码已重置为 123456')
  } catch (e) {
    // 请求层统一提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.username}」吗？删除后该账号将无法登录。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deleteUser(row.userId)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 请求层统一提示（不能删除自己、至少保留一个管理员等）
  }
}

onMounted(() => {
  loadList()
  loadRoles()
})
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="账号">
          <el-input
            v-model="query.keyword"
            placeholder="按账号搜索"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="table-toolbar">
        <div>
          <el-button
            v-if="canEdit"
            type="primary"
            :icon="Plus"
            @click="handleCreate"
          >
            新增用户
          </el-button>
        </div>
        <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="userId" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="账号" min-width="130">
          <template #default="{ row }">
            {{ row.username }}
            <el-tag v-if="row.userId === currentUserId" size="small" type="info">当前账号</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="140" align="center">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="150" align="center">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canEdit"
              type="primary"
              link
              :icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="canEdit"
              type="warning"
              link
              :icon="Key"
              @click="handleResetPassword(row)"
            >
              重置密码
            </el-button>
            <el-button
              v-if="canEdit && row.userId !== currentUserId"
              type="danger"
              link
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" />
        </template>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑用户' : '新增用户'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="3-30 位字母、数字或下划线"
            maxlength="30"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="dialogMode === 'edit' ? '留空表示不修改密码' : '请输入密码（6-30 位）'"
            maxlength="30"
          />
        </el-form-item>

        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roles"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="选填，11 位手机号"
            maxlength="11"
          />
          <div v-if="dialogMode === 'edit'" class="form-tip">
            列表中的手机号已脱敏，不能回填；留空表示保持原手机号不变。
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '确定' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
