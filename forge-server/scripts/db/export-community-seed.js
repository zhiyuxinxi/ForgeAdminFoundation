#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, copyFileSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const config = await readConfig()
const seedDir = resolve(scriptDir, config.seedDir)
const exportDir = resolve(scriptDir, config.exportDir, 'seed')
const dryRun = process.argv.includes('--dry-run')
const allowedPrefixes = ['R__', 'D__', 'O__']

if (!existsSync(seedDir)) {
  fail(`seed directory not found: ${seedDir}`)
}

const files = listSqlFiles(seedDir)
const invalidFiles = files.filter(file => !allowedPrefixes.some(prefix => file.name.startsWith(prefix)))
if (invalidFiles.length > 0) {
  fail(`invalid seed file names:\n${invalidFiles.map(file => `- ${file.relativePath}`).join('\n')}`)
}

if (!dryRun) {
  rmSync(exportDir, { recursive: true, force: true })
  mkdirSync(exportDir, { recursive: true })
  for (const file of files) {
    const target = join(exportDir, file.relativePath)
    mkdirSync(dirname(target), { recursive: true })
    copyFileSync(file.absolutePath, target)
  }
}

const summary = {
  type: 'seed',
  dryRun,
  source: seedDir,
  target: exportDir,
  allowTables: config.allowTables,
  denyTables: config.denyTables,
  files: files.map(file => file.relativePath)
}

writeFileSync(resolve(scriptDir, config.exportDir, 'seed-summary.json'), `${JSON.stringify(summary, null, 2)}\n`)
console.log(JSON.stringify(summary, null, 2))

function listSqlFiles(root, base = root) {
  return readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = join(root, entry.name)
    if (entry.isDirectory()) {
      return listSqlFiles(absolutePath, base)
    }
    if (!entry.name.endsWith('.sql')) {
      return []
    }
    return [{
      name: entry.name,
      absolutePath,
      relativePath: absolutePath.slice(base.length + 1)
    }]
  }).sort((a, b) => a.relativePath.localeCompare(b.relativePath))
}

async function readConfig() {
  const configPath = resolve(scriptDir, 'community-db.config.json')
  return JSON.parse(await import('node:fs/promises').then(fs => fs.readFile(configPath, 'utf8')))
}

function fail(message) {
  console.error(message)
  process.exit(1)
}
