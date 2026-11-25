<script setup>
// 主人管理页：分页查询、新增/编辑、删除；列表手机号由后端脱敏显示（138****8000）
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getOwnerPage,
  createOwner,
  updateOwner,
  deleteOwner
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const dialogVisible = ref(false)
// create=新增，edit=编辑；编辑时手机号不回填，见表单里的提示
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  ownerId: null,
  ownerName: '',
  phone: '',
  userId: null
})

const rules = {
  ownerName: [
    { required: true, message: '请输入主人姓名', trigger: 'blur' },
    { max: 30, message: '姓名不能超过 30 个字', trigger: 'blur' }
  ],
  phone: [
    {
      // 手机号可以留空，但只要填了就必须是 11 位、1 开头
      pattern: /^$|^1\d{10}$/,
      message: '请输入正确的 11 位手机号',
      trigger: 'blur'
    }
  ]
}

async function loadList() {
  loading.value = true
  try {
    const data = await getOwnerPage({
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
  Object.assign(form, { ownerId: null, ownerName: '', phone: '', userId: null })
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
    ownerId: row.ownerId,
    ownerName: row.ownerName,
    // 列表里是脱敏手机号（138****8000），不能直接回填，否则校验不通过；
    // 留空提交会清空原手机号，所以表单里明确提示要重新输入完整号码
    phone: '',
    userId: row.userId || null
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
      ownerName: form.ownerName.trim(),
      phone: form.phone.trim(),
      userId: form.userId || null
    }
    if (dialogMode.value === 'edit') {
      payload.ownerId = form.ownerId
      await updateOwner(payload)
      ElMessage.success('主人信息修改成功')
    } else {
      await createOwner(payload)
      ElMessage.success('主人新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // 请求层统一提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除主人「${row.ownerName}」吗？若其名下还有宠物，后端会拒绝删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deleteOwner(row.ownerId)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 名下有宠物时后端会提示无法删除
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="主人姓名 / 手机号"
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
            v-if="userStore.hasPerm('owner:edit')"
            type="primary"
            :icon="Plus"
            @click="handleCreate"
          >
            新增主人
          </el-button>
        </div>
        <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="ownerId" label="ID" width="80" align="center" />
        <el-table-column prop="ownerName" label="主人姓名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="150">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="绑定登录账号" min-width="140" align="center">
          <template #default="{ row }">
            <span v-if="row.userId">用户 ID：{{ row.userId }}</span>
            <span v-else class="form-tip">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="userStore.hasPerm('owner:edit')"
              type="primary"
              link
              :icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="userStore.hasPerm('owner:edit')"
              type="danger"
              link
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <span v-if="!userStore.hasPerm('owner:edit')" class="form-tip">只读</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无主人数据" />
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑主人' : '新增主人'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="主人姓名" prop="ownerName">
          <el-input v-model="form.ownerName" placeholder="请输入主人姓名" maxlength="30" show-word-limit />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="11 位手机号，可不填"
            maxlength="11"
          />
          <div class="form-tip">
            列表中的手机号已脱敏，编辑时不会回填：留空提交会清空原手机号；
            如需保留，请重新输入完整 11 位号码。
          </div>
        </el-form-item>

        <el-form-item label="绑定账号 ID" prop="userId">
          <el-input-number
            v-model="form.userId"
            :min="1"
            :controls="false"
            placeholder="可选，填入 sys_user 的用户 ID"
            style="width: 100%"
          />
          <div class="form-tip">
            选填。绑定后该账号登录即为宠物主人角色；不清楚用户 ID 时可先留空。
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '保存中...' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
