<script setup>
// 宠物档案页：分页查询、新增/编辑（含照片上传）、删除；主人角色只能看到并新建自己的宠物
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getPetPage,
  getOwnerOptions,
  getMyOwner,
  createPet,
  updatePet,
  deletePet,
  uploadPetPhoto
} from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 主人角色（roleId=3）的新增逻辑和工作人员不同，单独用一个变量判断
const isOwner = computed(() => userStore.roleId === 3)

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  ownerId: null
})

const ownerOptions = ref([])
// 主人自己的档案（含 ownerId），新增宠物时自动使用
const myOwner = ref(null)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref(null)
// 主人角色不能选主人，用一个只读文案显示"当前给谁建档"
const ownerLabel = ref('')

// 表单数据：字段名严格按接口清单里的 PetDTO
const form = reactive({
  petId: null,
  petName: '',
  chipNo: '',
  breed: '',
  ownerId: null,
  photoPath: ''
})

const rules = {
  petName: [
    { required: true, message: '请输入宠物名称', trigger: 'blur' },
    { max: 30, message: '宠物名称不能超过 30 个字', trigger: 'blur' }
  ],
  chipNo: [
    { required: true, message: '请输入芯片号', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9-]{4,30}$/,
      message: '芯片号只能包含字母、数字、短横线，长度 4~30 位',
      trigger: 'blur'
    }
  ],
  ownerId: [{ required: true, message: '请选择宠物主人', trigger: 'change' }]
}

// 把后端返回的相对路径（/files/2026/11/xx.jpg）拼成浏览器可访问的地址
function photoUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  const base = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '')
  return base + path
}

async function loadList() {
  loading.value = true
  try {
    const data = await getPetPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      // 空字符串不传，避免后端把空关键字也拼进 SQL
      keyword: query.keyword.trim() || undefined,
      ownerId: query.ownerId || undefined
    })
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    // 接口失败时不保留旧数据，避免误以为查询成功
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 仅工作人员/管理员有 owner:list 权限，可以加载主人下拉
async function loadOwnerOptions() {
  if (!userStore.hasPerm('owner:list')) return
  try {
    ownerOptions.value = await getOwnerOptions()
  } catch (e) {
    ownerOptions.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.ownerId = null
  query.pageNum = 1
  loadList()
}

function handleSizeChange() {
  query.pageNum = 1
  loadList()
}

// 清空表单，弹窗关闭时调用
function resetForm() {
  Object.assign(form, {
    petId: null,
    petName: '',
    chipNo: '',
    breed: '',
    ownerId: null,
    photoPath: ''
  })
  ownerLabel.value = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

async function handleCreate() {
  resetForm()
  dialogMode.value = 'create'

  if (isOwner.value) {
    // 主人新增宠物时后端要求 ownerId 必须等于自己的档案 id，先查 /owner/me
    try {
      myOwner.value = myOwner.value || (await getMyOwner())
      form.ownerId = myOwner.value.ownerId
      ownerLabel.value = `${myOwner.value.ownerName}（ID:${myOwner.value.ownerId}）`
    } catch (e) {
      // 未绑定主人档案时后端会返回 400，请求层已经弹出"请联系店员"之类提示
      return
    }
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  dialogMode.value = 'edit'
  Object.assign(form, {
    petId: row.petId,
    petName: row.petName,
    chipNo: row.chipNo,
    breed: row.breed || '',
    ownerId: row.ownerId,
    photoPath: row.photoPath || ''
  })
  if (isOwner.value) {
    ownerLabel.value = `${row.ownerName}（ID:${row.ownerId}）`
  }
  dialogVisible.value = true
}

// el-upload 上传前校验：类型和大小与后端 FileStorageService 的限制保持一致
function beforeUpload(file) {
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) {
    ElMessage.error('只支持 JPG、PNG、GIF、WEBP 格式的图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 自定义上传：不用 el-upload 默认的 action，而是走我们封装好的 axios（这样会自动带 token）
async function handleUpload(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  uploading.value = true
  try {
    const path = await uploadPetPhoto(formData)
    form.photoPath = path
    ElMessage.success('照片上传成功')
  } catch (e) {
    // 失败提示已由请求拦截器统一弹出
  } finally {
    uploading.value = false
  }
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
      petName: form.petName.trim(),
      chipNo: form.chipNo.trim(),
      breed: form.breed.trim(),
      ownerId: form.ownerId,
      photoPath: form.photoPath || null
    }
    if (dialogMode.value === 'edit') {
      payload.petId = form.petId
      await updatePet(payload)
      ElMessage.success('宠物信息修改成功')
    } else {
      await createPet(payload)
      ElMessage.success('宠物建档成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // 请求层已提示后端 message（例如芯片号重复）
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除宠物「${row.petName}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  try {
    await deletePet(row.petId)
    ElMessage.success('删除成功')
    // 如果当前页只有这一条，删除后回退一页，避免出现空页
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    loadList()
  } catch (e) {
    // 被疫苗/就诊等记录引用时，后端会返回"该数据已被其它记录引用，无法删除"
  }
}

onMounted(() => {
  loadList()
  loadOwnerOptions()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="宠物名称 / 芯片号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <!-- 主人角色没有 owner:list 权限，不显示主人筛选（后端也会自动只看自己） -->
        <el-form-item v-if="userStore.hasPerm('owner:list')" label="宠物主人">
          <el-select
            v-model="query.ownerId"
            placeholder="全部主人"
            clearable
            filterable
            style="width: 220px"
          >
            <el-option
              v-for="owner in ownerOptions"
              :key="owner.ownerId"
              :label="`${owner.ownerName}（ID:${owner.ownerId}）`"
              :value="owner.ownerId"
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
            v-if="userStore.hasPerm('pet:add')"
            type="primary"
            :icon="Plus"
            @click="handleCreate"
          >
            新增宠物
          </el-button>
        </div>
        <el-button :icon="RefreshLeft" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="照片" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.photoPath"
              :src="photoUrl(row.photoPath)"
              :preview-src-list="[photoUrl(row.photoPath)]"
              preview-teleported
              fit="cover"
              class="table-photo"
            />
            <span v-else class="form-tip">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="petName" label="宠物名称" min-width="110" />
        <el-table-column prop="chipNo" label="芯片号" min-width="150" />
        <el-table-column prop="breed" label="品种" min-width="110">
          <template #default="{ row }">{{ row.breed || '—' }}</template>
        </el-table-column>
        <el-table-column label="宠物主人" min-width="160">
          <template #default="{ row }">
            <div>{{ row.ownerName }}</div>
            <div class="cell-sub">{{ row.ownerPhone || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="建档时间" width="170" align="center" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="userStore.hasPerm('pet:edit')"
              type="primary"
              link
              :icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="userStore.hasPerm('pet:delete')"
              type="danger"
              link
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <span
              v-if="!userStore.hasPerm('pet:edit') && !userStore.hasPerm('pet:delete')"
              class="form-tip"
            >
              无操作权限
            </span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无宠物档案，点击左上角「新增宠物」开始建档" />
        </template>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑宠物' : '新增宠物'"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="宠物名称" prop="petName">
          <el-input v-model="form.petName" placeholder="例如：豆豆" maxlength="30" show-word-limit />
        </el-form-item>

        <el-form-item label="芯片号" prop="chipNo">
          <el-input
            v-model="form.chipNo"
            placeholder="字母、数字、短横线，4~30 位"
            maxlength="30"
          />
        </el-form-item>

        <el-form-item label="品种" prop="breed">
          <el-input v-model="form.breed" placeholder="例如：金毛、英短（可不填）" maxlength="30" />
        </el-form-item>

        <el-form-item label="宠物主人" prop="ownerId">
          <!-- 工作人员/管理员可以从全部主人里选 -->
          <el-select
            v-if="!isOwner"
            v-model="form.ownerId"
            placeholder="请选择主人"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="owner in ownerOptions"
              :key="owner.ownerId"
              :label="`${owner.ownerName}（ID:${owner.ownerId}）`"
              :value="owner.ownerId"
            />
          </el-select>
          <!-- 主人只能给自己建档，显示只读文案 -->
          <el-input v-else :model-value="ownerLabel" disabled />
        </el-form-item>

        <el-form-item label="宠物照片">
          <el-upload
            class="photo-uploader"
            :show-file-list="false"
            :http-request="handleUpload"
            :before-upload="beforeUpload"
            :disabled="uploading"
            accept="image/jpeg,image/png,image/gif,image/webp"
          >
            <div class="photo-box">
              <img v-if="form.photoPath" :src="photoUrl(form.photoPath)" class="photo-preview" />
              <div v-else class="photo-placeholder">
                <el-icon :size="24"><Plus /></el-icon>
                <span>点击上传</span>
              </div>
              <div v-if="uploading" class="photo-mask">上传中...</div>
            </div>
          </el-upload>
          <div class="form-tip">
            支持 JPG / PNG / GIF / WEBP，大小不超过 5MB；照片会保存为服务器上的相对路径。
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

<style scoped>
.table-photo {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.photo-box {
  position: relative;
  width: 120px;
  height: 120px;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}

.photo-box:hover {
  border-color: #409eff;
}

.photo-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
}

.photo-mask {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}
</style>
