<script setup>
// 免疫记录页：按宠物筛选、只看即将到期、新增/编辑/删除，以及手动触发疫苗到期提醒扫描
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Bell } from '@element-plus/icons-vue'
import {
  getVaccinePage,
  getPetOptions,
  createVaccine,
  updateVaccine,
  deleteVaccine,
  remindNotice
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const petOptions = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  petId: null,
  dueSoon: false
})

const remindLoading = ref(false)
// 有 notice:publish（管理员）或 vaccine:edit（工作人员）才显示"立即执行提醒扫描"
const canRemind = computed(() => userStore.hasAnyPerm(['notice:publish', 'vaccine:edit']))

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  vaccineId: null,
  petId: null,
  vaccineType: '',
  inoculateDate: '',
  nextDueDate: '',
  cycleDays: null
})

const rules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  vaccineType: [
    { required: true, message: '请输入疫苗类型', trigger: 'blur' },
    { max: 30, message: '疫苗类型不能超过 30 个字', trigger: 'blur' }
  ],
  inoculateDate: [{ required: true, message: '请选择接种日期', trigger: 'change' }]
}

// 生成 yyyy-MM-dd 格式的今天，作为新增时的默认接种日期
function todayStr() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

async function loadList() {
  loading.value = true
  try {
    const data = await getVaccinePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      petId: query.petId || undefined,
      // 只在开关打开时传 dueSoon=true，关掉时传 undefined 表示不筛选
      dueSoon: query.dueSoon ? true : undefined
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
  query.dueSoon = false
  query.pageNum = 1
  loadList()
}

function resetForm() {
  Object.assign(form, {
    vaccineId: null,
    petId: null,
    vaccineType: '',
    inoculateDate: '',
    nextDueDate: '',
    cycleDays: null
  })
  if (formRef.value) formRef.value.clearValidate()
}

function handleCreate() {
  resetForm()
  dialogMode.value = 'create'
  // 接种日期默认今天，减少演示时的手动操作
  form.inoculateDate = todayStr()
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  dialogMode.value = 'edit'
  Object.assign(form, {
    vaccineId: row.vaccineId,
    petId: row.petId,
    vaccineType: row.vaccineType,
    inoculateDate: row.inoculateDate,
    nextDueDate: row.nextDueDate || '',
    cycleDays: row.cycleDays || null
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
      vaccineType: form.vaccineType.trim(),
      inoculateDate: form.inoculateDate,
      // 留空时传 null，让后端按 1 年（365 天）自动算下次到期日
      nextDueDate: form.nextDueDate || null,
      cycleDays: form.cycleDays || null
    }
    if (dialogMode.value === 'edit') {
      payload.vaccineId = form.vaccineId
      await updateVaccine(payload)
      ElMessage.success('免疫记录修改成功')
    } else {
      await createVaccine(payload)
      ElMessage.success('免疫记录新增成功')
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
      `确定删除「${row.petName}」的这条 ${row.vaccineType} 记录吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deleteVaccine(row.vaccineId)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 请求层统一提示
  }
}

// 手动触发一次定时任务的逻辑，成功后后端返回本次生成的提醒条数
async function handleRemind() {
  remindLoading.value = true
  try {
    const count = await remindNotice()
    if (count > 0) {
      ElMessage.success(`提醒扫描完成，本次生成 ${count} 条疫苗到期提醒`)
    } else {
      ElMessage.info('提醒扫描完成，本次没有需要新生成的提醒（可能之前已提醒过）')
    }
    loadList()
  } catch (e) {
    // 请求层统一提示
  } finally {
    remindLoading.value = false
  }
}

// 剩余天数标签文案：负数表示已经过期
function daysText(daysLeft) {
  if (daysLeft === null || daysLeft === undefined) return '—'
  if (daysLeft < 0) return `已逾期 ${-daysLeft} 天`
  if (daysLeft === 0) return '今天到期'
  return `${daysLeft} 天后到期`
}

function daysTagType(daysLeft) {
  if (daysLeft === null || daysLeft === undefined) return 'info'
  if (daysLeft <= 0) return 'danger'
  if (daysLeft <= 7) return 'warning'
  return 'info'
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

        <el-form-item label="到期筛选">
          <el-switch v-model="query.dueSoon" active-text="只看即将到期（7 天内）" />
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
            v-if="userStore.hasPerm('vaccine:add')"
            type="primary"
            :icon="Plus"
            @click="handleCreate"
          >
            新增免疫记录
          </el-button>
        </div>
        <div>
          <el-button
            v-if="canRemind"
            type="warning"
            :icon="Bell"
            :loading="remindLoading"
            @click="handleRemind"
          >
            立即执行提醒扫描
          </el-button>
          <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="petName" label="宠物名称" min-width="110" />
        <el-table-column prop="ownerName" label="主人" min-width="110" />
        <el-table-column prop="vaccineType" label="疫苗类型" min-width="150" />
        <el-table-column prop="inoculateDate" label="接种日期" width="120" align="center" />
        <el-table-column prop="nextDueDate" label="下次到期日" width="120" align="center" />
        <el-table-column label="剩余天数" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="daysTagType(row.daysLeft)" effect="light">
              {{ daysText(row.daysLeft) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提醒状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.reminded" type="success" effect="plain">已提醒</el-tag>
            <el-tag v-else type="info" effect="plain">未提醒</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="userStore.hasPerm('vaccine:edit')"
              type="primary"
              link
              :icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="userStore.hasPerm('vaccine:edit')"
              type="danger"
              link
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <span v-if="!userStore.hasPerm('vaccine:edit')" class="form-tip">只读</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无免疫记录" />
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
      :title="dialogMode === 'edit' ? '编辑免疫记录' : '新增免疫记录'"
      width="540px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="宠物" prop="petId">
          <el-select
            v-model="form.petId"
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
        </el-form-item>

        <el-form-item label="疫苗类型" prop="vaccineType">
          <el-input
            v-model="form.vaccineType"
            placeholder="例如：狂犬疫苗、猫三联"
            maxlength="30"
          />
        </el-form-item>

        <el-form-item label="接种日期" prop="inoculateDate">
          <el-date-picker
            v-model="form.inoculateDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择接种日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="下次到期日" prop="nextDueDate">
          <el-date-picker
            v-model="form.nextDueDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="留空则自动计算"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="免疫周期" prop="cycleDays">
          <el-input-number
            v-model="form.cycleDays"
            :min="1"
            :max="3650"
            :controls="true"
            placeholder="默认 365 天"
            style="width: 100%"
          />
          <div class="form-tip">
            下次到期日留空时，后端按接种日期 + 免疫周期自动计算；免疫周期不填默认
            365 天（即 1 年）。填了下次到期日后，本项不参与计算。
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
