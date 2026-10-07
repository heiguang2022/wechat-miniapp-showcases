<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request, saveSession, session, upload, type Session } from './api'

const current = ref<Session | null>(session())
const section = ref('overview')
const busy = ref(false)
const merchants = ref<any[]>([]), stores = ref<any[]>([]), categories = ref<any[]>([]), products = ref<any[]>([]), specs = ref<any[]>([]), orders = ref<any[]>([])
const dialog = ref('')
const storeForm = reactive({ id: '', name: '', address: '', status: 'OPEN', businessHours: '08:00-20:00' })
const categoryForm = reactive({ id: '', name: '', sortOrder: 10, enabled: true })
const productForm = reactive({ id: '', categoryId: '', name: '', description: '', basePriceYuan: 26, imageUrl: '', status: 'DRAFT', sortOrder: 10, groupIds: [] as string[] })
const specForm = reactive({ name: '', required: true, valuesText: '冰:0\n热:0' })
const role = computed(() => current.value?.user.role || '')
const specGroups = computed(() => Array.from(new Map(specs.value.map(row => [row.id, { id: row.id, name: row.name }])).values()))
const merchantMenus = [
  ['overview', '经营概览'], ['orders', '订单处理'], ['stores', '门店管理'], ['categories', '分类管理'], ['products', '商品管理'], ['specs', '规格管理']
]

async function login(email: string) {
  busy.value = true
  try { const value = await request<Session>('/auth/dev-login', { method: 'POST', body: JSON.stringify({ email }) }); saveSession(value); current.value = value; section.value = 'overview'; await load() }
  catch (e: any) { ElMessage.error(e.message) } finally { busy.value = false }
}
function logout() { saveSession(null); current.value = null; merchants.value = []; stores.value = []; products.value = [] }
async function load() {
  if (!current.value) return
  try {
    if (role.value === 'PLATFORM_ADMIN') merchants.value = (await request<any>('/platform/merchants?page=1&pageSize=100')).items
    else { await Promise.all([loadStores(), loadCategories(), loadProducts(), loadSpecs(), loadOrders()]) }
  } catch (e: any) { ElMessage.error(e.message) }
}
async function loadStores() { stores.value = await request('/merchant/stores') }
async function loadCategories() { categories.value = await request('/merchant/categories') }
async function loadProducts() { products.value = await request('/merchant/products') }
async function loadSpecs() { specs.value = await request('/merchant/spec-groups') }
async function loadOrders() { orders.value = (await request<any>('/merchant/orders?page=1&pageSize=100')).items }
async function decide(id: string, action: string) {
  let reason = ''
  if (action !== 'approve') reason = await ElMessageBox.prompt('请输入原因', '确认操作').then(v => v.value)
  await request(`/platform/merchants/${id}/${action}`, { method: 'POST', body: JSON.stringify({ reason }) }); ElMessage.success('状态已更新'); await load()
}
function openStore(row?: any) { Object.assign(storeForm, row ? { ...row } : { id: '', name: '', address: '', status: 'OPEN', businessHours: '08:00-20:00' }); dialog.value = 'store' }
async function saveStore() { const { id, ...body } = storeForm; await request(`/merchant/stores${id ? `/${id}` : ''}`, { method: id ? 'PATCH' : 'POST', body: JSON.stringify(body) }); dialog.value = ''; ElMessage.success('门店已保存'); await loadStores() }
function openCategory(row?: any) { Object.assign(categoryForm, row ? { ...row } : { id: '', name: '', sortOrder: 10, enabled: true }); dialog.value = 'category' }
async function saveCategory() { const { id, ...body } = categoryForm; await request(`/merchant/categories${id ? `/${id}` : ''}`, { method: id ? 'PATCH' : 'POST', body: JSON.stringify(body) }); dialog.value = ''; ElMessage.success('分类已保存'); await loadCategories() }
async function openProduct(row?: any) { Object.assign(productForm, row ? { ...row, basePriceYuan: row.basePrice / 100, groupIds: [] } : { id: '', categoryId: categories.value[0]?.id || '', name: '', description: '', basePriceYuan: 26, imageUrl: '', status: 'DRAFT', sortOrder: 10, groupIds: [] }); if (row) productForm.groupIds = await request(`/merchant/products/${row.id}/spec-groups`); dialog.value = 'product' }
async function saveProduct() { const { id, basePriceYuan, groupIds, ...rest } = productForm; const saved = await request<any>(`/merchant/products${id ? `/${id}` : ''}`, { method: id ? 'PATCH' : 'POST', body: JSON.stringify({ ...rest, basePrice: Math.round(basePriceYuan * 100) }) }); await request(`/merchant/products/${saved.id}/spec-groups`, { method: 'PUT', body: JSON.stringify({ groupIds }) }); dialog.value = ''; ElMessage.success('商品已保存'); await loadProducts() }
async function productImage(file: File) { try { productForm.imageUrl = (await upload(file)).url; ElMessage.success('图片已上传') } catch (e: any) { ElMessage.error(e.message) } return false }
async function toggleProduct(row: any) { const status = row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'; await request(`/merchant/products/${row.id}/status`, { method: 'POST', body: JSON.stringify({ status }) }); await loadProducts() }
async function saveSpec() { const values = specForm.valuesText.split('\n').filter(Boolean).map(line => { const [name, price = '0'] = line.split(':'); return { name: name.trim(), priceDelta: Math.round(Number(price) * 100) } }); await request('/merchant/spec-groups', { method: 'POST', body: JSON.stringify({ name: specForm.name, required: specForm.required, values }) }); dialog.value = ''; ElMessage.success('规格已创建'); await loadSpecs() }
async function orderAction(id: string, action: string) { await request(`/merchant/orders/${id}/${action}`, { method: 'POST', body: '{}' }); ElMessage.success('订单状态已更新'); await loadOrders() }
function money(cents: number) { return `¥${(cents / 100).toFixed(2)}` }
function choose(value: string) { section.value = value; if (value === 'orders') loadOrders() }
onMounted(load)
</script>

<template>
  <div v-if="!current" class="login">
    <el-card class="login-card">
      <div class="brand">一刻咖啡 <span style="color:#b67b37">●</span></div>
      <p class="muted">多商户餐饮运营台 · 开发演示登录</p>
      <div class="login-actions">
        <el-button type="primary" size="large" :loading="busy" @click="login('admin@example.test')">平台管理员</el-button>
        <el-button size="large" :loading="busy" @click="login('merchant@example.test')">一刻咖啡商家</el-button>
        <el-button size="large" :loading="busy" @click="login('pending@example.test')">待审核商家</el-button>
      </div>
      <el-alert style="margin-top:20px" :closable="false" title="生产环境会自动关闭开发登录" type="info" />
    </el-card>
  </div>

  <el-container v-else class="shell">
    <el-aside width="230px" class="aside">
      <div class="brand">一刻咖啡</div><div class="muted">YIKE CONSOLE</div>
      <div class="user"><strong>{{ current.user.displayName }}</strong><br><small>{{ current.user.role }}</small></div>
      <template v-if="role === 'PLATFORM_ADMIN'">
        <div class="menu-item" :class="{active:section==='overview'}" @click="choose('overview')">平台概览</div>
        <div class="menu-item" :class="{active:section==='merchants'}" @click="choose('merchants')">商家审核</div>
      </template>
      <template v-else><div v-for="item in merchantMenus" :key="item[0]" class="menu-item" :class="{active:section===item[0]}" @click="choose(item[0])">{{ item[1] }}</div></template>
    </el-aside>
    <el-container>
      <el-header class="header"><strong>{{ role === 'PLATFORM_ADMIN' ? '平台管理' : '商家经营管理' }}</strong><el-button text @click="logout">退出登录</el-button></el-header>
      <el-main class="content">
        <template v-if="section==='overview'">
          <div class="page-head"><h2>{{ role === 'PLATFORM_ADMIN' ? '平台概览' : '今日经营' }}</h2><el-button @click="load">刷新</el-button></div>
          <div class="metric-grid">
            <div class="metric"><span class="muted">{{ role === 'PLATFORM_ADMIN' ? '商家总数' : '门店数量' }}</span><strong>{{ role === 'PLATFORM_ADMIN' ? merchants.length : stores.length }}</strong></div>
            <div class="metric"><span class="muted">{{ role === 'PLATFORM_ADMIN' ? '待审核商家' : '在售商品' }}</span><strong>{{ role === 'PLATFORM_ADMIN' ? merchants.filter(x=>x.status==='PENDING').length : products.filter(x=>x.status==='ON_SALE').length }}</strong></div>
            <div class="metric"><span class="muted">{{ role === 'PLATFORM_ADMIN' ? '已审核商家' : '待处理订单' }}</span><strong>{{ role === 'PLATFORM_ADMIN' ? merchants.filter(x=>x.status==='APPROVED').length : orders.filter(x=>['PENDING_ACCEPTANCE','PREPARING','READY'].includes(x.status)).length }}</strong></div>
          </div>
          <el-card><el-empty description="这是演示经营概览，第一阶段不包含财务结算和复杂数据大屏" /></el-card>
        </template>

        <template v-if="section==='merchants'">
          <div class="page-head"><h2>商家审核</h2><el-button @click="load">刷新</el-button></div>
          <el-card><el-table :data="merchants"><el-table-column prop="name" label="商家"/><el-table-column prop="status" label="状态"/><el-table-column prop="rejectReason" label="原因"/><el-table-column label="操作" width="250"><template #default="s"><div class="actions"><el-button size="small" type="success" @click="decide(s.row.id,'approve')">通过</el-button><el-button size="small" type="danger" @click="decide(s.row.id,'reject')">驳回</el-button><el-button size="small" @click="decide(s.row.id,'suspend')">停用</el-button></div></template></el-table-column></el-table></el-card>
        </template>

        <template v-if="section==='orders'">
          <div class="page-head"><h2>订单处理</h2><el-button @click="loadOrders">刷新</el-button></div>
          <el-card><el-table :data="orders"><el-table-column prop="orderNo" label="订单号"/><el-table-column prop="status" label="状态"/><el-table-column label="金额"><template #default="s">{{ money(s.row.totalAmount) }}</template></el-table-column><el-table-column prop="itemCount" label="件数"/><el-table-column label="操作" width="300"><template #default="s"><el-button v-if="s.row.status==='PENDING_ACCEPTANCE'" size="small" type="primary" @click="orderAction(s.row.id,'accept')">接单</el-button><el-button v-if="s.row.status==='PENDING_ACCEPTANCE'" size="small" @click="orderAction(s.row.id,'reject')">拒单</el-button><el-button v-if="s.row.status==='PREPARING'" size="small" type="success" @click="orderAction(s.row.id,'ready')">制作完成</el-button><el-button v-if="s.row.status==='READY'" size="small" type="success" @click="orderAction(s.row.id,'complete')">确认取餐</el-button></template></el-table-column></el-table></el-card>
        </template>

        <template v-if="section==='stores'">
          <div class="page-head"><h2>门店管理</h2><el-button type="primary" @click="openStore()">新增门店</el-button></div>
          <el-card><el-table :data="stores"><el-table-column prop="name" label="门店"/><el-table-column prop="address" label="地址"/><el-table-column prop="businessHours" label="营业时间"/><el-table-column prop="status" label="状态"/><el-table-column label="操作"><template #default="s"><el-button size="small" @click="openStore(s.row)">编辑</el-button></template></el-table-column></el-table></el-card>
        </template>
        <template v-if="section==='categories'">
          <div class="page-head"><h2>分类管理</h2><el-button type="primary" @click="openCategory()">新增分类</el-button></div>
          <el-card><el-table :data="categories"><el-table-column prop="name" label="分类"/><el-table-column prop="sortOrder" label="排序"/><el-table-column prop="enabled" label="启用"/><el-table-column label="操作"><template #default="s"><el-button size="small" @click="openCategory(s.row)">编辑</el-button></template></el-table-column></el-table></el-card>
        </template>
        <template v-if="section==='products'">
          <div class="page-head"><h2>商品管理</h2><el-button type="primary" @click="openProduct()">新增商品</el-button></div>
          <el-card><el-table :data="products"><el-table-column prop="name" label="商品"/><el-table-column label="价格"><template #default="s">{{ money(s.row.basePrice) }}</template></el-table-column><el-table-column prop="status" label="状态"/><el-table-column label="操作" width="200"><template #default="s"><el-button size="small" @click="openProduct(s.row)">编辑</el-button><el-button size="small" @click="toggleProduct(s.row)">{{ s.row.status==='ON_SALE'?'下架':'上架' }}</el-button></template></el-table-column></el-table></el-card>
        </template>
        <template v-if="section==='specs'">
          <div class="page-head"><h2>规格管理</h2><el-button type="primary" @click="dialog='spec'">新增规格组</el-button></div>
          <el-card><el-table :data="specs"><el-table-column prop="name" label="规格组"/><el-table-column prop="optionName" label="规格值"/><el-table-column label="加价"><template #default="s">{{ money(s.row.priceDelta||0) }}</template></el-table-column><el-table-column prop="requiredFlag" label="必选"/></el-table></el-card>
        </template>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="dialog" :title="dialog==='store'?'门店':dialog==='category'?'分类':dialog==='product'?'商品':'规格组'" width="560px">
    <el-form v-if="dialog==='store'" label-position="top"><div class="form-grid"><el-form-item label="门店名称"><el-input v-model="storeForm.name"/></el-form-item><el-form-item label="营业状态"><el-select v-model="storeForm.status"><el-option label="营业中" value="OPEN"/><el-option label="休息中" value="CLOSED"/><el-option label="已停用" value="DISABLED"/></el-select></el-form-item><el-form-item label="地址"><el-input v-model="storeForm.address"/></el-form-item><el-form-item label="营业时间"><el-input v-model="storeForm.businessHours"/></el-form-item></div><el-button type="primary" @click="saveStore">保存</el-button></el-form>
    <el-form v-else-if="dialog==='category'" label-position="top"><el-form-item label="分类名称"><el-input v-model="categoryForm.name"/></el-form-item><el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" :min="0"/></el-form-item><el-switch v-model="categoryForm.enabled" active-text="启用"/><div style="margin-top:20px"><el-button type="primary" @click="saveCategory">保存</el-button></div></el-form>
    <el-form v-else-if="dialog==='product'" label-position="top"><div class="form-grid"><el-form-item label="名称"><el-input v-model="productForm.name"/></el-form-item><el-form-item label="分类"><el-select v-model="productForm.categoryId"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/></el-select></el-form-item><el-form-item label="价格（元）"><el-input-number v-model="productForm.basePriceYuan" :min="0" :precision="2"/></el-form-item><el-form-item label="状态"><el-select v-model="productForm.status"><el-option label="草稿" value="DRAFT"/><el-option label="在售" value="ON_SALE"/><el-option label="下架" value="OFF_SALE"/></el-select></el-form-item></div><el-form-item label="描述"><el-input v-model="productForm.description"/></el-form-item><el-form-item label="适用规格组"><el-checkbox-group v-model="productForm.groupIds"><el-checkbox v-for="g in specGroups" :key="g.id" :value="g.id">{{ g.name }}</el-checkbox></el-checkbox-group></el-form-item><el-form-item label="商品图片"><el-upload :show-file-list="false" :before-upload="productImage"><el-button>上传图片</el-button></el-upload><span class="muted" style="margin-left:12px">{{ productForm.imageUrl }}</span></el-form-item><el-button type="primary" @click="saveProduct">保存</el-button></el-form>
    <el-form v-else-if="dialog==='spec'" label-position="top"><el-form-item label="规格组名称"><el-input v-model="specForm.name" placeholder="如：杯型"/></el-form-item><el-form-item label="规格值（每行：名称:加价元）"><el-input v-model="specForm.valuesText" type="textarea" :rows="5"/></el-form-item><el-switch v-model="specForm.required" active-text="必选"/><div style="margin-top:20px"><el-button type="primary" @click="saveSpec">创建</el-button></div></el-form>
  </el-dialog>
</template>
