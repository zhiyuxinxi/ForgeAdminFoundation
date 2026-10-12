#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const config = JSON.parse(readFileSync(resolve(scriptDir, 'community-db.config.json'), 'utf8'))
const target = resolve(process.cwd(), process.argv[2] || resolve(scriptDir, config.exportDir))

if (!existsSync(target)) {
  console.log(JSON.stringify({ target, checkedFiles: 0, findings: [] }, null, 2))
  process.exit(0)
}

const files = listSqlFiles(target)
const sensitiveWords = config.sensitiveColumns.map(word => new RegExp(`\\b${escapeRegExp(word)}\\b`, 'i'))
const sensitivePatterns = config.sensitivePatterns.map(pattern => new RegExp(pattern, 'i'))
const findings = []

for (const file of files) {
  const content = readFileSync(file, 'utf8')
  const lines = content.split('\n')
  lines.forEach((line, index) => {
    const matchedWord = sensitiveWords.find(pattern => pattern.test(line))
    const matchedPattern = sensitivePatterns.find(pattern => pattern.test(line))
    if (matchedWord || matchedPattern) {
      findings.push({ file, line: index + 1, text: line.trim().slice(0, 160) })
    }
  })
}

const summary = { target, checkedFiles: files.length, findings }
console.log(JSON.stringify(summary, null, 2))

if (findings.length > 0) {
  process.exit(1)
}

function listSqlFiles(root) {
  return readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = join(root, entry.name)
    if (entry.isDirectory()) {
      return listSqlFiles(absolutePath)
    }
    return entry.name.endsWith('.sql') ? [absolutePath] : []
  })
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
