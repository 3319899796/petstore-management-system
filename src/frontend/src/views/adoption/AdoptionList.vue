<script setup>
// 领养管理页：主人提交领养申请；工作人员/管理员审核；审核通过后可登记回访
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Delete, Check, ChatDotRound } from '@element-plus/icons-vue'
import {
  getAdoptionPage,
  getPetOptions,
  createAdoption,
  auditAdoption,
  followUpAdoption,
  deleteAdoption
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 主人角色：申请时宠物下拉只能看到自己的宠物，所以改成手填宠物 ID
const isOwner = computed(() => userStore.isOwner)
// 能提交申请：只有宠物主人角色显示。管理员虽然权限是 *，但账号没有主人档案，
// 后端会拒绝（"当前账号还没有主人档案"），所以不让管理员看到这个按钮。
const canApply = computed(() => userStore.isOwner && userStore.hasPerm('adoption:add'))
// 能审核（工作人员/管理员）
const canAudit = computed(() => userStore.hasPerm('adoption:audit'))
// 能登记回访（工作人员/管理员）
const canFollowUp = computed(() => userStore.hasPerm('adoption:followup'))
// 能删除（后端允许 adoption:add 或 adoption:audit，且只允许删已驳回的）
const canDelete = computed(() => userStore.hasAnyPerm(['adoption:add', 'adoption:audit']))

const loading = ref(false)
const list = ref([])
const total = ref(0)
const petOptions = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null
})

const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }
]

// 状态兜底文案：正常情况直接用后端返回的 statusText
const STATUS_TEXT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }

/* ---------------- 申请弹窗 ---------------- */
const applyVisible = ref(false)
const applySubmitting = ref(false)
const applyFormRef = ref(null)
const applyForm = reactive({ petId: null })
const applyRules = {
  petId: [{ required: true, message: '请填写/选择要领养的宠物', trigger: 'change' }]
}

/* ---------------- 审核弹窗 ---------------- */
const auditVisible = ref(false)
const auditSubmitting = ref(false)
const auditFormRef = ref(null)
const auditForm = reactive({ id: null, approved: true, reason: '' })

function validateRejectReason(rule, value, callback) {
  // 驳回必须说明原因，通过时不用填
  if (!auditForm.approved && !value.trim()) {
    callback(new Error('驳回时必须填写原因'))
  } else {
    callback()
  }
}

const auditRules = {
  reason: [{ validator: validateRejectReason, trigger: 'blur' }]
}

/* ---------------- 回访弹窗 ---------------- */
const followVisible = ref(false)
const followSubmitting = ref(false)
const followFormRef = ref(null)
const followForm = reactive({ adoptionId: null, followUpResult: '' })
const followRules = {
  followUpResult: [
    { required: true, message: '请填写回访结果', trigger: 'blur' },
    { max: 500, message: '回访结果不能超过 500 字', trigger: 'blur' }
  ]
}

async function loadList() {
  loading.value = true
  try {
    const data = await getAdoptionPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
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
  query.status = null
  query.pageNum = 1
  loadList()
}

function handleApply() {
  applyForm.petId = null
  applyVisible.value = true
  // 工作人员/管理员用下拉选宠物；主人只能手填 ID，不需要加载下拉
  if (!isOwner.value) {
    loadPetOptions()
  }
  setTimeout(() => applyFormRef.value?.clearValidate(), 0)
}

async function submitApply() {
  if (!applyFormRef.value) return
  try {
    await applyFormRef.value.validate()
  } catch (e) {
    return
  }

  applySubmitting.value = true
  try {
    await createAdoption({ petId: applyForm.petId })
    ElMessage.success('领养申请已提交，等待工作人员审核')
    applyVisible.value = false
    handleSearch()
  } catch (e) {
    // 请求层统一提示（例如不能申请自己名下的宠物、已有待审核申请）
  } finally {
    applySubmitting.value = false
  }
}

function handleAudit(row) {
  auditForm.id = row.adoptionId
  auditForm.approved = true
  auditForm.reason = ''
  auditVisible.value = true
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
    await auditAdoption({
      id: auditForm.id,
      approved: auditForm.approved,
      // 通过时不传原因；驳回时传填好的原因
      reason: auditForm.approved ? undefined : auditForm.reason.trim()
    })
    ElMessage.success(
      auditForm.approved ? '审核通过，宠物主人已变更为申请人' : '已驳回该领养申请'
    )
    auditVisible.value = false
    loadList()
  } catch (e) {
    // 请求层统一提示
  } finally {
    auditSubmitting.value = false
  }
}

function handleFollowUp(row) {
  followForm.adoptionId = row.adoptionId
  followForm.followUpResult = row.followUpResult || ''
  followVisible.value = true
  setTimeout(() => followFormRef.value?.clearValidate(), 0)
}

async function submitFollowUp() {
  if (!followFormRef.value) return
  try {
    await followFormRef.value.validate()
  } catch (e) {
    return
  }

  followSubmitting.value = true
  try {
    await followUpAdoption({
      adoptionId: followForm.adoptionId,
      followUpResult: followForm.followUpResult.trim()
    })
    ElMessage.success('回访记录已保存')
    followVisible.value = false
    loadList()
  } catch (e) {
    // 请求层统一提示
  } finally {
    followSubmitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.petName}」的这条领养申请吗？只有已驳回的申请能删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deleteAdoption(row.adoptionId)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 请求层统一提示
  }
}

function statusTagType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

onMounted(loadList)
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
            v-if="canApply"
            type="primary"
            :icon="Plus"
            @click="handleApply"
          >
            申请领养
          </el-button>
        </div>
        <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="petName" label="宠物" min-width="100" />
        <el-table-column prop="ownerName" label="当前主人" min-width="100" />
        <el-table-column prop="applicantName" label="申请人" min-width="100" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ row.statusText || STATUS_TEXT[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="170" align="center" />
        <el-table-column label="审核人" width="100" align="center">
          <template #default="{ row }">{{ row.auditorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="140">
          <template #default="{ row }">
            <span :class="{ 'text-danger': !!row.rejectReason }">
              {{ row.rejectReason || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="回访结果" min-width="180">
          <template #default="{ row }">
            <div v-if="row.followUpResult">
              <div class="pre-wrap">{{ row.followUpResult }}</div>
              <div class="cell-sub">{{ row.followUpTime }}</div>
            </div>
            <span v-else class="cell-sub">暂无回访</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
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
              v-if="row.status === 'APPROVED' && canFollowUp"
              type="primary"
              link
              :icon="ChatDotRound"
              @click="handleFollowUp(row)"
            >
              {{ row.followUpResult ? '修改回访' : '回访登记' }}
            </el-button>
            <el-button
              v-if="row.status === 'REJECTED' && canDelete"
              type="danger"
              link
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <span
              v-if="
                !(row.status === 'PENDING' && canAudit) &&
                !(row.status === 'APPROVED' && canFollowUp) &&
                !(row.status === 'REJECTED' && canDelete)
              "
              class="form-tip"
            >
              只读
            </span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无领养申请" />
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

    <!-- 申请领养：主人手填宠物ID，工作人员/管理员用下拉 -->
    <el-dialog
      v-model="applyVisible"
      title="申请领养"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="宠物" prop="petId">
          <!-- 主人角色：/pet/options 只会返回自己的宠物，而系统不允许申请自己名下的宠物，
               所以这里只能让主人手填想要领养的宠物 ID -->
          <el-input-number
            v-if="isOwner"
            v-model="applyForm.petId"
            :min="1"
            :controls="false"
            placeholder="请输入宠物ID"
            style="width: 100%"
          />
          <el-select
            v-else
            v-model="applyForm.petId"
            placeholder="请选择宠物"
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
          <div v-if="isOwner" class="form-tip">
            说明：主人账号只能查询到自己名下的宠物，无法在线浏览别人的宠物列表，因此申请领养需要填写目标宠物的 ID（可向店员询问）。不能申请自己名下已有的宠物。
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applySubmitting" @click="submitApply">
          {{ applySubmitting ? '提交中...' : '提交申请' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog
      v-model="auditVisible"
      title="领养审核"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.approved">
            <el-radio :value="true">通过（宠物归申请人所有）</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="!auditForm.approved" label="驳回原因" prop="reason">
          <el-input
            v-model="auditForm.reason"
            type="textarea"
            :rows="3"
            placeholder="驳回时必须填写原因，例如：申请人不符合领养条件"
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

    <!-- 回访登记弹窗 -->
    <el-dialog
      v-model="followVisible"
      title="领养回访登记"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="followFormRef"
        :model="followForm"
        :rules="followRules"
        label-width="90px"
      >
        <el-form-item label="回访结果" prop="followUpResult">
          <el-input
            v-model="followForm.followUpResult"
            type="textarea"
            :rows="4"
            placeholder="例如：已电话回访，宠物状态良好，主人满意"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="followVisible = false">取消</el-button>
        <el-button type="primary" :loading="followSubmitting" @click="submitFollowUp">
          {{ followSubmitting ? '保存中...' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
