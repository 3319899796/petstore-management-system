<script setup>
// 就诊记录页：按宠物/关键字查询，新增、编辑、删除就诊记录（症状必填）
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getMedicalPage,
  getPetOptions,
  createMedical,
  updateMedical,
  deleteMedical
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 工作人员只有 medical:add（可以新增、不能改删），管理员是 *；用"任意一个"控制编辑/删除按钮
const canEdit = computed(() => userStore.hasAnyPerm(['medical:add', 'medical:edit']))

const loading = ref(false)
const list = ref([])
const total = ref(0)
const petOptions = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  petId: null,
  keyword: ''
})

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  medicalId: null,
  petId: null,
  symptom: '',
  diagnosis: '',
  visitDate: ''
})

const rules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  symptom: [
    { required: true, message: '请输入症状描述', trigger: 'blur' },
    { max: 255, message: '症状不能超过 255 个字', trigger: 'blur' }
  ],
  diagnosis: [{ max: 255, message: '诊断不能超过 255 个字', trigger: 'blur' }],
  visitDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }]
}

function todayStr() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

async function loadList() {
  loading.value = true
  try {
    const data = await getMedicalPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      petId: query.petId || undefined,
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
  query.keyword = ''
  query.pageNum = 1
  loadList()
}

function resetForm() {
  Object.assign(form, {
    medicalId: null,
    petId: null,
    symptom: '',
    diagnosis: '',
    visitDate: ''
  })
  if (formRef.value) formRef.value.clearValidate()
}

function handleCreate() {
  resetForm()
  dialogMode.value = 'create'
  form.visitDate = todayStr()
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  dialogMode.value = 'edit'
  Object.assign(form, {
    medicalId: row.medicalId,
    petId: row.petId,
    symptom: row.symptom || '',
    diagnosis: row.diagnosis || '',
    visitDate: row.visitDate
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
      symptom: form.symptom.trim(),
      diagnosis: form.diagnosis.trim(),
      visitDate: form.visitDate
    }
    if (dialogMode.value === 'edit') {
      payload.medicalId = form.medicalId
      await updateMedical(payload)
      ElMessage.success('就诊记录修改成功')
    } else {
      await createMedical(payload)
      ElMessage.success('就诊记录新增成功')
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
      `确定删除「${row.petName}」在 ${row.visitDate} 的就诊记录吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deleteMedical(row.medicalId)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 请求层统一提示
  }
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

        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="症状 / 诊断"
            clearable
            style="width: 220px"
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
            v-if="userStore.hasPerm('medical:add')"
            type="primary"
            :icon="Plus"
            @click="handleCreate"
          >
            新增就诊记录
          </el-button>
        </div>
        <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="petName" label="宠物名称" min-width="110" />
        <el-table-column prop="ownerName" label="主人" min-width="110" />
        <el-table-column prop="symptom" label="症状" min-width="180" show-overflow-tooltip />
        <el-table-column prop="diagnosis" label="诊断" min-width="180">
          <template #default="{ row }">{{ row.diagnosis || '—' }}</template>
        </el-table-column>
        <el-table-column prop="visitDate" label="就诊日期" width="120" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
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
              type="danger"
              link
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <span v-if="!canEdit" class="form-tip">只读</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无就诊记录" />
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
      :title="dialogMode === 'edit' ? '编辑就诊记录' : '新增就诊记录'"
      width="560px"
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

        <el-form-item label="症状" prop="symptom">
          <el-input
            v-model="form.symptom"
            type="textarea"
            :rows="3"
            placeholder="必填，例如：食欲不振、呕吐、发热"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="诊断" prop="diagnosis">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="3"
            placeholder="选填，例如：急性肠胃炎"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="就诊日期" prop="visitDate">
          <el-date-picker
            v-model="form.visitDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择就诊日期"
            style="width: 100%"
          />
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
