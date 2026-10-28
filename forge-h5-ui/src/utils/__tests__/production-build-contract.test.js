import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const projectFile = (name) => fileURLToPath(new URL(`../../../${name}`, import.meta.url))

for (const envFile of ['.env.production', '.env.test']) {
  test(`${envFile} keeps H5 assets subpath-safe`, () => {
    const source = readFileSync(projectFile(envFile), 'utf8')

    assert.match(source, /^VITE_PUBLIC_PATH=\.\/$/m)
    assert.match(source, /^VITE_BASE_URL=\/forge-h5$/m)
    assert.doesNotMatch(source, /^VITE_PUBLIC_PATH=\/forge-h5\/?$/m)
  })
}
