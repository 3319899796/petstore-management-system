// 极简下载器：node fetch.js <url> <目标路径>
// 用 DSH 自带的 node（沙箱里 curl/schannel 拿不到凭据，只有 node 的 fetch 能出网）
const fs = require('fs')
const path = require('path')

async function main() {
  const [url, out] = process.argv.slice(2)
  if (!url || !out) {
    console.error('用法: node fetch.js <url> <目标路径>')
    process.exit(1)
  }
  fs.mkdirSync(path.dirname(out), { recursive: true })
  if (fs.existsSync(out) && fs.statSync(out).size > 0) {
    console.log('已存在，跳过：' + out + ' (' + fs.statSync(out).size + ' B)')
    return
  }
  console.log('开始下载 ' + url)
  const res = await fetch(url, { redirect: 'follow' })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const total = Number(res.headers.get('content-length') || 0)
  const tmp = out + '.part'
  const fh = fs.createWriteStream(tmp)
  let got = 0
  let lastPct = -10
  const reader = res.body.getReader()
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    got += value.length
    if (!fh.write(Buffer.from(value))) await new Promise((r) => fh.once('drain', r))
    const pct = total ? Math.floor((got / total) * 100) : 0
    if (pct - lastPct >= 10) {
      lastPct = pct
      console.log(`  ${pct}%  ${(got / 1048576).toFixed(1)} MB / ${(total / 1048576).toFixed(1)} MB`)
    }
  }
  await new Promise((r) => fh.end(r))
  fs.renameSync(tmp, out)
  console.log('完成：' + out + ' (' + fs.statSync(out).size + ' B)')
}

main().catch((e) => {
  console.error('下载失败：' + e.message)
  process.exit(1)
})
