<script setup>
// 公告管理页：所有角色可查看公告；管理员可发布/编辑/删除；系统疫苗提醒只读
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, View } from '@element-plus/icons-vue'
import {
  getNoticePage,
  getNoticeDetail,
  createNotice,
  updateNotice,
  deleteNotice
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 只有管理员有 notice:publish 权限，才能新增/编辑/删除公告
const canPublish = computed(() => userStore.hasPerm('notice:publish'))

// 系统自动生成的疫苗提醒标题固定以这个前缀开头，后端不允许修改/删除
const SYSTEM_PREFIX = '【疫苗到期提醒】'

function isSystemNotice(row) {
  return (row.title || '').startsWith(SYSTEM_PREFIX)
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

/* ---------------- 详情弹窗 ---------------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = reactive({
  noticeId: null,
  title: '',
  content: '',
  publishTime: ''
})

/* ---------------- 新增/编辑弹窗 ---------------- */
const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  noticeId: null,
  title: '',
  content: ''
})

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 个字', trigger: 'blur' }
  ],
  content: [{ max: 5000, message: '内容不能超过 5000 个字', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const data = await getNoticePage({
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

// 点标题：调详情接口后弹窗展示完整内容
async function handleDetail(row) {
  detail.noticeId = row.noticeId
  detail.title = row.title
  detail.content = ''
  detail.publishTime = ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    const data = await getNoticeDetail(row.noticeId)
    detail.title = data.title
    detail.content = data.content
    detail.publishTime = data.publishTime
  } catch (e) {
    // 请求层已提示错误，这里直接关闭详情弹窗
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, { noticeId: null, title: '', content: '' })
  if (formRef.value) formRef.value.clearValidate()
}

function handleCreate() {
  resetForm()
  dialogMode.value = 'create'
  dialogVisible.value = true
}

function handleEdit(row) {
  // 系统提醒是定时任务写进去的，后端禁止修改，前端也不显示按钮（这里再防一层）
  if (isSystemNotice(row)) {
    ElMessage.warning('系统提醒不能编辑')
    return
  }
  resetForm()
  dialogMode.value = 'edit'
  Object.assign(form, {
    noticeId: row.noticeId,
    title: row.title,
    content: row.content || ''
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
      title: form.title.trim(),
      content: form.content
    }
    if (dialogMode.value === 'edit') {
      payload.noticeId = form.noticeId
      await updateNotice(payload)
      ElMessage.success('公告修改成功')
    } else {
      await createNotice(payload)
      ElMessage.success('公告发布成功')
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
      `确定删除公告「${row.title}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deleteNotice(row.noticeId)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 请求层统一提示
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="按标题搜索"
            clearable
            style="width: 260px"
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
            v-if="canPublish"
            type="primary"
            :icon="Plus"
            @click="handleCreate"
          >
            发布公告
          </el-button>
        </div>
        <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="标题" min-width="280">
          <template #default="{ row }">
            <span class="link-title" @click="handleDetail(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="isSystemNotice(row)" type="warning" effect="light">系统提醒</el-tag>
            <el-tag v-else type="primary" effect="light">公告</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180" align="center" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleDetail(row)">
              查看
            </el-button>
            <!-- 系统提醒不显示编辑/删除按钮（后端也会拦截） -->
            <template v-if="canPublish && !isSystemNotice(row)">
              <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无公告" />
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

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="detailVisible" title="公告详情" width="640px">
      <div v-loading="detailLoading">
        <h3 style="margin: 0 0 8px">{{ detail.title }}</h3>
        <div class="cell-sub" style="margin-bottom: 16px">
          发布时间：{{ detail.publishTime || '—' }}
        </div>
        <div class="pre-wrap">{{ detail.content || '（无正文）' }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑公告弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑公告' : '发布公告'"
      width="620px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入公告标题（最多 100 字）"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容（最多 5000 字）"
            maxlength="5000"
            show-word-limit
          />
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
