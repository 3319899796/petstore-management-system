<script setup>
// 寄养管理页：主人提交寄养申请；工作人员/管理员审核（通过/驳回）、结束寄养、删除
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Check, Close } from '@element-plus/icons-vue'
import {
  getFosterPage,
  getPetOptions,
  createFoster,
  updateFoster,
  auditFoster,
  finishFoster,
  deleteFoster
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// foster:add（主人提交）或 foster:audit（工作人员代客登记）都能新增/编辑/删除
const canManage = computed(() => userStore.hasAnyPerm(['foster:add', 'foster:audit']))
// 只有工作人员/管理员能审核、结束寄养
const canAudit = computed(() => userStore.hasPerm('foster:audit'))

const loading = ref(false)
const list = ref([])
const total = ref(0)
const petOptions = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  petId: null,
  status: null
})

// 状态字典：值与后端 schema 一致
const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '寄养中' },
  { value: 'FINISHED', label: '已结束' },
  { value: 'REJECTED', label: '已驳回' }
]

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  fosterId: null,
  petId: null,
  startDate: '',
  endDate: ''
})

// 日期区间校验：结束日期不能早于开始日期（字符串 yyyy-MM-dd 可以直接比较）
function validateEndDate(rule, value, callback) {
  if (!value) {
    callback(new Error('请选择结束日期'))
  } else if (form.startDate && value < form.startDate) {
    callback(new Error('结束日期不能早于开始日期'))
  } else {
    callback()
  }
}

const rules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, validator: validateEndDate, trigger: 'change' }]
}

// 审核弹窗
const auditVisible = ref(false)
const auditSubmitting = ref(false)
const auditFormRef = ref(null)
const auditForm = reactive({
  id: null,
  approved: true,
  reason: ''
})

function validateRejectReason(rule, value, callback) {
  // 只有选择"驳回"时才要求填原因
  if (!auditForm.approved && !value.trim()) {
    callback(new Error('驳回时必须填写原因'))
  } else {
    callback()
  }
}

const auditRules = {
  reason: [{ validator: validateRejectReason, trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const data = await getFosterPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      petId: query.petId || undefined,
      status: query.status || undefined
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

async function loadPetOptions() {
  try {
    petOptions.value = await getPetOptions()
  } catch (e) {
    petOptions.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.petId = null
  query.status = null
  query.pageNum = 1
  loadList()
}

function resetForm() {
  Object.assign(form, { fosterId: null, petId: null, startDate: '', endDate: '' })
  if (formRef.value) formRef.value.clearValidate()
}

function handleCreate() {
  resetForm()
  dialogMode.value = 'create'
  dialogVisible.value = true
}

function handleEdit(row) {
  // 只有待审核状态可以修改（与后端 FosterService.update 的限制保持一致）
  if (row.status !== 'PENDING') {
    ElMessage.warning('只有待审核的寄养单可以编辑')
    return
  }
  resetForm()
  dialogMode.value = 'edit'
  Object.assign(form, {
    fosterId: row.fosterId,
    petId: row.petId,
    startDate: row.startDate,
    endDate: row.endDate
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
      petId: form.petId,
      startDate: form.startDate,
      endDate: form.endDate
    }
    if (dialogMode.value === 'edit') {
      payload.fosterId = form.fosterId
      await updateFoster(payload)
      ElMessage.success('寄养单修改成功')
    } else {
      await createFoster(payload)
      ElMessage.success('寄养申请已提交，等待工作人员审核')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // 请求层统一提示（日期不合法、时间重叠等后端 message 都会显示）
  } finally {
    submitting.value = false
  }
}

function handleAudit(row) {
  auditForm.id = row.fosterId
  auditForm.approved = true
  auditForm.reason = ''
  auditVisible.value = true
  // 弹窗刚打开时清掉上一次的校验红字
  setTimeout(() => auditFormRef.value?.clearValidate(), 0)
}

async function submitAudit() {
  if (!auditFormRef.value) return
  try {
    await auditFormRef.value.validate()
  } catch (e) {
    return
  }

  auditSubmitting.value = true
  try {
    await auditFoster({
      id: auditForm.id,
      approved: auditForm.approved,
      // 通过时不传原因；驳回时把填好的原因传给后端
      reason: auditForm.approved ? undefined : auditForm.reason.trim()
    })
    ElMessage.success(auditForm.approved ? '审核通过，宠物进入寄养中状态' : '已驳回该寄养申请')
    auditVisible.value = false
    loadList()
  } catch (e) {
    // 请求层统一提示
  } finally {
    auditSubmitting.value = false
  }
}

async function handleFinish(row) {
  try {
    await ElMessageBox.confirm(
      `确定结束「${row.petName}」的寄养吗？结束后状态变为"已结束"，不能恢复。`,
      '结束寄养',
      { type: 'warning', confirmButtonText: '确定结束', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await finishFoster(row.fosterId)
    ElMessage.success('寄养已结束')
    loadList()
  } catch (e) {
    // 请求层统一提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.petName}」的这条寄养单吗？只有待审核/已驳回的寄养单能删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deleteFoster(row.fosterId)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 请求层统一提示
  }
}

// 状态标签颜色
function statusTagType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    FINISHED: 'info',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

// 当前行是否还有可点的按钮：没有就显示"只读"，避免出现空白操作列
function hasRowAction(row) {
  const canEdit = row.status === 'PENDING' && canManage.value
  const canAuditRow = row.status === 'PENDING' && canAudit.value
  const canFinish = row.status === 'APPROVED' && canAudit.value
  const canDelete = (row.status === 'PENDING' || row.status === 'REJECTED') && canManage.value
  return canEdit || canAuditRow || canFinish || canDelete
}

onMounted(() => {
  loadList()
  loadPetOptions()
})
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
            style="width: 160px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="宠物">
          <el-select
            v-model="query.petId"
            placeholder="全部宠物"
            clearable
            filterable
            style="width: 220px"
          >
            <el-option
              v-for="pet in petOptions"
              :key="pet.petId"
              :label="`${pet.petName}（ID:${pet.petId}）`"
              :value="pet.petId"
            />
          </el-select>
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
            v-if="canManage"
            type="primary"
            :icon="Plus"
            @click="handleCreate"
          >
            新增寄养
          </el-button>
        </div>
        <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="petName" label="宠物" min-width="100" />
        <el-table-column prop="ownerName" label="主人" min-width="100" />
        <el-table-column prop="startDate" label="开始日期" width="115" align="center" />
        <el-table-column prop="endDate" label="结束日期" width="115" align="center" />
        <el-table-column label="寄养天数" width="90" align="center">
          <template #default="{ row }">{{ row.days }} 天</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" min-width="100">
          <template #default="{ row }">{{ row.handlerName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING' && canManage"
              type="primary"
              link
              :icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'PENDING' && canAudit"
              type="success"
              link
              :icon="Check"
              @click="handleAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.status === 'APPROVED' && canAudit"
              type="warning"
              link
              :icon="Close"
              @click="handleFinish(row)"
            >
              结束寄养
            </el-button>
            <el-button
              v-if="(row.status === 'PENDING' || row.status === 'REJECTED') && canManage"
              type="danger"
              link
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <span v-if="!hasRowAction(row)" class="form-tip">
              只读
            </span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无寄养记录" />
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

    <!-- 新增/编辑寄养单 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑寄养单' : '新增寄养'"
      width="540px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="宠物" prop="petId">
          <el-select
            v-model="form.petId"
            placeholder="请选择宠物（主人只能选自己的）"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="pet in petOptions"
              :key="pet.petId"
              :label="`${pet.petName}（ID:${pet.petId}）`"
              :value="pet.petId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择开始日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择结束日期"
            style="width: 100%"
          />
          <div class="form-tip">
            结束日期必须不早于开始日期，也不早于今天；同一宠物的寄养时间段不能重叠。
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗：通过 / 驳回（驳回必须填原因） -->
    <el-dialog
      v-model="auditVisible"
      title="寄养审核"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.approved">
            <el-radio :value="true">通过（进入寄养中）</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="!auditForm.approved" label="驳回原因" prop="reason">
          <el-input
            v-model="auditForm.reason"
            type="textarea"
            :rows="3"
            placeholder="驳回时必须填写原因，例如：该时间段已约满"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="submitAudit">
          {{ auditSubmitting ? '提交中...' : '确定' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
