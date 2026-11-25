// 冒烟脚本：用 node 自带的 fetch 依次打后端接口，打印关键结果。
// 用法：node smoke.mjs   （后端需已在 8080 运行）
const BASE = process.env.BASE || 'http://127.0.0.1:8080/api';

async function call(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  let data;
  try { data = await res.json(); } catch { data = await res.text(); }
  return { status: res.status, data };
}

function line(title) { console.log('\n=== ' + title + ' ==='); }

async function login(username, password = '123456') {
  const r = await call('POST', '/auth/login', { body: { username, password } });
  const ok = r.data && r.data.code === 200;
  console.log(`login ${username}: http=${r.status} code=${r.data?.code} role=${r.data?.data?.roleName} perms=${r.data?.data?.permissions?.length ?? 0}`);
  return ok ? r.data.data.token : null;
}

const admin = await login('admin');
const staff = await login('staff');
const owner = await login('owner');

line('GET /auth/me (admin)');
{
  const r = await call('GET', '/auth/me', { token: admin });
  console.log('http=' + r.status, JSON.stringify(r.data.data));
}

line('GET /pet/page (admin)');
{
  const r = await call('GET', '/pet/page?pageNum=1&pageSize=3', { token: admin });
  console.log('http=' + r.status, 'total=' + r.data.data.total);
  console.log(r.data.data.records.map(p => `${p.petName}/${p.breed}/${p.ownerName}`).join(' | '));
}

line('GET /pet/page (owner 数据隔离)');
{
  const r = await call('GET', '/pet/page?pageNum=1&pageSize=10', { token: owner });
  console.log('http=' + r.status, 'total=' + r.data.data?.total);
  console.log((r.data.data?.records || []).map(p => p.petName).join(' | '));
}

line('GET /stat/overview (admin)');
{
  const r = await call('GET', '/stat/overview', { token: admin });
  const d = r.data.data;
  console.log('http=' + r.status);
  console.log(JSON.stringify({
    petTotal: d.petTotal, ownerTotal: d.ownerTotal, vaccineDue7: d.vaccineDue7,
    vaccineOverdue: d.vaccineOverdue, adoptionRate: d.adoptionRate,
    foster: [d.fosterPending, d.fosterActive], adoption: [d.adoptionPending, d.adoptionApproved, d.adoptionTotal],
    dueDistribution: d.dueDistribution, monthlyPets: d.monthlyPets,
  }, null, 0));
}

line('权限拦截：staff 调 admin 专属 /user/page');
{
  const r = await call('GET', '/user/page', { token: staff });
  console.log('http=' + r.status, 'code=' + r.data.code, 'message=' + r.data.message);
}

line('GET /user/page (admin)');
{
  const r = await call('GET', '/user/page?pageNum=1&pageSize=10', { token: admin });
  console.log('http=' + r.status, 'total=' + r.data.data.total,
    r.data.data.records.map(u => `${u.username}:${u.roleName}(${u.phone})`).join(' | '));
}

line('GET /vaccine/page?dueSoon=true');
{
  const r = await call('GET', '/vaccine/page?dueSoon=true&pageNum=1&pageSize=10', { token: staff });
  console.log('http=' + r.status, 'total=' + r.data.data.total);
  console.log(r.data.data.records.map(v => `${v.petName}/${v.vaccineType}/${v.nextDueDate}/${v.daysLeft}天`).join(' | '));
}

line('POST /notice/remind 生成到期提醒');
{
  const r = await call('POST', '/notice/remind', { token: admin });
  console.log('http=' + r.status, 'code=' + r.data.code, 'message=' + r.data.message, 'data=' + JSON.stringify(r.data.data));
  const r2 = await call('POST', '/notice/remind', { token: admin });
  console.log('第二次（验幂等）:', 'code=' + r2.data.code, 'data=' + JSON.stringify(r2.data.data));
}

line('GET /notice/page');
{
  const r = await call('GET', '/notice/page?pageNum=1&pageSize=10', { token: admin });
  console.log('http=' + r.status, 'total=' + r.data.data.total);
  console.log(r.data.data.records.map(n => n.title).join(' | '));
}

line('GET /foster/page + /adoption/page');
{
  const f = await call('GET', '/foster/page?pageNum=1&pageSize=10', { token: staff });
  console.log('foster total=' + f.data.data.total, f.data.data.records.map(x => `${x.petName}/${x.statusText}`).join(' | '));
  const a = await call('GET', '/adoption/page?pageNum=1&pageSize=10', { token: admin });
  console.log('adoption total=' + a.data.data.total, a.data.data.records.map(x => `${x.petName}/${x.statusText}`).join(' | '));
}

console.log('\n完成。');
