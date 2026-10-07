const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const ROOT = path.resolve(__dirname, '..')
const PROJECTS = ['brew-miniapp', 'city-miniapp', 'habit-miniapp']
const ALLOWED_WXML_TAGS = new Set(['block', 'button', 'image', 'input', 'scroll-view', 'text', 'view'])
const SOURCE_EXTENSIONS = new Set(['.js', '.json', '.wxml', '.wxss'])

let passed = 0

function test(name, fn) {
  try {
    fn()
    passed += 1
    console.log(`✓ ${name}`)
  } catch (error) {
    console.error(`✗ ${name}`)
    throw error
  }
}

function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const fullPath = path.join(directory, entry.name)
    return entry.isDirectory() ? walk(fullPath) : [fullPath]
  })
}

function readText(file) {
  return fs.readFileSync(file, 'utf8')
}

function createRuntime() {
  const storage = new Map()
  const calls = { toast: [], vibration: [] }
  const wx = {
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, structuredClone(value)),
    removeStorageSync: key => storage.delete(key),
    showToast: options => calls.toast.push(options),
    vibrateShort: options => calls.vibration.push(options)
  }

  return { storage, calls, wx }
}

function loadPage(project, runtime) {
  let definition
  const filename = path.join(ROOT, project, 'pages', 'index', 'index.js')
  vm.runInNewContext(readText(filename), {
    Page: page => { definition = page },
    wx: runtime.wx,
    Date,
    console
  }, { filename })

  assert.ok(definition, `${project} must register a Page`)
  const page = { ...definition, data: structuredClone(definition.data) }
  page.setData = patch => {
    Object.entries(patch).forEach(([key, value]) => { page.data[key] = value })
  }
  definition.onLoad.call(page)
  return page
}

function event(dataset = {}, value = '') {
  return { currentTarget: { dataset }, detail: { value } }
}

test('project configs, scripts and assets are valid', () => {
  for (const project of PROJECTS) {
    const directory = path.join(ROOT, project)
    const files = walk(directory)
    const appConfig = JSON.parse(readText(path.join(directory, 'app.json')))
    const projectConfig = JSON.parse(readText(path.join(directory, 'project.config.json')))

    assert.deepEqual(appConfig.pages, ['pages/index/index'])
    assert.equal(projectConfig.compileType, 'miniprogram')
    assert.equal(projectConfig.miniprogramRoot, './')

    for (const file of files.filter(item => SOURCE_EXTENSIONS.has(path.extname(item)))) {
      const prefix = fs.readFileSync(file).subarray(0, 3)
      assert.notDeepEqual([...prefix], [0xef, 0xbb, 0xbf], `${file} must not contain a UTF-8 BOM`)
    }

    for (const extension of ['js', 'json', 'wxml', 'wxss']) {
      assert.ok(files.includes(path.join(directory, 'pages', 'index', `index.${extension}`)))
    }

    const hero = fs.readFileSync(path.join(directory, 'assets', 'hero.png'))
    assert.equal(hero.subarray(1, 4).toString(), 'PNG')
    assert.ok(hero.length < 500 * 1024, `${project} hero should stay below 500 KB`)

    const packageBytes = files.reduce((sum, file) => sum + fs.statSync(file).size, 0)
    assert.ok(packageBytes < 2 * 1024 * 1024, `${project} should stay below the 2 MB main-package limit`)
  }
})

test('WXML uses supported components, balanced tags and existing handlers', () => {
  for (const project of PROJECTS) {
    const base = path.join(ROOT, project, 'pages', 'index', 'index')
    const wxml = readText(`${base}.wxml`)
    const runtime = createRuntime()
    const page = loadPage(project, runtime)
    const stack = []
    const tags = wxml.matchAll(/<\s*(\/)?\s*([a-z][\w-]*)([^>]*)>/g)

    for (const match of tags) {
      const [, closing, tag, attributes] = match
      assert.ok(ALLOWED_WXML_TAGS.has(tag), `${project} contains unsupported <${tag}>`)
      if (closing) {
        assert.equal(stack.pop(), tag, `${project} has mismatched </${tag}>`)
      } else if (!attributes.trimEnd().endsWith('/')) {
        stack.push(tag)
      }
    }
    assert.deepEqual(stack, [], `${project} has unclosed WXML tags`)

    for (const match of wxml.matchAll(/(?:bindtap|catchtap|bindinput)="([\w]+)"/g)) {
      assert.equal(typeof page[match[1]], 'function', `${project} is missing handler ${match[1]}`)
    }

    const wxss = readText(`${base}.wxss`)
    assert.equal((wxss.match(/{/g) || []).length, (wxss.match(/}/g) || []).length)
  }
})

test('coffee ordering supports variants, quantities, checkout and persistence', () => {
  const runtime = createRuntime()
  const page = loadPage('brew-miniapp', runtime)

  page.openSku(event({ id: 1 }))
  page.chooseOption(event({ type: 'sugar', value: '半糖' }))
  page.addToCart()
  page.openSku(event({ id: 1 }))
  page.chooseOption(event({ type: 'temperature', value: '热' }))
  page.addToCart()
  assert.equal(page.data.cart.length, 2)
  assert.equal(page.data.cartCount, 2)

  const firstKey = page.data.cart[0].key
  page.changeQty(event({ key: firstKey, delta: 1 }))
  assert.equal(page.data.cartCount, 3)
  page.changeQty(event({ key: firstKey, delta: -1 }))
  assert.equal(page.data.cartCount, 2)

  page.checkout()
  assert.equal(page.data.activeTab, 'orders')
  assert.equal(page.data.cart.length, 0)
  assert.equal(page.data.order.status, '制作中')
  assert.ok(runtime.storage.has('brew_order'))

  const reloaded = loadPage('brew-miniapp', runtime)
  assert.equal(reloaded.data.order.no, page.data.order.no)
  reloaded.clearOrder()
  assert.equal(reloaded.data.order, null)
})

test('city discovery supports filters, favorites, booking and ticket removal', () => {
  const runtime = createRuntime()
  const page = loadPage('city-miniapp', runtime)

  page.selectFilter(event({ filter: '展览' }))
  assert.equal(page.data.events.length, 1)
  assert.equal(page.data.events[0].type, '展览')

  page.toggleLike(event({ id: 1 }))
  assert.equal(page.data.favorites.length, 1)
  page.openEvent(event({ id: 1 }))
  page.book()
  assert.equal(page.data.tickets.length, 1)
  assert.equal(page.data.activeTab, 'tickets')

  page.openEvent(event({ id: 1 }))
  page.book()
  assert.equal(page.data.tickets.length, 1, 'duplicate booking must not create another ticket')
  assert.equal(runtime.calls.toast.at(-1).title, '已在票夹中')

  const reloaded = loadPage('city-miniapp', runtime)
  assert.equal(reloaded.data.favorites.length, 1)
  assert.equal(reloaded.data.tickets.length, 1)
  reloaded.removeTicket(event({ id: 1 }))
  assert.equal(reloaded.data.tickets.length, 0)
})

test('habit tracker supports check-in, creation, deletion, reset and persistence', () => {
  const runtime = createRuntime()
  const page = loadPage('habit-miniapp', runtime)
  const originalCount = page.data.habits.length

  page.toggleHabit(event({ id: 2 }))
  assert.equal(page.data.completed, 3)
  assert.equal(runtime.calls.vibration.length, 1)

  page.addHabit()
  assert.equal(page.data.habits.length, originalCount)
  assert.equal(runtime.calls.toast.at(-1).title, '先写下习惯名称')

  page.choosePreset(event({ index: 2 }))
  page.onNameInput(event({}, '记录今日灵感'))
  page.addHabit()
  assert.equal(page.data.habits.length, originalCount + 1)
  assert.equal(page.data.habits.at(-1).icon, '✍️')

  const createdId = page.data.habits.at(-1).id
  const reloaded = loadPage('habit-miniapp', runtime)
  assert.ok(reloaded.data.habits.some(item => item.id === createdId))
  reloaded.deleteHabit(event({ id: createdId }))
  assert.equal(reloaded.data.habits.length, originalCount)
  reloaded.resetData()
  assert.equal(reloaded.data.habits.length, originalCount)
})

test('multi-merchant MVP contains deployable API, admin and split miniapp flows', () => {
  const required = [
    'apps/api/pom.xml',
    'apps/api/src/main/resources/db/migration/V1__schema.sql',
    'apps/api/src/main/resources/db/demo/R__demo_data.sql',
    'apps/admin/package.json',
    'deploy/docker-compose.yml',
    'apps/miniapp/pages/login/index.js',
    'apps/miniapp/pages/menu/index.js',
    'apps/miniapp/pages/cart/index.js',
    'apps/miniapp/pages/confirm/index.js',
    'apps/miniapp/pages/orders/index.js',
    'apps/miniapp/pages/order-detail/index.js'
  ]
  required.forEach(file => assert.ok(fs.existsSync(path.join(ROOT, file)), `${file} must exist`))
  const app = JSON.parse(readText(path.join(ROOT, 'apps/miniapp/app.json')))
  assert.ok(app.pages.includes('pages/confirm/index'))
  assert.ok(app.pages.includes('pages/order-detail/index'))
  const api = readText(path.join(ROOT, 'apps/miniapp/utils/api.js'))
  assert.match(api, /Authorization/)
  const order = readText(path.join(ROOT, 'apps/miniapp/pages/confirm/index.js'))
  assert.match(order, /X-Idempotency-Key/)

  const allowed = new Set([...ALLOWED_WXML_TAGS, 'picker'])
  for (const page of app.pages) {
    const base = path.join(ROOT, 'apps/miniapp', page)
    for (const extension of ['js', 'json', 'wxml', 'wxss']) assert.ok(fs.existsSync(`${base}.${extension}`), `${page}.${extension} must exist`)
    const wxml = readText(`${base}.wxml`)
    const js = readText(`${base}.js`)
    const stack = []
    for (const match of wxml.matchAll(/<\s*(\/)?\s*([a-z][\w-]*)([^>]*)>/g)) {
      const [, closing, tag, attributes] = match
      assert.ok(allowed.has(tag), `${page} contains unsupported <${tag}>`)
      if (closing) assert.equal(stack.pop(), tag, `${page} has mismatched </${tag}>`)
      else if (!attributes.trimEnd().endsWith('/')) stack.push(tag)
    }
    assert.deepEqual(stack, [], `${page} has unclosed WXML tags`)
    for (const match of wxml.matchAll(/(?:bindtap|catchtap|bindchange)="([\w]+)"/g)) {
      assert.match(js, new RegExp(`\\b${match[1]}\\s*\\(`), `${page} is missing handler ${match[1]}`)
    }
  }
})

console.log(`\n${passed} test groups passed.`)
