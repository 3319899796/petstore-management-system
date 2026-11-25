// 接口函数集中管理：每个函数对应 docs/接口清单.md 里的一个后端接口，
// 页面只调用这里的函数，不直接写 axios，方便统一核对路径和参数
import request from './request'

/* ==================== 1. 认证 ==================== */

// 登录（不需要 token）
export function login(data) {
  return request.post('/auth/login', data)
}

// 获取当前登录用户信息
export function getMe() {
  return request.get('/auth/me')
}

// 退出登录（后端只是提示，前端清 token 即可）
export function logout() {
  return request.post('/auth/logout')
}

/* ==================== 2. 宠物 ==================== */

// 宠物分页：params 可传 pageNum/pageSize/keyword/ownerId/breed
export function getPetPage(params) {
  return request.get('/pet/page', { params })
}

// 宠物下拉选项（主人角色只会拿到自己的宠物）
export function getPetOptions() {
  return request.get('/pet/options')
}

export function getPetDetail(petId) {
  return request.get(`/pet/${petId}`)
}

export function createPet(data) {
  return request.post('/pet', data)
}

export function updatePet(data) {
  return request.put('/pet', data)
}

export function deletePet(petId) {
  return request.delete(`/pet/${petId}`)
}

// 上传宠物照片：参数必须是 FormData，字段名 file；返回照片相对路径
export function uploadPetPhoto(formData) {
  return request.post('/pet/upload', formData)
}

/* ==================== 3. 主人 ==================== */

export function getOwnerPage(params) {
  return request.get('/owner/page', { params })
}

export function getOwnerOptions() {
  return request.get('/owner/options')
}

// 主人角色查询自己的主人档案（拿 ownerId）；未绑定会返回 400
export function getMyOwner() {
  return request.get('/owner/me')
}

export function createOwner(data) {
  return request.post('/owner', data)
}

export function updateOwner(data) {
  return request.put('/owner', data)
}

export function deleteOwner(ownerId) {
  return request.delete(`/owner/${ownerId}`)
}

/* ==================== 4. 免疫记录 ==================== */

// 分页：params 可传 pageNum/pageSize/petId/dueSoon
export function getVaccinePage(params) {
  return request.get('/vaccine/page', { params })
}

export function getVaccineByPet(petId) {
  return request.get(`/vaccine/pet/${petId}`)
}

// 7 天内到期提醒列表（看板用），days 默认 7
export function getUpcomingVaccines(days = 7) {
  return request.get('/vaccine/upcoming', { params: { days } })
}

export function createVaccine(data) {
  return request.post('/vaccine', data)
}

export function updateVaccine(data) {
  return request.put('/vaccine', data)
}

export function deleteVaccine(vaccineId) {
  return request.delete(`/vaccine/${vaccineId}`)
}

/* ==================== 5. 就诊记录 ==================== */

// 分页：params 可传 pageNum/pageSize/petId/keyword
export function getMedicalPage(params) {
  return request.get('/medical/page', { params })
}

export function getMedicalByPet(petId) {
  return request.get(`/medical/pet/${petId}`)
}

export function createMedical(data) {
  return request.post('/medical', data)
}

export function updateMedical(data) {
  return request.put('/medical', data)
}

export function deleteMedical(medicalId) {
  return request.delete(`/medical/${medicalId}`)
}

/* ==================== 6. 寄养 ==================== */

// 分页：params 可传 pageNum/pageSize/petId/status
export function getFosterPage(params) {
  return request.get('/foster/page', { params })
}

export function createFoster(data) {
  return request.post('/foster', data)
}

export function updateFoster(data) {
  return request.put('/foster', data)
}

// 审核：{ id, approved, reason }，驳回时 reason 必填
export function auditFoster(data) {
  return request.post('/foster/audit', data)
}

// 结束寄养：只有寄养中（APPROVED）的才能结束
export function finishFoster(fosterId) {
  return request.post(`/foster/${fosterId}/finish`)
}

export function deleteFoster(fosterId) {
  return request.delete(`/foster/${fosterId}`)
}

/* ==================== 7. 领养 ==================== */

// 分页：params 可传 pageNum/pageSize/petId/status；主人只会看到自己提交的申请
export function getAdoptionPage(params) {
  return request.get('/adoption/page', { params })
}

// 提交领养申请：{ petId }
export function createAdoption(data) {
  return request.post('/adoption', data)
}

// 审核：{ id, approved, reason }
export function auditAdoption(data) {
  return request.post('/adoption/audit', data)
}

// 回访登记：{ adoptionId, followUpResult }，仅已通过的申请可登记
export function followUpAdoption(data) {
  return request.post('/adoption/followup', data)
}

export function deleteAdoption(adoptionId) {
  return request.delete(`/adoption/${adoptionId}`)
}

/* ==================== 8. 公告 ==================== */

export function getNoticePage(params) {
  return request.get('/notice/page', { params })
}

export function getNoticeDetail(noticeId) {
  return request.get(`/notice/${noticeId}`)
}

export function createNotice(data) {
  return request.post('/notice', data)
}

export function updateNotice(data) {
  return request.put('/notice', data)
}

export function deleteNotice(noticeId) {
  return request.delete(`/notice/${noticeId}`)
}

// 立即执行疫苗到期提醒扫描，返回本次生成的提醒条数
export function remindNotice() {
  return request.post('/notice/remind')
}

/* ==================== 9. 数据看板 ==================== */

export function getStatOverview() {
  return request.get('/stat/overview')
}

/* ==================== 10. 用户管理 ==================== */

export function getUserPage(params) {
  return request.get('/user/page', { params })
}

// 角色下拉（返回 roleId/roleName/permission）
export function getUserRoles() {
  return request.get('/user/roles')
}

export function createUser(data) {
  return request.post('/user', data)
}

// 编辑：password 留空时不要传这个字段，后端会保持原密码
export function updateUser(data) {
  return request.put('/user', data)
}

// 重置密码为 123456
export function resetUserPassword(userId) {
  return request.post(`/user/${userId}/reset-password`)
}

export function deleteUser(userId) {
  return request.delete(`/user/${userId}`)
}
