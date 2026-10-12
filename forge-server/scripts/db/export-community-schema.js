#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, copyFileSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const config = await readConfig()
const migrationDir = resolve(scriptDir, config.migrationDir)
const exportDir = resolve(scriptDir, config.exportDir, 'migration')
const dryRun = process.argv.includes('--dry-run')
const migrationPattern = /^V\d+\.\d+\.\d+__[a-z0-9_]+\.sql$/

if (!existsSync(migrationDir)) {
  fail(`migration directory not found: ${migrationDir}`)
}

const files = readdirSync(migrationDir)
  .filter(file => file.endsWith('.sql'))
  .sort()

const invalidFiles = files.filter(file => !migrationPattern.test(file))
if (invalidFiles.length > 0) {
  fail(`invalid migration file names:\n${invalidFiles.map(file => `- ${file}`).join('\n')}`)
}

if (!dryRun) {
  rmSync(exportDir, { recursive: true, force: true })
  mkdirSync(exportDir, { recursive: true })
  for (const file of files) {
    copyFileSync(join(migrationDir, file), join(exportDir, file))
  }
}

const summary = {
  type: 'schema',
  dryRun,
  source: migrationDir,
  target: exportDir,
  files
}

writeFileSync(resolve(scriptDir, config.exportDir, 'schema-summary.json'), `${JSON.stringify(summary, null, 2)}\n`)
console.log(JSON.stringify(summary, null, 2))

async function readConfig() {
  const configPath = resolve(scriptDir, 'community-db.config.json')
  return JSON.parse(await import('node:fs/promises').then(fs => fs.readFile(configPath, 'utf8')))
}

function fail(message) {
  console.error(message)
  process.exit(1)
}
