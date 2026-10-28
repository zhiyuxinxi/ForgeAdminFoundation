import assert from 'node:assert/strict'
import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

// 静态资源缓存契约（硬性验收）：发布修复必须能稳定送达用户。
// 1) 仅 assets/ 下文件名携带 8-9 位内容哈希的资源才允许 immutable；
// 2) 非哈希 assets / static / 默认静态：Cache-Control no-cache（禁止固定长缓存）；
// 3) 入口 HTML 与所有 SPA fallback HTML：no-store,no-cache,must-revalidate + Pragma:no-cache + Expires:0；
// 4) API 代理 location 不携带静态缓存头；PC/H5 不串线。
//
// 本测试读取仓库运行配置，按配置变更显式执行，不强加每次镜像构建。

function resolveNginxConfPath() {
  const candidates = []
  const explicit = process.env.NGINX_CONF_TEST_PATH
  if (explicit) {
    candidates.push(explicit)
  }
  candidates.push(fileURLToPath(new URL('../../../../docker-forge-admin/nginx.conf', import.meta.url)))
  for (const candidate of candidates) {
    if (existsSync(candidate)) {
      return candidate
    }
  }
  throw new Error(
    `nginx.conf 不可读：NGINX_CONF_TEST_PATH(${explicit || '未设置'}) 与仓库相对路径均不存在。`
    + '请从完整仓库执行此配置测试，或显式指定NGINX_CONF_TEST_PATH。',
  )
}

const nginxConfPath = resolveNginxConfPath()
const nginxSource = readFileSync(nginxConfPath, 'utf8')

const H5_ASSETS = fileURLToPath(new URL('../../../dist/build/h5/assets', import.meta.url))
const PC_ASSETS = fileURLToPath(new URL('../../../../forge-admin-ui/dist/assets', import.meta.url))

function locationBlock(name) {
  const index = nginxSource.indexOf(name)
  assert.ok(index >= 0, `location ${name} must exist`)
  const end = nginxSource.indexOf('\n    }', index)
  return nginxSource.slice(index, end)
}

function extractHashRegex() {
  const lineMatch = nginxSource.match(/location ~\* "?(\^\/\(forge\|forge-h5\)\/assets\/[^ "\n]+)"? \{/)
  assert.ok(lineMatch, 'hashed-assets regex location must exist')
  return new RegExp(lineMatch[1])
}

// ---------- 0) Nginx 语法守卫：含量词正则必须整体引号包裹 ----------

test('every regex location pattern is wrapped in double quotes (nginx syntax safe)', () => {
  // 真实 nginx -t 曾因 location 正则含 {8,9} 未加引号报 unknown directive。
  // Nginx 解析器把未加引号的 { 当块边界，因此含量词（{n,m}）的正则必须整体引号包裹。
  const lines = nginxSource.split('\n').filter(line => /location ~\* /.test(line))
  assert.ok(lines.length >= 2, 'regex locations must exist')
  for (const line of lines) {
    assert.match(line, /location ~\* "[^"]+" \{/, `regex location must be quoted: ${line.trim()}`)
    assert.doesNotMatch(line, /location ~\* [^"]+?\{[0-9]/)
  }
  // 具体断言 {8,9} 资产正则已整体引号包裹（本次真实语法失败的回归用例）
  assert.match(
    nginxSource,
    /location ~\* "\^\/\(forge\|forge-h5\)\/assets\/\[\^\/\]\*\[\.-\]\[A-Za-z0-9_-\]\{8,9\}\\\.\(js\|css\|woff2\?\|ttf\|otf\|eot\|svg\|png\|jpe\?g\|gif\|webp\|ico\|map\)\$" \{/,
    'assets regex location must be fully quoted',
  )
  // 配置中任何含 {n,m} 量词的行必须位于带引号的正则 location（或为注释），
  // 未加引号的 { 会被 Nginx 当作块边界导致 nginx -t 失败
  for (const line of nginxSource.split('\n')) {
    if (/\{[0-9]+,[0-9]+\}/.test(line) && !line.trim().startsWith('#')) {
      assert.match(line, /location ~\* "[^"]+" \{/, `quantifier line must be a quoted regex location: ${line.trim()}`)
    }
  }
})

// ---------- 1) 哈希资源缓存 ----------

test('hashed-assets location grants immutable with 8-9 char hash rule', () => {
  const block = locationBlock('location ~* "^/(forge|forge-h5)/assets/')
  assert.match(block, /\[A-Za-z0-9_-\]\{8,9\}/, 'hash must be 8-9 base64url chars')
  assert.match(block, /add_header Cache-Control "public, max-age=31536000, immutable" always;/)
  assert.match(block, /add_header X-Content-Type-Options "nosniff" always;/)
})

test('hash regex matches EVERY real H5/PC asset (no missed hashed files)', (t) => {
  const hashRegex = extractHashRegex()
  for (const [label, dir] of [['H5', H5_ASSETS], ['PC', PC_ASSETS]]) {
    if (!existsSync(dir)) {
      t.skip(`${label} dist not built; run build first`)
      continue
    }
    const files = readdirSync(dir)
    assert.ok(files.length > 0, `${label} assets must exist`)
    const missed = files.filter(name => !hashRegex.test(`/forge-h5/assets/${name}`) || !hashRegex.test(`/forge/assets/${name}`))
    assert.deepEqual(missed, [], `${label} hashed assets must all match under both prefixes: ${missed.join(',')}`)
  }
})

test('hash regex does NOT match real non-hashed files (no false positives)', () => {
  const hashRegex = extractHashRegex()
  for (const uri of [
    '/forge-h5/index.html', '/forge/index.html', '/forge/favicon.ico', '/forge-h5/static/logo.png',
    '/forge-h5/static/icons/ai-icon/user.svg', '/forge-h5/static/icons/dynamic-icons.js',
    '/forge/robots.txt', '/forge-h5/manifest.json', '/forge-h5/assets/README.md',
  ]) {
    assert.equal(hashRegex.test(uri), false, `non-hashed ${uri} must not match`)
  }
})

// ---------- 2) 非哈希静态默认 no-cache ----------

test('prefix locations default non-hashed static to no-cache (never fixed long cache)', () => {
  for (const prefix of ['location /forge/', 'location /forge-h5/']) {
    const block = locationBlock(prefix)
    assert.match(block, /add_header Cache-Control "no-cache" always;/, `${prefix} must default to no-cache`)
    assert.doesNotMatch(block, /max-age=31536000|max-age=3600/, `${prefix} must not set fixed long cache`)
  }
})

// ---------- 3) 入口与 SPA fallback HTML 强制不缓存 ----------

test('HTML responses carry no-store triple plus Pragma/Expires', () => {
  const htmlBlock = locationBlock('location ~* "^/(forge|forge-h5)/.*\\.html$"')
  assert.match(htmlBlock, /add_header Cache-Control "no-store, no-cache, must-revalidate" always;/)
  assert.match(htmlBlock, /add_header Pragma "no-cache" always;/)
  assert.match(htmlBlock, /add_header Expires "0" always;/)
})

test('exact entry directory locations force no-cache triple with Pragma/Expires', () => {
  for (const exact of ['location = /forge/', 'location = /forge-h5/']) {
    const block = locationBlock(exact)
    assert.match(block, /add_header Cache-Control "no-store, no-cache, must-revalidate" always;/)
    assert.match(block, /add_header Pragma "no-cache" always;/)
    assert.match(block, /add_header Expires "0" always;/)
  }
})

test('SPA fallback flows to HTML no-cache via try_files internal redirect', () => {
  assert.match(locationBlock('location /forge/'), /try_files \$uri \$uri\/ \/forge\/index\.html;/)
  assert.match(locationBlock('location /forge-h5/'), /try_files \$uri \$uri\/ \/forge-h5\/index\.html;/)
  // fallback 目标 index.html 必然命中 HTML 正则 location（已整体引号包裹）
  assert.match(nginxSource, /location ~\* "\^\/\(forge\|forge-h5\)\/\.\*\\\.html\$" \{/)
})

// ---------- 4) API 不带静态缓存头、location 优先级不串线 ----------

test('API locations carry no static Cache-Control header', () => {
  for (const api of ['location /forge-api/', 'location /forge-h5-api/']) {
    const block = locationBlock(api)
    assert.doesNotMatch(block, /add_header Cache-Control/, `${api} must not set static cache headers`)
  }
})

test('regex locations never match API URIs and PC/H5 do not cross', () => {
  const htmlRegex = /^\/(forge|forge-h5)\/.*\.html$/
  const hashRegex = extractHashRegex()
  for (const uri of ['/forge-api/auth/captcha', '/forge-h5-api/api/flow/task/todo', '/forge-api/api/flow/', '/ws']) {
    assert.equal(htmlRegex.test(uri), false, `HTML regex must not match ${uri}`)
    assert.equal(hashRegex.test(uri), false, `hash regex must not match ${uri}`)
  }
  // PC 与 H5 前缀互不重叠
  assert.ok(!'/forge-h5-api/foo'.startsWith('/forge/'), 'H5 API must not fall into PC prefix')
  assert.ok(!'/forge-api/foo'.startsWith('/forge-h5/'), 'PC API must not fall into H5 prefix')
})

// ---------- 既有移动契约保持 ----------

test('mobile static locations and API routing are preserved', () => {
  assert.match(nginxSource, /location \/forge-h5\/ \{/)
  assert.match(nginxSource, /location \/forge-h5-api\//)
  const appBlock = locationBlock('location /forge-h5-api/ {')
  assert.match(appBlock, /set \$forge_app_backend forge-app:8583;/)
  assert.match(appBlock, /rewrite \^\/forge-h5-api\/\(\.\*\)\$ \/\$1 break;/)
  assert.match(appBlock, /proxy_pass http:\/\/\$forge_app_backend;/)
  assert.match(nginxSource, /proxy_pass http:\/\/forge-flow:8081\/api\/flow\//)
  assert.match(nginxSource, /location = \/forge-h5 \{/)
})
